import { get, post } from './request'

// 挑战赛面板
export const getChallengeDashboard = () => get('/challenge/dashboard')

// 创建挑战赛 (type: 'RANKED' 排位赛, 'FRIEND' 好友PK)
export const createChallenge = (type = 'RANKED', opponentId) => post('/challenge/create', { type, opponentId })

// 提交挑战结果
export const submitChallengeResult = (payload) => post('/challenge/submit', payload)

// 获取挑战赛记录
export const getChallengeRecords = () => get('/challenge/records')

// 获取挑战积分榜
export const getChallengeRanking = () => get('/challenge/ranking')
