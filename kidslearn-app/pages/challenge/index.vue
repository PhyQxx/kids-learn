<template>
  <AppLayout title="挑战赛" active-nav="/pages/ranking/index">
    <view class="challenge-content">
      <!-- 赛季横幅 -->
      <view class="season-banner">
        <view class="season-info">
          <text class="season-emoji">⚔️</text>
          <view>
            <text class="text-lg text-bold text-white">{{ dashboard.season.name }}</text>
            <text class="text-sm text-white" style="opacity: 0.8;">{{ dashboard.season.remainingText }}</text>
          </view>
        </view>
        <tn-button shape="round" size="lg" :loading="matching" style="background: rgba(255,255,255,0.2); color: #fff;" @click="startChallenge('RANKED')">参加挑战</tn-button>
      </view>

      <!-- 挑战卡片 -->
      <view class="challenge-grid">
        <view v-for="c in challenges" :key="c.id" class="challenge-card card" :style="{ borderLeftColor: c.color }" @tap="startChallenge(c.type)">
          <view class="challenge-header">
            <text class="challenge-icon">{{ c.icon }}</text>
            <view>
              <text class="text-md text-bold">{{ c.name }}</text>
              <view class="challenge-tag" :style="{ background: c.tagBg, color: c.tagColor }">
                <text class="text-xs">{{ c.tag }}</text>
              </view>
            </view>
          </view>
          <text class="text-sm text-light">{{ c.desc }}</text>
          <view class="challenge-meta">
            <text class="text-xs text-light">👥 {{ c.players }}人参与</text>
            <text class="text-xs text-light">🪙 {{ c.reward }} 金币</text>
          </view>
        </view>
      </view>

      <!-- 段位 -->
      <view class="rank-tier card">
        <view class="tier-info">
          <text class="tier-icon">🛡️</text>
          <view>
            <text class="text-lg text-bold">{{ dashboard.tier.tierName }}</text>
            <text class="text-xs text-light">
              {{ dashboard.tier.nextTierName ? `距离 ${dashboard.tier.nextTierName} 还需 ${dashboard.tier.pointsToNext} 分` : '已到达当前最高段位' }}
            </text>
          </view>
        </view>
        <view class="progress-bar" style="width: 200px;">
          <view class="progress-fill" :style="{ width: dashboard.tier.progressPercent + '%', background: 'linear-gradient(90deg, #9B59B6, #8E44AD)' }"></view>
        </view>
      </view>

      <view class="stats-grid">
        <view class="stat-card card">
          <text class="text-xs text-light">胜场</text>
          <text class="text-lg text-bold">{{ dashboard.stats.wins }}</text>
        </view>
        <view class="stat-card card">
          <text class="text-xs text-light">胜率</text>
          <text class="text-lg text-bold">{{ dashboard.stats.winRate }}%</text>
        </view>
        <view class="stat-card card">
          <text class="text-xs text-light">积分</text>
          <text class="text-lg text-bold">{{ dashboard.tier.points }}</text>
        </view>
      </view>

      <!-- 对战历史 -->
      <view class="history-card card">
        <text class="text-md text-bold" style="margin-bottom: 12px;">📜 对战历史</text>
        <view v-for="h in history" :key="h.id" class="history-row">
          <text class="history-result">{{ h.win ? '✅' : '❌' }}</text>
          <text class="text-sm">{{ h.opponent }}</text>
          <text class="text-xs text-light">{{ h.time }}</text>
          <text class="history-score" :class="{ win: h.win, lose: !h.win }">{{ h.score }}</text>
        </view>
        <view v-if="history.length === 0" class="history-empty">
          <text class="text-sm text-light">还没有对战记录，先来一局吧</text>
        </view>
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { createChallenge, getChallengeDashboard, getChallengeRecords } from '@/api/challenge'
import { normalizeChallengeDashboard, normalizeChallengeRecords } from '@/utils/challengeData.mjs'

const challenges = ref([
  { id: 1, type: 'FRIEND', name: '好友对战', icon: '👥', tag: '好友', tagBg: '#E8F0FE', tagColor: '#4A90D9', color: '#4A90D9', desc: '匹配好友或同水平对手', players: 128, reward: 20 },
  { id: 2, type: 'RANKED', name: '排位赛', icon: '🏆', tag: '排位', tagBg: '#F3E8FF', tagColor: '#9B59B6', color: '#9B59B6', desc: '赢得积分挑战更高段位', players: 256, reward: 20 },
  { id: 3, type: 'RANKED', name: '限时挑战', icon: '⏱️', tag: '限时', tagBg: '#FFE8E8', tagColor: '#E74C3C', color: '#E74C3C', desc: '快速答题冲击高分', players: 96, reward: 20 },
  { id: 4, type: 'RANKED', name: '综合挑战', icon: '🎯', tag: '综合', tagBg: '#E8F8F0', tagColor: '#2ECC71', color: '#2ECC71', desc: '随机学科知识对决', players: 64, reward: 20 }
])

const matching = ref(false)
const dashboard = ref(normalizeChallengeDashboard())
const history = ref([])

async function loadDashboard() {
  try {
    dashboard.value = normalizeChallengeDashboard(await getChallengeDashboard())
  } catch (e) {
    console.log('challenge: dashboard fallback', e)
  }
}

async function loadRecords() {
  try {
    history.value = normalizeChallengeRecords(await getChallengeRecords())
  } catch (e) {
    console.log('challenge: records fallback', e)
    history.value = []
  }
}

async function startChallenge(type) {
  if (matching.value) return
  matching.value = true
  try {
    const result = await createChallenge(type)
    const levelId = result?.level?.id || result?.levelId
    if (!levelId) {
      uni.showToast({ title: '暂无可挑战关卡', icon: 'none' })
      return
    }
    const opponentId = result?.opponent?.id || ''
    uni.navigateTo({
      url: `/pages/learn/quiz?levelId=${levelId}&challengeId=${result.challengeId}&opponentId=${opponentId}`
    })
  } catch (e) {
    uni.showToast({ title: '匹配失败，请稍后再试', icon: 'none' })
  } finally {
    matching.value = false
  }
}

onMounted(() => {
  loadDashboard()
  loadRecords()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.challenge-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.season-banner {
  background: linear-gradient(135deg, #9B59B6, #8E44AD);
  border-radius: $radius-md;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.season-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.season-emoji { font-size: 40px; }

.challenge-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.challenge-card {
  border-left: 4px solid;
  padding: 16px;
  cursor: pointer;
}

.challenge-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.challenge-icon { font-size: 28px; }

.challenge-tag {
  display: inline-flex;
  padding: 2px 8px;
  border-radius: 100px;
  margin-left: 6px;
}

.challenge-meta {
  display: flex;
  gap: 16px;
  margin-top: 8px;
}

.rank-tier {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.stat-card {
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tier-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tier-icon { font-size: 32px; }

.history-card { padding: 16px 20px; }

.history-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  & + .history-row { border-top: 1px solid #F5F5F5; }
}

.history-result { font-size: 16px; }
.history-score {
  margin-left: auto;
  font-size: 14px;
  font-weight: 600;
  &.win { color: $success; }
  &.lose { color: $error; }
}

.history-empty {
  padding: 16px 0 4px;
  text-align: center;
}
</style>
