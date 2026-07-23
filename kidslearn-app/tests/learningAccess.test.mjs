import test from 'node:test'
import assert from 'node:assert/strict'

import { ensureLearningAccess } from '../utils/learningAccess.mjs'

test('allows learning when report and time control stay within limits', async () => {
  let modalMessage = ''

  const allowed = await ensureLearningAccess({
    fetchTimeControl: async () => ({ dailyLimitMinutes: 30, allowedStartTime: '08:00', allowedEndTime: '21:00', restReminder: true }),
    fetchReport: async () => ({ today: { learnMinutes: 12 } }),
    showBlockedMessage: message => { modalMessage = message },
    now: '2026-05-24T10:00:00+08:00'
  })

  assert.equal(allowed, true)
  assert.equal(modalMessage, '')
})

test('shows a blocking message when parent time control denies learning', async () => {
  let modalMessage = ''

  const allowed = await ensureLearningAccess({
    fetchTimeControl: async () => ({ dailyLimitMinutes: 30, allowedStartTime: '08:00', allowedEndTime: '21:00', restReminder: true }),
    fetchReport: async () => ({ today: { learnMinutes: 30 } }),
    showBlockedMessage: message => { modalMessage = message },
    now: '2026-05-24T10:00:00+08:00'
  })

  assert.equal(allowed, false)
  assert.match(modalMessage, /30/)
})

test('blocks new learning when parent APIs are unavailable', async () => {
  let modalMessage = ''
  const allowed = await ensureLearningAccess({
    fetchTimeControl: async () => { throw new Error('offline') },
    fetchReport: async () => ({ today: { learnMinutes: 30 } }),
    showBlockedMessage: message => { modalMessage = message },
    now: '2026-05-24T10:00:00+08:00'
  })

  assert.equal(allowed, false)
  assert.match(modalMessage, /无法验证/)
})

test('uses authoritative server access status when available', async () => {
  const allowed = await ensureLearningAccess({
    fetchAccessStatus: async () => ({ allowed: false, message: '今日学习已达到 30 分钟上限' }),
    showBlockedMessage: () => {}
  })
  assert.equal(allowed, false)
})
