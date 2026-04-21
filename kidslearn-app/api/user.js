import { get, put } from './request'

// 获取用户信息
export const getUserInfo = () => get('/user/info')

// 更新用户信息
export const updateUserInfo = (data) => put('/user/info', data)

// 更新孩子档案
export const updateChildProfile = (data) => put('/user/child-profile', data)
