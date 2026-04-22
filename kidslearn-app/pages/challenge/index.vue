<template>
  <AppLayout title="挑战赛" active-nav="/pages/ranking/index">
    <view class="challenge-content">
      <!-- 赛季横幅 -->
      <view class="season-banner">
        <view class="season-info">
          <text class="season-emoji">⚔️</text>
          <view>
            <text class="text-lg text-bold text-white">春季挑战赛</text>
            <text class="text-sm text-white" style="opacity: 0.8;">剩余 3天 12小时</text>
          </view>
        </view>
        <tn-button shape="round" size="lg" style="background: rgba(255,255,255,0.2); color: #fff;">参加挑战</tn-button>
      </view>

      <!-- 挑战卡片 -->
      <view class="challenge-grid">
        <view v-for="c in challenges" :key="c.id" class="challenge-card card" :style="{ borderLeftColor: c.color }">
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
            <text class="text-lg text-bold">白银 III</text>
            <text class="text-xs text-light">距离晋级还需 200 分</text>
          </view>
        </view>
        <view class="progress-bar" style="width: 200px;">
          <view class="progress-fill" style="width: 60%; background: linear-gradient(90deg, #9B59B6, #8E44AD);"></view>
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
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { createChallenge, getChallengeRecords } from '@/api/challenge'

const challenges = ref([
  { id: 1, name: '好友对战', icon: '👥', tag: '好友', tagBg: '#E8F0FE', tagColor: '#4A90D9', color: '#4A90D9', desc: '邀请好友来一场知识对决', players: 128, reward: 20 },
  { id: 2, name: '排位赛', icon: '🏆', tag: '排位', tagBg: '#F3E8FF', tagColor: '#9B59B6', color: '#9B59B6', desc: '挑战更高段位', players: 256, reward: 50 },
  { id: 3, name: '限时挑战', icon: '⏱️', tag: '限时', tagBg: '#FFE8E8', tagColor: '#E74C3C', color: '#E74C3C', desc: '60秒内答对最多题', players: 96, reward: 30 },
  { id: 4, name: '团队赛', icon: '🎯', tag: '团队', tagBg: '#E8F8F0', tagColor: '#2ECC71', color: '#2ECC71', desc: '组队挑战其他队伍', players: 64, reward: 100 }
])

const history = ref([
  { id: 1, opponent: '小明', time: '5分钟前', score: '8:5', win: true },
  { id: 2, opponent: '小红', time: '1小时前', score: '4:7', win: false },
  { id: 3, opponent: '小华', time: '昨天', score: '9:3', win: true }
])

onMounted(async () => {
  try {
    const res = await getChallengeRecords()
    if (res && Array.isArray(res) && res.length > 0) {
      history.value = res.map(r => ({
        id: r.id,
        opponent: r.opponentName || '未知对手',
        time: r.playTime || '',
        score: `${r.myScore || 0}:${r.opponentScore || 0}`,
        win: r.isWin || false
      }))
    }
  } catch (e) {
    console.log('challenge: 使用模拟数据')
  }
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
</style>
