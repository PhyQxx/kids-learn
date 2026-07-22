import axios from 'axios'
import type { InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

// 是否正在刷新token
let isRefreshing = false
// 等待刷新的请求队列
let pendingRequests: Array<{ resolve: (token: string) => void; reject: (error: any) => void }> = []

// 主动刷新定时器句柄
let refreshTimer: ReturnType<typeof setTimeout> | null = null
// 主动续期：到期前多久触发刷新（毫秒）。取「剩余 5 分钟」与「剩余 50%」中的较小值。
const PROACTIVE_REFRESH_LEAD_MS = 5 * 60 * 1000

request.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    // 1) 滑动续期：后端在 access token 临期时通过响应头下发新 token，这里静默替换
    const newToken = response.headers?.['x-new-token'] as string | undefined
    if (newToken) {
      const userStore = useUserStore()
      // 仅在仍使用同一旧 token 时替换，避免覆盖更新更晚的 token
      userStore.setToken(newToken)
      scheduleProactiveRefresh()
    }

    const res = response.data
    if (res.code !== 200) {
      // 业务码401
      if (res.code === 401) {
        const originalConfig = response.config as InternalAxiosRequestConfig & { _retry?: boolean }
        // 刷新token的请求本身401了，直接去登录
        if (originalConfig.url?.includes('/auth/refresh-token')) {
          redirectToLogin()
          return Promise.reject(new Error(res.msg))
        }
        return handle401(originalConfig)
      }
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg))
    }
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return res as any
  },
  (error) => {
    if (error?.response?.status === 401) {
      const originalConfig = error.config as InternalAxiosRequestConfig & { _retry?: boolean }
      return handle401(originalConfig)
    }
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

function redirectToLogin() {
  const userStore = useUserStore()
  userStore.logout()
  clearRefreshTimer()
  if (router.currentRoute.value.path !== '/login') {
    router.replace('/login')
  }
}

// 处理等待队列
function processPendingQueue(newToken: string | null) {
  pendingRequests.forEach(({ resolve, reject }) => {
    if (newToken) {
      resolve(newToken)
    } else {
      reject(new Error('Token refresh failed'))
    }
  })
  pendingRequests = []
}

// 尝试刷新token
async function tryRefreshToken(): Promise<string | null> {
  const refreshToken = localStorage.getItem('admin_refresh_token')
  if (!refreshToken) return null

  try {
    const res = await axios.post('/api/v1/auth/refresh-token', { refreshToken })
    if (res.data?.code === 200 && res.data?.data) {
      const { accessToken, refreshToken: newRefreshToken, expiresIn } = res.data.data
      const userStore = useUserStore()
      const expireAt = Date.now() + (expiresIn || 7200) * 1000
      userStore.setTokens(accessToken, newRefreshToken, expireAt)
      return accessToken
    }
  } catch {
    // 刷新失败
  }
  return null
}

// 统一处理401错误（被动兜底）
async function handle401(originalConfig: InternalAxiosRequestConfig & { _retry?: boolean }) {
  if (originalConfig._retry) {
    return Promise.reject(new Error('Token refresh failed'))
  }

  originalConfig._retry = true

  if (!isRefreshing) {
    isRefreshing = true
    try {
      const newToken = await tryRefreshToken()
      isRefreshing = false

      if (newToken) {
        // 刷新成功，重新调度主动续期定时器
        scheduleProactiveRefresh()
        // 执行队列中的请求
        processPendingQueue(newToken)
        // 重试原请求
        originalConfig.headers.Authorization = `Bearer ${newToken}`
        return request(originalConfig)
      } else {
        // 刷新失败，清空队列并跳转登录
        processPendingQueue(null)
        redirectToLogin()
        ElMessage.error('登录已过期，请重新登录')
        return Promise.reject(new Error('Token refresh failed'))
      }
    } catch (error) {
      isRefreshing = false
      processPendingQueue(null)
      redirectToLogin()
      return Promise.reject(error)
    }
  } else {
    // 正在刷新，加入队列等待
    return new Promise((resolve, reject) => {
      pendingRequests.push({
        resolve: (token: string) => {
          originalConfig.headers.Authorization = `Bearer ${token}`
          resolve(request(originalConfig))
        },
        reject: (error: any) => {
          reject(error)
        }
      })
    })
  }
}

/* ===================== 主动续期 + 跨标签页同步 ===================== */

function clearRefreshTimer() {
  if (refreshTimer) {
    clearTimeout(refreshTimer)
    refreshTimer = null
  }
}

/**
 * 主动续期调度：在 access token 到期前的合适时机触发一次刷新。
 * 用 setTimeout 而非 setInterval，每次刷新成功后重新调度，避免 token 变化后旧定时器跑飞。
 * 返回的 delay = min(剩余时间 - 5分钟, 剩余时间 / 2)。留 5 分钟提前量，并对极短有效期兜底。
 */
function scheduleProactiveRefresh() {
  clearRefreshTimer()
  const userStore = useUserStore()
  const expireAt = userStore.tokenExpireAt
  if (!expireAt) return // 未知过期时间，交由 401 兜底

  const remaining = expireAt - Date.now()
  if (remaining <= 0) {
    // 已过期，立即刷新
    triggerProactiveRefresh()
    return
  }
  const lead = Math.min(PROACTIVE_REFRESH_LEAD_MS, remaining / 2)
  const delay = Math.max(remaining - lead, 0)
  refreshTimer = setTimeout(triggerProactiveRefresh, delay)
}

async function triggerProactiveRefresh() {
  const userStore = useUserStore()
  if (!userStore.token) return
  if (isRefreshing) {
    // 已有被动刷新在进行，等它完成后由其重新调度
    return
  }
  isRefreshing = true
  try {
    const newToken = await tryRefreshToken()
    isRefreshing = false
    if (newToken) {
      scheduleProactiveRefresh()
    } else {
      // 主动刷新也失败，走登出
      redirectToLogin()
      ElMessage.error('登录已过期，请重新登录')
    }
  } catch {
    isRefreshing = false
    redirectToLogin()
  }
}

/**
 * 跨标签页同步：监听 localStorage 变化（storage 事件不会在产生变化的同一标签页触发，天然适合跨标签页）。
 * - 另一标签页刷新/滑动续期写入新 token：本标签页同步内存态并重排定时器。
 * - 另一标签页登出清空 token：本标签页一并登出。
 */
function onStorageEvent(e: StorageEvent) {
  if (e.key === 'admin_token') {
    const userStore = useUserStore()
    const newValue = e.newValue
    if (newValue) {
      // 仅当与当前内存中的 token 不同时同步，避免无谓重排
      if (userStore.token !== newValue) {
        // 不传 expireAt：若 admin_token_expire 也被一起更新，会由下面 else 分支精确同步；
        // 否则 setToken 会按默认有效期估算，足够触发下一次主动续期。
        userStore.setToken(newValue)
        scheduleProactiveRefresh()
      }
    } else {
      // token 被清空 = 另一标签页登出
      clearRefreshTimer()
      if (router.currentRoute.value.path !== '/login') {
        router.replace('/login')
      }
    }
  } else if (e.key === 'admin_token_expire') {
    // 过期时间被其他标签页更新（如刷新后），同步并重排
    const expire = Number(e.newValue || 0)
    if (expire && expire !== useUserStore().tokenExpireAt) {
      useUserStore().tokenExpireAt = expire
      scheduleProactiveRefresh()
    }
  }
}

let initialized = false
/**
 * 在应用启动时调用（main.ts 中，pinia 装载之后）。
 * 注册跨标签页监听，并在已登录时启动主动续期定时器。
 */
export function initTokenSync() {
  if (initialized) return
  initialized = true
  window.addEventListener('storage', onStorageEvent)
  const userStore = useUserStore()
  if (userStore.token) {
    scheduleProactiveRefresh()
  }
}

export { scheduleProactiveRefresh }

export default request
