import axios from 'axios'
import type { AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios'
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

request.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

function redirectToLogin() {
  const userStore = useUserStore()
  userStore.logout()
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
      const { accessToken, refreshToken: newRefreshToken } = res.data.data
      const userStore = useUserStore()
      userStore.setToken(accessToken)
      if (newRefreshToken) {
        localStorage.setItem('admin_refresh_token', newRefreshToken)
      }
      return accessToken
    }
  } catch {
    // 刷新失败
  }
  return null
}

// 统一处理401错误
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
        // 刷新成功，执行队列中的请求
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

request.interceptors.response.use(
  (response) => {
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

export default request
