import assert from 'node:assert/strict'
import test from 'node:test'
import {
  normalizeChallengeDashboard,
  normalizeChallengeRecords,
  normalizeRankingList
} from '../utils/challengeData.mjs'

test('normalizes challenge dashboard tier and season stats', () => {
  const dashboard = normalizeChallengeDashboard({
    tier: {
      tierName: 'Gold',
      points: 620,
      nextTierName: 'Platinum',
      pointsToNext: 280,
      progressPercent: 68
    },
    stats: { wins: 6, losses: 2, draws: 1, total: 9 },
    season: { name: 'Week 18', remainingText: '3 days' }
  })

  assert.equal(dashboard.tier.tierName, 'Gold')
  assert.equal(dashboard.tier.points, 620)
  assert.equal(dashboard.tier.progressPercent, 68)
  assert.equal(dashboard.stats.winRate, 67)
  assert.equal(dashboard.season.remainingText, '3 days')
})

test('normalizes challenge records for history rows', () => {
  const records = normalizeChallengeRecords([
    {
      id: 1,
      opponentName: 'Ada',
      myScore: 80,
      opponentScore: 60,
      isWin: true,
      rankDelta: 30,
      rewardGold: 20,
      playTime: '2026-05-06T18:00:00'
    }
  ])

  assert.deepEqual(records[0], {
    id: 1,
    opponent: 'Ada',
    time: '2026-05-06T18:00:00',
    score: '80:60',
    win: true,
    rankDelta: 30,
    rewardGold: 20
  })
})

test('normalizes ranking rows into podium, list, and current user', () => {
  const ranking = normalizeRankingList([
    { id: 1, nickname: 'A', avatar: 'A', level: 9, score: 900, rank: 1 },
    { id: 2, nickname: 'B', avatar: 'B', level: 8, score: 800, rank: 2 },
    { id: 3, nickname: 'C', avatar: 'C', level: 7, score: 700, rank: 3 },
    { id: 4, nickname: 'Me', avatar: 'M', level: 6, score: 600, rank: 4, isMe: true }
  ])

  assert.equal(ranking.podium[0].name, 'A')
  assert.equal(ranking.list[0].rank, 4)
  assert.equal(ranking.me.rank, 4)
  assert.equal(ranking.me.stars, 1)
})
