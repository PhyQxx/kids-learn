import { get, post } from './request'

// 获取成就列表
export const getAchievements = (params) => get('/achievement/list', params)

// 获取我的成就进度
export const getMyProgress = () => get('/achievement/my-progress')

// 获取贴纸列表
export const getStickers = () => get('/achievement/stickers')

// 获取称号列表
export const getTitles = () => get('/achievement/titles')

// 领取奖励
export const receiveReward = (data) => post('/achievement/receive-reward', data)

// 激活称号
export const activateTitle = (data) => post('/achievement/activate-title', data)
