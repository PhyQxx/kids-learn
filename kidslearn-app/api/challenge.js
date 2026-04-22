import { get, post } from './request'

// 创建挑战赛 (type: 'RANDOM' 默认随机匹配)
export const createChallenge = (type = 'RANDOM') => post('/challenge/create', { type })

// 获取挑战赛记录
export const getChallengeRecords = () => get('/challenge/records')
