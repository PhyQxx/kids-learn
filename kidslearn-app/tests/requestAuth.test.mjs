import test from 'node:test'
import assert from 'node:assert/strict'
import { handleUnauthorizedResponse } from '../utils/requestAuth.mjs'

test('handles HTTP 401 responses by logging out', () => {
  let loggedOut = false

  const handled = handleUnauthorizedResponse(
    { statusCode: 401, data: { msg: 'Unauthorized' } },
    { logout: () => { loggedOut = true } }
  )

  assert.equal(handled, true)
  assert.equal(loggedOut, true)
})

test('handles business 401 responses by logging out', () => {
  let loggedOut = false

  const handled = handleUnauthorizedResponse(
    { statusCode: 200, data: { code: 401, msg: 'Token expired' } },
    { logout: () => { loggedOut = true } }
  )

  assert.equal(handled, true)
  assert.equal(loggedOut, true)
})

test('ignores non-401 responses', () => {
  let loggedOut = false

  const handled = handleUnauthorizedResponse(
    { statusCode: 500, data: { msg: 'Server error' } },
    { logout: () => { loggedOut = true } }
  )

  assert.equal(handled, false)
  assert.equal(loggedOut, false)
})
