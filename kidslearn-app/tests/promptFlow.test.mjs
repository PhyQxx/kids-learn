import test from 'node:test'
import assert from 'node:assert/strict'

import { getAutoCheckinDecision } from '../utils/promptFlow.mjs'

test('defers auto checkin while grade setup popup is visible', () => {
  assert.deepEqual(getAutoCheckinDecision({ gradePopupVisible: true, alreadyAutoOpened: false }), {
    shouldOpen: false,
    shouldRememberPending: true
  })
})

test('defers auto checkin while grade setup is still required', () => {
  assert.deepEqual(getAutoCheckinDecision({
    gradePopupVisible: false,
    gradeSetupRequired: true,
    alreadyAutoOpened: false
  }), {
    shouldOpen: false,
    shouldRememberPending: true
  })
})

test('opens pending auto checkin after grade setup popup closes', () => {
  assert.deepEqual(getAutoCheckinDecision({ gradePopupVisible: false, alreadyAutoOpened: false }), {
    shouldOpen: true,
    shouldRememberPending: false
  })
})

test('does not reopen auto checkin after it has already opened once', () => {
  assert.deepEqual(getAutoCheckinDecision({ gradePopupVisible: false, alreadyAutoOpened: true }), {
    shouldOpen: false,
    shouldRememberPending: false
  })
})
