import test from 'node:test'
import assert from 'node:assert/strict'
import { buildMineProfileSummary } from '../utils/mineProfile.mjs'

test('mine profile summary uses real user and achievement data', () => {
  const summary = buildMineProfileSummary({
    userInfo: {
      nickname: '乐乐',
      avatar: '/static/avatar.png',
      level: 8,
      gold: 128,
      diamond: 6,
      gradeLevelName: '二年级',
    },
    petInfo: {
      petName: '小星',
      currentLevel: 5,
      mood: 4,
    },
    achievementProgress: {
      completedAchievements: 9,
      collectedStickers: 12,
    },
  })

  assert.equal(summary.nickname, '乐乐')
  assert.equal(summary.avatar, '/static/avatar.png')
  assert.equal(summary.levelText, 'Lv.8 二年级')
  assert.deepEqual(summary.stats, [
    { label: '金币', value: 128 },
    { label: '钻石', value: 6 },
    { label: '贴纸', value: 12 },
  ])
  assert.equal(summary.petText, '小星 Lv.5 · 兴奋')
  assert.equal(summary.achievementText, '已解锁 9 个成就')
})

test('mine profile summary falls back to friendly defaults', () => {
  const summary = buildMineProfileSummary({})

  assert.equal(summary.nickname, '小朋友')
  assert.equal(summary.avatar, '/static/logo.png')
  assert.equal(summary.levelText, 'Lv.1 趣学探索者')
  assert.deepEqual(summary.stats, [
    { label: '金币', value: 0 },
    { label: '钻石', value: 0 },
    { label: '贴纸', value: 0 },
  ])
  assert.equal(summary.petText, '小星 Lv.1 · 开心')
  assert.equal(summary.achievementText, '已解锁 0 个成就')
})
