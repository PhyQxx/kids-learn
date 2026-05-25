const DEFAULT_AVATAR = '/static/logo.png'

const MOOD_TEXT = {
  1: '难过',
  2: '一般',
  3: '开心',
  4: '兴奋',
}

export function buildMineProfileSummary({
  userInfo = null,
  petInfo = null,
  achievementProgress = null,
} = {}) {
  const level = toNumber(userInfo?.level, 1)
  const gradeName = textOr(userInfo?.gradeLevelName, '趣学探索者')
  const petName = textOr(petInfo?.petName, '小星')
  const petLevel = toNumber(petInfo?.currentLevel, 1)
  const petMood = MOOD_TEXT[toNumber(petInfo?.mood, 3)] || MOOD_TEXT[3]
  const completedAchievements = toNumber(achievementProgress?.completedAchievements, 0)

  return {
    nickname: textOr(userInfo?.nickname, '小朋友'),
    avatar: textOr(userInfo?.avatar, DEFAULT_AVATAR),
    levelText: `Lv.${level} ${gradeName}`,
    stats: [
      { label: '金币', value: toNumber(userInfo?.gold, 0) },
      { label: '钻石', value: toNumber(userInfo?.diamond, 0) },
      { label: '贴纸', value: toNumber(achievementProgress?.collectedStickers, 0) },
    ],
    petText: `${petName} Lv.${petLevel} · ${petMood}`,
    achievementText: `已解锁 ${completedAchievements} 个成就`,
  }
}

function textOr(value, fallback) {
  return typeof value === 'string' && value.trim() ? value.trim() : fallback
}

function toNumber(value, fallback) {
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}
