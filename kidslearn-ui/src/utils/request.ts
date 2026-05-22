import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

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

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      if (res.code === 401) {
        redirectToLogin()
      }
      return Promise.reject(new Error(res.msg))
    }
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return res as any
  },
  (error) => {
    if (error?.response?.status === 401) {
      redirectToLogin()
      ElMessage.error(error.response.data?.msg || '登录已过期，请重新登录')
    } else {
      ElMessage.error(error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default request
