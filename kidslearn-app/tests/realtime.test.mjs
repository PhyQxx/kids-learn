import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildRealtimeUrl,
  parseRealtimeMessage,
  reduceBalanceMessage,
  shouldReconnect
} from '../utils/realtime.mjs'

test('builds websocket url from http api base url and token', () => {
  assert.equal(
    buildRealtimeUrl('http://127.0.0.1:19084/api/v1', 'abc 123'),
    'ws://127.0.0.1:19084/ws/realtime?token=abc%20123'
  )
  assert.equal(
    buildRealtimeUrl('https://example.com/api/v1/', 'token'),
    'wss://example.com/ws/realtime?token=token'
  )
})

test('parses valid realtime message and ignores malformed input', () => {
  assert.deepEqual(parseRealtimeMessage('{"type":"PET_STATUS_UPDATE","payload":{"hunger":80}}'), {
    type: 'PET_STATUS_UPDATE',
    payload: { hunger: 80 },
    timestamp: ''
  })

  assert.equal(parseRealtimeMessage('{bad json'), null)
  assert.equal(parseRealtimeMessage('{"payload":{}}'), null)
})

test('merges balance payload into current user info', () => {
  const current = { nickname: 'Kid', gold: 20, diamond: 3 }

  assert.deepEqual(reduceBalanceMessage(current, { gold: 45 }), {
    nickname: 'Kid',
    gold: 45,
    diamond: 3
  })

  assert.deepEqual(reduceBalanceMessage(null, { gold: 1 }), null)
})

test('reconnects only when user did not close the connection and token exists', () => {
  assert.equal(shouldReconnect({ closedByUser: false, token: 'token' }), true)
  assert.equal(shouldReconnect({ closedByUser: true, token: 'token' }), false)
  assert.equal(shouldReconnect({ closedByUser: false, token: '' }), false)
})
