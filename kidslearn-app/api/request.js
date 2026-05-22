import { refreshToken as doRefreshToken } from './auth'
import { useUserStore } from '@/store/user'
import { showLoading as startLoading, hideLoading as stopLoading } from '@/utils/loading'
import { handleUnauthorizedResponse } from '@/utils/requestAuth.mjs'

// API 请求封装
export const BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

let isRefreshing = false
let requestsToRetry = []
let loadingCount = 0

const request = (options) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    
    // 自动加载处理
    if (options.showLoading) {
      loadingCount++
      const loadingTitle = typeof options.showLoading === 'string' ? options.showLoading : '加载中'
      startLoading(loadingTitle, options.loadingMascot || '🌍')
    }

    const clearLoading = () => {
      if (options.showLoading) {
        loadingCount--
        if (loadingCount <= 0) {
          loadingCount = 0
          stopLoading()
        }
      }
    }

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
        if (res.statusCode === 401) {
          clearLoading()
          handleUnauthorizedResponse(res, useUserStore())
          reject(res.data || res)
          return
        }

        if (res.statusCode === 200) {
          if (res.data.code === 200) {
            clearLoading()
            resolve(res.data.data)
          } else if (res.data.code === 401) {
            // Token过期，尝试无感刷新
            if (options.url.includes('/auth/refresh-token')) {
              // 刷新token的请求本身都401了，直接去登录
              clearLoading()
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
                  
                  // 注意：这里由于重新调用了 request，clearLoading 会在后续调用中执行，所以这里不需要额外处理 clearLoading
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
                clearLoading()
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
                  clearLoading()
                  reject(res.data)
                }
              })
            }
          } else {
            clearLoading()
            uni.showToast({ title: res.data.msg || '请求失败', icon: 'none' })
            reject(res.data)
          }
        } else {
          clearLoading()
          uni.showToast({ title: '网络错误', icon: 'none' })
          reject(res)
        }
      },
      fail: (err) => {
        clearLoading()
        uni.showToast({ title: '网络连接失败', icon: 'none' })
        reject(err)
      }
    })
  })
}

// 快捷方法
export const get = (url, data, options = {}) => request({ url, method: 'GET', data, ...options })
export const post = (url, data, options = {}) => request({ url, method: 'POST', data, ...options })
export const put = (url, data, options = {}) => request({ url, method: 'PUT', data, ...options })
export const del = (url, data, options = {}) => request({ url, method: 'DELETE', data, ...options })

export default request
