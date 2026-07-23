import { get, post, put, del } from './request'

// 获取用户信息
export const getUserInfo = () => get('/user/info')

// 更新用户信息
export const updateUserInfo = (data) => put('/user/info', data)

// 更新孩子档案
export const updateChildProfile = (data) => put('/user/child-profile', data)
export const updatePassword = (data) => put('/user/password', data)
export const sendPhoneChangeCode = (phone) => post('/user/phone/send-code', { phone })
export const changePhone = (data) => put('/user/phone', data)
export const getDevices = () => get('/user/devices')
export const revokeDevice = (deviceId) => del(`/user/devices/${deviceId}`)
export const deactivateAccount = (data) => post('/user/account-cancellation', data)
export const exportAccountData = (parentPin) => post('/user/data-export', { parentPin })
