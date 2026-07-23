const DEFAULT_TIME_CONTROL = {
  enabled: true,
  dailyLimitMinutes: 60,
  limitEnabled: true,
  allowedStartTime: '08:00',
  allowedEndTime: '21:00',
  allowedWindowEnabled: true,
  restReminder: true,
  autoLockAfterTask: true
}

function toInteger(value, fallback = 0) {
  const number = Number(value)
  return Number.isFinite(number) ? Math.max(0, Math.round(number)) : fallback
}

function normalizeClockTime(value, fallback) {
  const text = String(value || '').trim()
  const match = text.match(/^(\d{1,2}):(\d{2})/)
  if (!match) return fallback

  const hour = Number(match[1])
  const minute = Number(match[2])
  if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
    return fallback
  }

  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`
}

function toMinutes(time) {
  const normalized = normalizeClockTime(time, '00:00')
  const [hour, minute] = normalized.split(':').map(Number)
  return hour * 60 + minute
}

function getCurrentMinutes(now) {
  if (typeof now === 'string') {
    const isoTime = now.match(/T(\d{1,2}):(\d{2})/)
    if (isoTime) {
      return Number(isoTime[1]) * 60 + Number(isoTime[2])
    }
    if (/^\d{1,2}:\d{2}/.test(now)) {
      return toMinutes(now)
    }
  }

  const date = now instanceof Date ? now : new Date(now || Date.now())
  return date.getHours() * 60 + date.getMinutes()
}

function isWithinWindow(currentMinutes, startMinutes, endMinutes) {
  if (startMinutes === endMinutes) return true
  if (startMinutes < endMinutes) {
    return currentMinutes >= startMinutes && currentMinutes <= endMinutes
  }
  return currentMinutes >= startMinutes || currentMinutes <= endMinutes
}

export function normalizeTimeControl(raw = {}) {
  return {
    enabled: raw.enabled !== undefined ? Boolean(raw.enabled) : DEFAULT_TIME_CONTROL.enabled,
    dailyLimitMinutes: toInteger(raw.dailyLimitMinutes, DEFAULT_TIME_CONTROL.dailyLimitMinutes),
    limitEnabled: raw.limitEnabled !== undefined
      ? Boolean(raw.limitEnabled)
      : toInteger(raw.dailyLimitMinutes, DEFAULT_TIME_CONTROL.dailyLimitMinutes) > 0,
    allowedStartTime: normalizeClockTime(
      raw.allowedStartTime ?? raw.forbiddenStart,
      DEFAULT_TIME_CONTROL.allowedStartTime
    ),
    allowedEndTime: normalizeClockTime(
      raw.allowedEndTime ?? raw.forbiddenEnd,
      DEFAULT_TIME_CONTROL.allowedEndTime
    ),
    allowedWindowEnabled: raw.allowedWindowEnabled !== undefined
      ? Boolean(raw.allowedWindowEnabled)
      : DEFAULT_TIME_CONTROL.allowedWindowEnabled,
    restReminder: raw.restReminder !== undefined
      ? Boolean(raw.restReminder)
      : DEFAULT_TIME_CONTROL.restReminder,
    autoLockAfterTask: raw.autoLockAfterTask !== undefined
      ? Boolean(raw.autoLockAfterTask)
      : DEFAULT_TIME_CONTROL.autoLockAfterTask
  }
}

export function evaluateLearningAccess({ timeControl, todayMinutes = 0, now = new Date() } = {}) {
  const control = normalizeTimeControl(timeControl)
  const learnedMinutes = toInteger(todayMinutes, 0)

  if (!control.enabled) {
    return { allowed: true, reasonCode: 'ALLOWED', message: '' }
  }

  if (
    control.limitEnabled &&
    control.dailyLimitMinutes > 0 &&
    learnedMinutes >= control.dailyLimitMinutes
  ) {
    return {
      allowed: false,
      reasonCode: 'DAILY_LIMIT_REACHED',
      message: `今日学习已达到 ${control.dailyLimitMinutes} 分钟上限，先休息一下吧。`
    }
  }

  const currentMinutes = getCurrentMinutes(now)
  const startMinutes = toMinutes(control.allowedStartTime)
  const endMinutes = toMinutes(control.allowedEndTime)
  if (control.allowedWindowEnabled && !isWithinWindow(currentMinutes, startMinutes, endMinutes)) {
    return {
      allowed: false,
      reasonCode: 'OUTSIDE_ALLOWED_TIME',
      message: `当前不在学习时段内，请在 ${control.allowedStartTime}-${control.allowedEndTime} 再开始。`
    }
  }

  return {
    allowed: true,
    reasonCode: 'ALLOWED',
    message: ''
  }
}
