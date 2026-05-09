export const PARENT_MONITOR_MESSAGE_TYPES = {
  CHILD_ACTIVITY_UPDATE: 'CHILD_ACTIVITY_UPDATE',
  PARENT_MONITOR_UPDATE: 'PARENT_MONITOR_UPDATE'
}

const STATUS_TEXT = {
  LEARNING: '学习中',
  RESTING: '休息中',
  ONLINE: '在线',
  IDLE: '待开始',
  LIMITED: '已达上限',
  OFFLINE: '离线'
}

function toNumber(value, fallback = 0) {
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}

function clampNonNegative(value) {
  return Math.max(0, Math.round(toNumber(value, 0)))
}

function normalizeStatus(child) {
  const rawStatus = String(child.status || '').toUpperCase()
  if (rawStatus) return rawStatus
  return child.online ? 'ONLINE' : 'OFFLINE'
}

function normalizeChild(raw = {}) {
  const todayMinutes = clampNonNegative(raw.todayMinutes)
  const dailyLimitMinutes = clampNonNegative(raw.dailyLimitMinutes)
  const status = normalizeStatus(raw)
  const remainingMinutes = dailyLimitMinutes > 0
    ? Math.max(0, dailyLimitMinutes - todayMinutes)
    : 0

  return {
    childId: raw.childId ?? raw.userId ?? raw.id ?? null,
    nickname: raw.nickname || raw.name || '孩子',
    avatar: raw.avatar || '',
    online: Boolean(raw.online || status === 'LEARNING' || status === 'ONLINE'),
    status,
    statusText: (!raw.status && raw.statusText) || STATUS_TEXT[status] || STATUS_TEXT.IDLE,
    todayMinutes,
    dailyLimitMinutes,
    remainingMinutes,
    completedLevels: clampNonNegative(raw.completedLevels),
    totalQuestions: clampNonNegative(raw.totalQuestions),
    correctCount: clampNonNegative(raw.correctCount),
    accuracy: Math.min(100, clampNonNegative(raw.accuracy)),
    currentSubjectName: raw.currentSubjectName || '',
    currentLevelName: raw.currentLevelName || '',
    latestScore: clampNonNegative(raw.latestScore),
    stars: clampNonNegative(raw.stars),
    isPass: Boolean(raw.isPass),
    lastActivityAt: raw.lastActivityAt || ''
  }
}

function buildSummary(children) {
  return {
    childCount: children.length,
    onlineCount: children.filter(child => child.online).length,
    learningCount: children.filter(child => child.status === 'LEARNING').length,
    todayMinutes: children.reduce((sum, child) => sum + child.todayMinutes, 0),
    completedLevels: children.reduce((sum, child) => sum + child.completedLevels, 0),
    alertCount: children.filter(child => child.dailyLimitMinutes > 0 && child.todayMinutes >= child.dailyLimitMinutes).length
  }
}

export function normalizeMonitorSnapshot(raw = {}) {
  const source = raw.data && typeof raw.data === 'object' ? raw.data : raw
  const children = Array.isArray(source.children)
    ? source.children.map(normalizeChild)
    : []

  return {
    family: {
      familyName: source.family?.familyName || '',
      inviteCode: source.family?.inviteCode || ''
    },
    children,
    summary: buildSummary(children),
    generatedAt: source.generatedAt || ''
  }
}

export function reduceMonitorEvent(current, message) {
  if (!message || !message.type) {
    return normalizeMonitorSnapshot(current)
  }
  if (message.type === PARENT_MONITOR_MESSAGE_TYPES.PARENT_MONITOR_UPDATE) {
    return normalizeMonitorSnapshot(message.payload)
  }
  if (message.type !== PARENT_MONITOR_MESSAGE_TYPES.CHILD_ACTIVITY_UPDATE) {
    return normalizeMonitorSnapshot(current)
  }

  const snapshot = normalizeMonitorSnapshot(current)
  const payload = message.payload || {}
  const childId = payload.childId ?? payload.userId ?? payload.id
  const children = snapshot.children.map(child => (
    child.childId === childId ? normalizeChild({ ...child, ...payload }) : child
  ))
  if (!children.some(child => child.childId === childId)) {
    children.push(normalizeChild(payload))
  }

  return normalizeMonitorSnapshot({
    ...snapshot,
    children,
    generatedAt: message.timestamp || snapshot.generatedAt
  })
}
