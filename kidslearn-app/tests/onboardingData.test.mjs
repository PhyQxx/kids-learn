import test from 'node:test'
import assert from 'node:assert/strict'

import {
  getOnboardingPayload,
  normalizePetOptions
} from '../utils/onboardingData.mjs'

test('normalizes available pets when request layer already unwraps data', () => {
  const pets = normalizePetOptions([
    { id: 1, petName: '星小猫', imageUrl: '🐱' },
    { id: 2, petName: '骨头小狗', imageUrl: '🐶' }
  ])

  assert.deepEqual(pets.map(pet => [pet.id, pet.petName, pet.imageUrl]), [
    [1, '星小猫', '🐱'],
    [2, '骨头小狗', '🐶']
  ])
})

test('normalizes available pets from raw response envelope', () => {
  const pets = normalizePetOptions({
    code: 200,
    data: [{ id: 3, petName: '月亮兔', imageUrl: '🐰' }]
  })

  assert.deepEqual(pets.map(pet => pet.petName), ['月亮兔'])
})

test('returns empty pet options for unexpected response shapes', () => {
  assert.deepEqual(normalizePetOptions({ code: 200, data: null }), [])
  assert.deepEqual(normalizePetOptions(null), [])
})

test('unwraps onboarding payloads from direct and enveloped responses', () => {
  assert.deepEqual(getOnboardingPayload({ id: 1, petName: '星小猫' }), { id: 1, petName: '星小猫' })
  assert.deepEqual(getOnboardingPayload({ code: 200, data: { id: 1, petName: '星小猫' } }), { id: 1, petName: '星小猫' })
})

