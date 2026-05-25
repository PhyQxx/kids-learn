import { get, put } from './request'

export const getParentReport = (month) => get('/parent/report', month ? { month } : {})

export const getParentAiSummary = () => get('/parent/ai-summary')

export const getTimeControl = () => get('/parent/time-control')

export const saveTimeControl = (data) => put('/parent/time-control', data)

export const getFamily = () => get('/parent/family')

export const getRealtimeMonitor = () => get('/parent/realtime-monitor')
