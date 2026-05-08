export function normalizeChallengeDashboard(payload = {}) {
  const tier = payload.tier || {}
  const stats = payload.stats || {}
  const season = payload.season || {}
  const total = Number(stats.total || 0)
  const wins = Number(stats.wins || 0)

  return {
    tier: {
      tierName: tier.tierName || 'Bronze',
      points: Number(tier.points || 0),
      nextTierName: tier.nextTierName || '',
      pointsToNext: Number(tier.pointsToNext || 0),
      progressPercent: clampPercent(tier.progressPercent)
    },
    stats: {
      wins,
      losses: Number(stats.losses || 0),
      draws: Number(stats.draws || 0),
      total,
      winRate: total > 0 ? Math.round(wins * 100 / total) : 0
    },
    season: {
      name: season.name || 'Current Season',
      remainingText: season.remainingText || ''
    }
  }
}

export function normalizeChallengeRecords(records = []) {
  if (!Array.isArray(records)) return []
  return records.map((record, index) => ({
    id: record.id || index,
    opponent: record.opponentName || record.opponent || 'Unknown',
    time: record.playTime || record.time || '',
    score: `${Number(record.myScore || 0)}:${Number(record.opponentScore || 0)}`,
    win: Boolean(record.isWin || record.win),
    rankDelta: Number(record.rankDelta || 0),
    rewardGold: Number(record.rewardGold || 0)
  }))
}

export function normalizeRankingList(rows = []) {
  const normalized = Array.isArray(rows) ? rows.map((row, index) => normalizeRankingRow(row, index)) : []
  return {
    podium: normalized.slice(0, 3),
    list: normalized.slice(3),
    me: normalized.find(row => row.isMe) || defaultMe()
  }
}

function normalizeRankingRow(row, index) {
  const score = Number(row.score || 0)
  return {
    id: row.id || index,
    rank: Number(row.rank || index + 1),
    name: row.nickname || row.name || 'Unknown',
    avatar: row.avatar || 'boy',
    level: Number(row.level || 0),
    city: row.city || '',
    score,
    stars: Math.min(Math.floor(score / 500), 5),
    isMe: Boolean(row.isMe)
  }
}

function defaultMe() {
  return { rank: '-', name: 'Me', avatar: 'boy', level: 0, city: '', score: 0, stars: 0, isMe: true }
}

function clampPercent(value) {
  const numeric = Number(value || 0)
  return Math.max(0, Math.min(100, numeric))
}
