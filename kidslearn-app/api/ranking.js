import { get } from './request'

// 获取排行榜 (type: 'weekly' 周榜, 'challenge' 挑战积分榜, 其他 总榜)
export const getRanking = (type = 'weekly') => get(`/leaderboard/${type}`)
