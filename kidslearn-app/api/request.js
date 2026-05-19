import { refreshToken as doRefreshToken } from './auth'
import { useUserStore } from '@/store/user'

// API 请求封装
export const BASE_URL = 'http://192.168.12.164:19084/api/v1'

let isRefreshing = false
let requestsToRetry = []

const request = (options) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    uni.request({
      url: BASE_URL + (options.url.startsWith('/') ? options.url : '/' + options.url),
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
        ...options.header
      },
      success: async (res) => {
        if (res.statusCode === 200) {
          if (res.data.code === 200) {
            resolve(res.data.data)
          } else if (res.data.code === 401) {
            // Token过期，尝试无感刷新
            if (options.url.includes('/auth/refresh-token')) {
              // 刷新token的请求本身都401了，直接去登录
              useUserStore().logout()
              reject(res.data)
              return
            }

            if (!isRefreshing) {
              isRefreshing = true
              try {
                const rToken = uni.getStorageSync('refreshToken')
                if (!rToken) throw new Error('No refresh token')
                
                const refreshRes = await doRefreshToken(rToken)
                if (refreshRes && refreshRes.accessToken) {
                  useUserStore().setToken(refreshRes.accessToken, refreshRes.refreshToken)
                  // 重新执行失败的请求
                  options.header = options.header || {}
                  options.header['Authorization'] = `Bearer ${refreshRes.accessToken}`
                  
                  // 执行当前请求
                  request(options).then(resolve).catch(reject)
                  
                  // 执行队列中的请求
                  requestsToRetry.forEach(cb => cb(refreshRes.accessToken))
                  requestsToRetry = []
                } else {
                  throw new Error('Refresh failed')
                }
              } catch (e) {
                console.error('无感刷新失败，跳转登录', e)
                requestsToRetry.forEach(cb => cb(null))
                requestsToRetry = []
                useUserStore().logout()
                reject(res.data)
              } finally {
                isRefreshing = false
              }
            } else {
              // 正在刷新，将请求加入队列
              requestsToRetry.push((newToken) => {
                if (newToken) {
                  options.header = options.header || {}
                  options.header['Authorization'] = `Bearer ${newToken}`
                  request(options).then(resolve).catch(reject)
                } else {
                  reject(res.data)
                }
              })
            }
          } else {
            uni.showToast({ title: res.data.msg || '请求失败', icon: 'none' })
            reject(res.data)
          }
        } else {
          uni.showToast({ title: '网络错误', icon: 'none' })
          reject(res)
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络连接失败', icon: 'none' })
        reject(err)
      }
    })
  })
}

// 快捷方法
export const get = (url, data) => request({ url, method: 'GET', data })
export const post = (url, data) => request({ url, method: 'POST', data })
export const put = (url, data) => request({ url, method: 'PUT', data })
export const del = (url, data) => request({ url, method: 'DELETE', data })

export default request
