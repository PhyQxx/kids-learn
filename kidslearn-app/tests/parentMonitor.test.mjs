import test from 'node:test'
import assert from 'node:assert/strict'

import {
  normalizeMonitorSnapshot,
  reduceMonitorEvent
} from '../utils/parentMonitor.mjs'

test('normalizes parent monitor snapshot and derives summary totals', () => {
  const snapshot = normalizeMonitorSnapshot({
    family: { familyName: 'Happy Home', inviteCode: 'ABCD' },
    children: [
      {
        childId: 7,
        nickname: 'Ming',
        online: true,
        status: 'LEARNING',
        todayMinutes: 18,
        dailyLimitMinutes: 40,
        completedLevels: 2,
        accuracy: 83,
        currentCourseName: 'Math',
        currentLevelName: 'Level 3',
        lastActivityAt: '2026-05-07T18:10:00'
      },
      {
        userId: 8,
        nickname: '',
        online: false,
        todayMinutes: -5,
        dailyLimitMinutes: 30,
        completedLevels: 0,
        accuracy: 0
      }
    ],
    generatedAt: '2026-05-07T18:15:00'
  })

  assert.deepEqual(snapshot.summary, {
    childCount: 2,
    onlineCount: 1,
    learningCount: 1,
    todayMinutes: 18,
    completedLevels: 2,
    alertCount: 0
  })
  assert.equal(snapshot.children[0].remainingMinutes, 22)
  assert.equal(snapshot.children[0].statusText, '学习中')
  assert.equal(snapshot.children[1].childId, 8)
  assert.equal(snapshot.children[1].nickname, '孩子')
  assert.equal(snapshot.children[1].todayMinutes, 0)
})

test('reduces child activity realtime event into the latest monitor state', () => {
  const current = normalizeMonitorSnapshot({
    children: [
      { childId: 7, nickname: 'Ming', todayMinutes: 10, dailyLimitMinutes: 40, completedLevels: 1 },
      { childId: 8, nickname: 'Hong', todayMinutes: 5, dailyLimitMinutes: 30, completedLevels: 0 }
    ]
  })

  const next = reduceMonitorEvent(current, {
    type: 'CHILD_ACTIVITY_UPDATE',
    payload: {
      childId: 7,
      online: true,
      status: 'RESTING',
      todayMinutes: 22,
      completedLevels: 2,
      latestScore: 90,
      lastActivityAt: '2026-05-07T18:20:00'
    },
    timestamp: '2026-05-07T18:20:00'
  })

  assert.equal(next.children[0].todayMinutes, 22)
  assert.equal(next.children[0].remainingMinutes, 18)
  assert.equal(next.children[0].statusText, '休息中')
  assert.equal(next.children[0].latestScore, 90)
  assert.equal(next.children[1].todayMinutes, 5)
  assert.deepEqual(next.summary, {
    childCount: 2,
    onlineCount: 1,
    learningCount: 0,
    todayMinutes: 27,
    completedLevels: 2,
    alertCount: 0
  })
})
