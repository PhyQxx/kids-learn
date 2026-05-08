export const REALTIME_MESSAGE_TYPES = {
  PET_STATUS_UPDATE: 'PET_STATUS_UPDATE',
  USER_BALANCE_UPDATE: 'USER_BALANCE_UPDATE',
  CHILD_ACTIVITY_UPDATE: 'CHILD_ACTIVITY_UPDATE',
  PARENT_MONITOR_UPDATE: 'PARENT_MONITOR_UPDATE',
  ACHIEVEMENT_UNLOCKED: 'ACHIEVEMENT_UNLOCKED',
  NOTIFICATION: 'NOTIFICATION',
  PONG: 'PONG'
}

export function buildRealtimeUrl(baseUrl, token) {
  const url = new URL(baseUrl)
  const protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${url.host}/ws/realtime?token=${encodeURIComponent(token || '')}`
}

export function parseRealtimeMessage(rawData) {
  try {
    const text = typeof rawData === 'string' ? rawData : String(rawData || '')
    const parsed = JSON.parse(text)
    if (!parsed || typeof parsed.type !== 'string' || !parsed.type) {
      return null
    }
    return {
      type: parsed.type,
      payload: parsed.payload || {},
      timestamp: parsed.timestamp || ''
    }
  } catch (e) {
    return null
  }
}

export function reduceBalanceMessage(userInfo, payload = {}) {
  if (!userInfo) return null
  return {
    ...userInfo,
    gold: payload.gold ?? userInfo.gold,
    diamond: payload.diamond ?? userInfo.diamond
  }
}

export function shouldReconnect({ closedByUser, token }) {
  return !closedByUser && !!token
}
