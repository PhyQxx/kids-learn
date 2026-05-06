import test from 'node:test'
import assert from 'node:assert/strict'
import { createHomeDataRequests } from '../utils/homeData.mjs'

test('home subject request uses current grade level id', async () => {
  const calls = []
  const api = {
    getDailyTasks: () => Promise.resolve([]),
    getSubjects: (gradeLevelId) => {
      calls.push(['getSubjects', gradeLevelId])
      return Promise.resolve([])
    },
    getPetStatus: () => Promise.resolve({}),
    getRanking: () => Promise.resolve([]),
    getMyProgress: () => Promise.resolve({})
  }

  await Promise.allSettled(createHomeDataRequests(api, 5))

  assert.deepEqual(calls, [['getSubjects', 5]])
})
