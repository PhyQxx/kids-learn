import { get, post } from './request'
import { createChallengePayload } from '@/utils/challengeData.mjs'

// 挑战赛面板
export const getChallengeDashboard = () => get('/challenge/dashboard')

// 创建挑战赛 (type: 'RANKED' 排位赛, 'FRIEND' 好友PK)
export const createChallenge = (type = 'RANKED', opponentId) =>
  post('/challenge/create', createChallengePayload(type, opponentId))

// 提交挑战结果
export const submitChallengeResult = (payload) => post('/challenge/submit', payload)

export const getChallengeQuestions = (matchId) => get(`/challenge/matches/${matchId}/questions`)
export const submitChallengeAnswer = (matchId, payload) => post(`/challenge/matches/${matchId}/answers`, payload)
export const finishChallenge = (matchId) => post(`/challenge/matches/${matchId}/finish`)
export const getChallengeStatus = (matchId) => get(`/challenge/matches/${matchId}`)
export const acceptChallenge = (matchId) => post(`/challenge/matches/${matchId}/accept`)
export const rejectChallenge = (matchId) => post(`/challenge/matches/${matchId}/reject`)

export const getFriendList = () => get('/friend/list')

// 获取挑战赛记录
export const getChallengeRecords = () => get('/challenge/records')

// 获取挑战积分榜
export const getChallengeRanking = () => get('/challenge/ranking')
