import { get, put } from './request'

// 获取学习报告 (month: "YYYY-MM", 默认当月)
export const getReport = (params) => get('/parent/report', params)

// 获取时间控制设置
export const getTimeControl = () => get('/parent/time-control')

// 更新时间控制设置
export const updateTimeControl = (data) => put('/parent/time-control', data)

// 获取家庭成员
export const getFamilyMembers = () => get('/parent/family')
