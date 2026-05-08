import test from 'node:test'
import assert from 'node:assert/strict'

import { getPostAuthRedirectUrl } from '../utils/onboardingFlow.mjs'

test('routes new users at onboarding step 0 to pet selection onboarding', () => {
  assert.equal(getPostAuthRedirectUrl({ onboardingStep: 0 }), '/pages/onboarding/index')
})

test('routes users with completed onboarding to main page', () => {
  assert.equal(getPostAuthRedirectUrl({ onboardingStep: 3 }), '/pages/main/index')
})

