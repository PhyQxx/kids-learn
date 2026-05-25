import { evaluateLearningAccess } from './timeControl.mjs'

function getTodayMinutes(report) {
  const today = report?.today || {}
  const value = today.learnMinutes ?? today.todayMinutes ?? report?.learnMinutes ?? 0
  const number = Number(value)
  return Number.isFinite(number) ? Math.max(0, Math.round(number)) : 0
}

export async function ensureLearningAccess({
  fetchTimeControl,
  fetchReport,
  showBlockedMessage,
  now = new Date()
} = {}) {
  if (typeof fetchTimeControl !== 'function' || typeof fetchReport !== 'function') {
    return true
  }

  try {
    const [timeControl, report] = await Promise.all([
      fetchTimeControl(),
      fetchReport()
    ])
    const result = evaluateLearningAccess({
      timeControl,
      todayMinutes: getTodayMinutes(report),
      now
    })

    if (!result.allowed) {
      if (typeof showBlockedMessage === 'function') {
        showBlockedMessage(result.message)
      }
      return false
    }
    return true
  } catch (error) {
    return true
  }
}
