import test from 'node:test'
import assert from 'node:assert/strict'

import {
  parseCondition,
  parseRewards,
  describeRewards,
  describeCondition,
  resolveTiers,
  tierBadgeName
} from '../utils/achievementFeature.mjs'

test('parseCondition reads target from various key names', () => {
  assert.deepEqual(parseCondition('{"type":"THREE_STAR","target":10}'), { type: 'THREE_STAR', target: 10 })
  assert.deepEqual(parseCondition({ type: 'STREAK_DAYS', days: 7 }), { type: 'STREAK_DAYS', target: 7 })
  assert.deepEqual(parseCondition({ type: 'STICKER_COUNT', stickerCount: 5 }), { type: 'STICKER_COUNT', target: 5 })
  assert.deepEqual(parseCondition('not json'), { type: '', target: 0 })
  assert.deepEqual(parseCondition(null), { type: '', target: 0 })
})

test('parseRewards extracts typed reward items', () => {
  const rewards = parseRewards('{"rewards":[{"type":"gold","value":120},{"type":"sticker","stickerId":9,"quantity":2}]}')
  assert.deepEqual(rewards, [
    { type: 'GOLD', label: '金币', value: 120 },
    { type: 'STICKER', label: '贴纸', value: 2 }
  ])
  assert.deepEqual(parseRewards(null), [])
  assert.deepEqual(parseRewards('{}'), [])
})

test('describeRewards joins rewards into readable text', () => {
  assert.equal(
    describeRewards('{"rewards":[{"type":"gold","value":120},{"type":"exp","value":30}]}'),
    '金币+120 · 经验+30'
  )
  assert.equal(describeRewards(''), '')
})

test('describeCondition renders type and target', () => {
  assert.equal(describeCondition('{"type":"THREE_STAR","target":10}'), '三星通关 10')
  assert.equal(describeCondition('{"type":"STREAK_DAYS","target":7}'), '连续学习 7')
  assert.equal(describeCondition(''), '')
})

test('resolveTiers computes per-tier achievement and current tier index', () => {
  const tiers = [
    { tierLevel: 1, tierName: '青铜', conditionJson: '{"target":5}', rewardJson: '{"rewards":[{"type":"gold","value":50}]}' },
    { tierLevel: 2, tierName: '白银', conditionJson: '{"target":10}', rewardJson: '{"rewards":[{"type":"gold","value":120}]}' },
    { tierLevel: 3, tierName: '黄金', conditionJson: '{"target":20}', rewardJson: '{"rewards":[{"type":"title","titleId":4}]}' }
  ]

  // 进度 7：青铜已达成，白银进行中
  const result = resolveTiers(tiers, 7)
  assert.equal(result.hasTiers, true)
  assert.equal(result.tiers.length, 3)
  assert.equal(result.totalTarget, 20)
  assert.equal(result.currentTierIndex, 1)
  assert.equal(result.tiers[0].achieved, true)
  assert.equal(result.tiers[1].achieved, false)
  assert.equal(result.tiers[2].achieved, false)
  assert.equal(result.tiers[0].rewardText, '金币+50')
})

test('resolveTiers marks all tiers achieved when fully completed', () => {
  const tiers = [
    { tierLevel: 1, conditionJson: '{"target":5}' },
    { tierLevel: 2, conditionJson: '{"target":10}' }
  ]
  const result = resolveTiers(tiers, 15)
  assert.equal(result.currentTierIndex, 1)
  assert.equal(result.tiers.every(t => t.achieved), true)
})

test('resolveTiers handles empty tiers gracefully', () => {
  assert.deepEqual(resolveTiers([], 5), { tiers: [], currentTierIndex: -1, totalTarget: 0, hasTiers: false })
  assert.deepEqual(resolveTiers(null, 5), { tiers: [], currentTierIndex: -1, totalTarget: 0, hasTiers: false })
})

test('tierBadgeName maps level to readable name', () => {
  assert.equal(tierBadgeName(1), '铜')
  assert.equal(tierBadgeName(2), '银')
  assert.equal(tierBadgeName(3), '金')
  assert.equal(tierBadgeName(4), '传说')
  assert.equal(tierBadgeName(6), 'Lv6')
})
