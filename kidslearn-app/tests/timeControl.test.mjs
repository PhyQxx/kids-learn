import test from 'node:test'
import assert from 'node:assert/strict'

import {
  evaluateLearningAccess,
  normalizeTimeControl
} from '../utils/timeControl.mjs'

test('normalizes parent time control with practical defaults', () => {
  const control = normalizeTimeControl({
    dailyLimitMinutes: '45',
    allowedStartTime: '07:30',
    allowedEndTime: '20:15',
    restReminder: true,
    autoLockAfterTask: true
  })

  assert.deepEqual(control, {
    dailyLimitMinutes: 45,
    allowedStartTime: '07:30',
    allowedEndTime: '20:15',
    restReminder: true,
    autoLockAfterTask: true
  })

  assert.deepEqual(normalizeTimeControl({}), {
    dailyLimitMinutes: 60,
    allowedStartTime: '08:00',
    allowedEndTime: '21:00',
    restReminder: true,
    autoLockAfterTask: false
  })
})

test('blocks learning when the daily limit has been reached', () => {
  const result = evaluateLearningAccess({
    timeControl: { dailyLimitMinutes: 30, allowedStartTime: '08:00', allowedEndTime: '21:00', restReminder: true },
    todayMinutes: 30,
    now: '2026-05-24T10:00:00+08:00'
  })

  assert.equal(result.allowed, false)
  assert.equal(result.reasonCode, 'DAILY_LIMIT_REACHED')
  assert.match(result.message, /30/)
})

test('blocks learning outside the allowed time window', () => {
  const result = evaluateLearningAccess({
    timeControl: { dailyLimitMinutes: 60, allowedStartTime: '09:00', allowedEndTime: '20:00', restReminder: true },
    todayMinutes: 10,
    now: '2026-05-24T08:30:00+08:00'
  })

  assert.equal(result.allowed, false)
  assert.equal(result.reasonCode, 'OUTSIDE_ALLOWED_TIME')
  assert.match(result.message, /09:00/)
})

test('allows learning inside limits and supports overnight windows', () => {
  assert.equal(evaluateLearningAccess({
    timeControl: { dailyLimitMinutes: 60, allowedStartTime: '08:00', allowedEndTime: '21:00', restReminder: true },
    todayMinutes: 20,
    now: '2026-05-24T10:00:00+08:00'
  }).allowed, true)

  assert.equal(evaluateLearningAccess({
    timeControl: { dailyLimitMinutes: 60, allowedStartTime: '21:00', allowedEndTime: '07:00', restReminder: true },
    todayMinutes: 20,
    now: '2026-05-24T22:00:00+08:00'
  }).allowed, true)
})
