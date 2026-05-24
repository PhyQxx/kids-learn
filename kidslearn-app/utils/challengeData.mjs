export function normalizeChallengeDashboard(payload = {}) {
  const tier = payload.tier || {}
  const stats = payload.stats || {}
  const season = payload.season || {}
  const players = payload.players || {}
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
    },
    players: {
      ranked: Number(players.rankedPlayers || 0),
      friend: Number(players.friendPlayers || 0)
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
  let normalized = Array.isArray(rows) ? rows.map((row, index) => normalizeRankingRow(row, index)) : []

  return {
    podium: normalized.slice(0, 3),
    list: normalized.slice(3),
    me: normalized.find(row => row.isMe) || defaultMe()
  }
}

function normalizeRankingRow(row, index) {
  const score = Number(row.score || 0)
  // 如果用户没有头像，根据索引分配一个合适的默认动物或人物头像
  const defaultAvatars = ['👦', '👧', '🦁', '🐰', '🦊', '🐼', '🐯', '🐻'];
  const avatar = row.avatar || defaultAvatars[index % defaultAvatars.length];
  
  return {
    id: row.id || index,
    rank: Number(row.rank || index + 1),
    name: row.nickname || row.name || '学习新星',
    avatar: avatar,
    level: Number(row.level || Math.max(1, Math.floor(score / 100))),
    city: row.city || '未知',
    score,
    stars: Math.min(Math.floor(score / 500), 5),
    isMe: Boolean(row.isMe)
  }
}

function defaultMe() {
  return { rank: '-', name: '我', avatar: '👦', level: 1, city: '未知', score: 0, stars: 0, isMe: true }
}

function clampPercent(value) {
  const numeric = Number(value || 0)
  return Math.max(0, Math.min(100, numeric))
}
