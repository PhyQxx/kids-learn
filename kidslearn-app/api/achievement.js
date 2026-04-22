import { get, post } from './request'

// 获取成就列表 (type: 可选筛选类型)
export const getAchievements = (type) => get('/achievement/list', { type })

// 获取我的成就进度
export const getMyProgress = () => get('/achievement/my-progress')

// 获取贴纸列表 (seriesId: 可选系列ID)
export const getStickers = (seriesId) => get('/achievement/stickers', { seriesId })

// 获取称号列表
export const getTitles = () => get('/achievement/titles')

// 领取奖励 (achievementId 通过 query param)
export const receiveReward = (achievementId) => post('/achievement/receive-reward?achievementId=' + achievementId)

// 激活称号 (titleId 通过 query param)
export const activateTitle = (titleId) => post('/achievement/activate-title?titleId=' + titleId)
