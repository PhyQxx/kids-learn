/**
 * 成就分级（tiers）/ 奖励解析的纯函数。
 * 后端 AchievementTier.conditionJson / rewardJson 是裸 JSON 字符串，
 * 这里统一解析成 App 端可展示的结构，并支持单测。
 */

/** 后端 AchievementRuleEngine 支持的条件类型 → 中文文案 */
const CONDITION_LABELS = {
  COMPLETE_LEVEL: '通关关卡',
  THREE_STAR: '三星通关',
  PERFECT_SCORE: '满分通关',
  MATH_LEVEL: '数学通关',
  MATH_PERFECT: '数学满分',
  STREAK_DAYS: '连续学习',
  SUBJECT_COUNT: '学习科目',
  STICKER_COUNT: '收集贴纸',
  STICKER: '收集贴纸',
  RANK_TOP: '排行榜前',
  GOLD: '获得金币',
  EXP: '获得经验'
}

/** 奖励类型 → 中文文案 */
const REWARD_LABELS = {
  GOLD: '金币',
  EXP: '经验',
  DIAMOND: '钻石',
  STICKER: '贴纸',
  TITLE: '称号'
}

function toNumber(value, fallback = 0) {
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}

/** 安全解析 JSON 字符串，失败返回 null */
function parseJson(value) {
  if (value == null) return null
  if (typeof value !== 'string') return value
  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}

/**
 * 解析单个 tier 的 conditionJson，返回 { type, target }。
 * 兼容后端多种 key 命名（target/targetValue/count/value/levelCount 等）。
 */
export function parseCondition(conditionJson) {
  const condition = parseJson(conditionJson)
  if (!condition || typeof condition !== 'object') {
    return { type: '', target: 0 }
  }
  const type = (condition.type || condition.conditionType || '').toString()
  const target = toNumber(
    condition.target || condition.targetValue || condition.count || condition.value ||
    condition.levelCount || condition.starCount || condition.stickerCount ||
    condition.subjectCount || condition.days || condition.rank
  )
  return { type, target }
}

/**
 * 解析单个 tier 的 rewardJson，返回奖励数组 [{ type, label, value }]。
 */
export function parseRewards(rewardJson) {
  const root = parseJson(rewardJson)
  if (!root) return []
  const items = Array.isArray(root) ? root : (root.rewards || root.items || [])
  if (!Array.isArray(items)) return []
  return items
    .map(item => {
      if (!item || typeof item !== 'object') return null
      const rawType = (item.type || '').toString().toUpperCase()
      const type = rawType
      const label = REWARD_LABELS[type] || type
      const value = toNumber(item.value || item.quantity || item.amount || item.count)
      return value > 0 ? { type, label, value } : null
    })
    .filter(Boolean)
}

/** 把奖励数组拼成一段可读文本，如「金币+120 · 贴纸×2」 */
export function describeRewards(rewardJson) {
  const rewards = parseRewards(rewardJson)
  if (rewards.length === 0) return ''
  return rewards
    .map(r => `${r.label}+${r.value}`)
    .join(' · ')
}

/** 条件类型 → 中文文案 */
export function describeCondition(conditionJson) {
  const { type, target } = parseCondition(conditionJson)
  if (!type && !target) return ''
  const label = CONDITION_LABELS[type] || type || '达成'
  return target > 0 ? `${label} ${target}` : label
}

/**
 * 把一个成就的所有 tiers 解析成多档进度结构。
 * 返回 { tiers: [...], currentTierIndex, totalTarget, hasTiers }
 * 每个 tier: { level, name, target, rewardText, achieved }
 *   - achieved: 该档是否已达成（currentValue >= target）
 *   - currentTierIndex: 当前所在档位（第一个未达成的档，全达成则为最后一档）
 *   - totalTarget: 最高档目标值（用于整体进度条）
 */
export function resolveTiers(tiers, currentValue = 0) {
  if (!Array.isArray(tiers) || tiers.length === 0) {
    return { tiers: [], currentTierIndex: -1, totalTarget: 0, hasTiers: false }
  }

  const current = toNumber(currentValue)
  const parsed = tiers.map((t, idx) => {
    const { target } = parseCondition(t.conditionJson)
    return {
      level: toNumber(t.tierLevel, idx + 1),
      name: t.tierName || '',
      target: target > 0 ? target : 1,
      rewardText: describeRewards(t.rewardJson),
      achieved: current >= (target > 0 ? target : 1)
    }
  })

  // 按档位级别排序
  parsed.sort((a, b) => a.level - b.level)

  const totalTarget = parsed.length > 0 ? parsed[parsed.length - 1].target : 0
  const currentTierIndex = parsed.findIndex(t => !t.achieved)
  const resolvedIndex = currentTierIndex === -1 ? parsed.length - 1 : currentTierIndex

  return {
    tiers: parsed,
    currentTierIndex: resolvedIndex,
    totalTarget,
    hasTiers: true
  }
}

/** 当前档位名称（用于卡片角标） */
const TIER_NAMES = ['', '铜', '银', '金', '传说']
export function tierBadgeName(level) {
  return TIER_NAMES[level] || `Lv${level}`
}
