import { get } from './request'

// 获取排行榜 (type: 'weekly' 周榜, 其他 总榜)
export const getRanking = (type = 'weekly') => get(`/leaderboard/${type}`)

// 获取排行榜 (兼容旧调用)
export const getMyRank = (type = 'weekly') => get(`/leaderboard/${type}`)
