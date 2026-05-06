import test from 'node:test'
import assert from 'node:assert/strict'
import { claimAchievementReward, claimAllAchievementRewards } from '../utils/achievementClaim.mjs'

test('marks achievement claimed only when reward request succeeds', async () => {
  const ach = { id: 3, claimed: false }
  const toasts = []

  await assert.rejects(
    claimAchievementReward(
      () => Promise.reject(new Error('network failed')),
      ach,
      { showToast: (toast) => toasts.push(toast) }
    )
  )

  assert.equal(ach.claimed, false)
  assert.equal(toasts[0].icon, 'none')
})

test('claims all unclaimed completed achievements and skips unavailable rewards', async () => {
  const achievements = [
    { id: 1, status: 'done', claimed: false },
    { id: 2, status: 'done', claimed: true },
    { id: 3, status: 'progress', claimed: false },
    { id: 4, status: 'done', claimed: false }
  ]
  const claimedIds = []
  const toasts = []

  const result = await claimAllAchievementRewards(
    (id) => {
      claimedIds.push(id)
      return Promise.resolve({ id })
    },
    achievements,
    { showToast: (toast) => toasts.push(toast) }
  )

  assert.deepEqual(claimedIds, [1, 4])
  assert.equal(achievements[0].claimed, true)
  assert.equal(achievements[1].claimed, true)
  assert.equal(achievements[2].claimed, false)
  assert.equal(achievements[3].claimed, true)
  assert.deepEqual(result, { total: 2, success: 2, failed: 0 })
  assert.equal(toasts.at(-1).icon, 'success')
})

test('keeps failed achievements unclaimed when claiming all rewards', async () => {
  const achievements = [
    { id: 1, status: 'done', claimed: false },
    { id: 2, status: 'done', claimed: false }
  ]
  const toasts = []

  const result = await claimAllAchievementRewards(
    (id) => id === 1 ? Promise.resolve({ id }) : Promise.reject(new Error('failed')),
    achievements,
    { showToast: (toast) => toasts.push(toast) }
  )

  assert.equal(achievements[0].claimed, true)
  assert.equal(achievements[1].claimed, false)
  assert.deepEqual(result, { total: 2, success: 1, failed: 1 })
  assert.equal(toasts.at(-1).icon, 'none')
})
