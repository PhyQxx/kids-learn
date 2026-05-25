import assert from 'node:assert/strict'
import test from 'node:test'
import { createChallengePayload } from '../utils/challengeData.mjs'

test('builds ranked challenge payload from a type string', () => {
  assert.deepEqual(createChallengePayload('RANKED'), { type: 'RANKED' })
})

test('builds challenge payload from options object without nesting type', () => {
  assert.deepEqual(createChallengePayload({ type: 'FRIEND', opponentId: 12 }), {
    type: 'FRIEND',
    opponentId: 12,
  })
})
