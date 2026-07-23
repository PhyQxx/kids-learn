import { evaluateLearningAccess } from './timeControl.mjs'

function getTodayMinutes(report) {
  const today = report?.today || {}
  const value = today.learnMinutes ?? today.todayMinutes ?? report?.learnMinutes ?? 0
  const number = Number(value)
  return Number.isFinite(number) ? Math.max(0, Math.round(number)) : 0
}

export async function ensureLearningAccess({
  fetchAccessStatus,
  fetchTimeControl,
  fetchReport,
  showBlockedMessage,
  now = new Date()
} = {}) {
  if (typeof fetchAccessStatus === 'function') {
    try {
      const status = await fetchAccessStatus()
      if (status?.allowed === false) {
        if (typeof showBlockedMessage === 'function') {
          showBlockedMessage(status.message || '当前暂时不能开始学习')
        }
        return false
      }
      return status?.allowed === true
    } catch (error) {
      if (typeof showBlockedMessage === 'function') {
        showBlockedMessage('暂时无法验证家长时间设置，请检查网络后重试')
      }
      return false
    }
  }

  if (typeof fetchTimeControl !== 'function' || typeof fetchReport !== 'function') {
    return false
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
    if (typeof showBlockedMessage === 'function') {
      showBlockedMessage('暂时无法验证家长时间设置，请检查网络后重试')
    }
    return false
  }
}
