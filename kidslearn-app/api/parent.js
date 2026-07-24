import { get, put } from './request'

export const getParentReport = (month) => get('/parent/report', month ? { month } : {})

export const getParentAiSummary = () => get('/parent/ai-summary')

export const getTimeControl = () => get('/parent/time-control')

export const saveTimeControl = (data, parentPin) =>
  put('/parent/time-control', parentPin ? { ...data, parentPin } : data)

export const getFamily = () => get('/parent/family')

export const getRealtimeMonitor = () => get('/parent/realtime-monitor')
