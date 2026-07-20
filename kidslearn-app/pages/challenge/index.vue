<template>
  <AppLayout title="PK对战">
    <view class="challenge-content">
      <!-- 赛季横幅 -->
      <view class="season-banner card">
        <view class="season-header">
          <text class="text-md text-bold text-white">{{ dashboard.season.name }}</text>
          <text class="season-time">{{ dashboard.season.remainingText }}</text>
        </view>
        <view class="season-tier-wrap">
          <view class="tier-icon-wrap">
            <text class="tier-icon">{{ getTierIcon(dashboard.tier.tierName) }}</text>
          </view>
          <view class="tier-info">
            <text class="tier-name">{{ dashboard.tier.tierName }}</text>
            <text class="tier-score">{{ dashboard.tier.points }} 分</text>
          </view>
        </view>
        <view class="tier-progress-area">
          <view class="progress-labels text-xs">
            <text style="color: rgba(255,255,255,0.9);">距下一段位 {{ dashboard.tier.nextTierName }}</text>
            <text style="color: #FFF;">还差 {{ dashboard.tier.pointsToNext }} 分</text>
          </view>
          <tn-line-progress :percent="dashboard.tier.progressPercent" active-color="#FFF" inactive-color="rgba(255,255,255,0.3)" :height="8" :show-percent="false" />
        </view>
      </view>

      <!-- 挑战模式列表 -->
      <view class="section-title">
        <text class="text-md text-bold">选择模式</text>
      </view>
      <view class="challenge-grid">
        <view v-for="c in challenges" :key="c.id" class="challenge-card card" :style="{ borderLeftColor: c.color }" @tap="startChallenge(c.type)">
          <view class="challenge-header">
            <text class="challenge-icon">{{ c.icon }}</text>
            <view class="challenge-title-area">
              <text class="text-md text-bold">{{ c.name }}</text>
              <view class="challenge-tag" :style="{ background: c.tagBg, color: c.tagColor }">
                <text class="text-xs">{{ c.tag }}</text>
              </view>
            </view>
          </view>
          <text class="text-sm text-light challenge-desc">{{ c.desc }}</text>
          <view class="challenge-meta">
            <text class="text-xs text-light">👥 {{ getPlayerCount(c.type) }}人参与</text>
            <text class="text-xs" style="color: #F1C40F; font-weight: bold;">胜 +{{ c.reward }}分</text>
          </view>
        </view>
      </view>

      <!-- 战绩统计 -->
      <view class="stats-card card">
        <view class="stats-row">
          <view class="stat-item">
            <text class="stat-value text-bold" style="color: #2C3E50;">{{ dashboard.stats.total }}</text>
            <text class="stat-label text-xs text-light">总场次</text>
          </view>
          <view class="stat-divider"></view>
          <view class="stat-item">
            <text class="stat-value text-bold" style="color: #2ECC71;">{{ dashboard.stats.winRate }}%</text>
            <text class="stat-label text-xs text-light">胜率</text>
          </view>
          <view class="stat-divider"></view>
          <view class="stat-item">
            <text class="stat-value text-bold" style="color: #FF6B6B;">{{ dashboard.stats.wins }}</text>
            <text class="stat-label text-xs text-light">胜场</text>
          </view>
        </view>
      </view>

      <!-- 最近战绩 -->
      <view class="section-title" style="margin-top: 8px;">
        <text class="text-md text-bold">最近战绩</text>
      </view>
      <view class="history-list">
        <view v-for="h in history" :key="h.id" class="history-card card">
          <view class="history-result" :class="h.win ? 'win' : 'loss'">
            <text class="text-md text-bold">{{ h.win ? '胜利' : '失败' }}</text>
            <text class="text-xs">{{ h.time }}</text>
          </view>
          <view class="history-detail">
            <text class="text-sm">VS {{ h.opponent }}</text>
            <text class="text-lg text-bold">{{ h.score }}</text>
          </view>
          <view class="history-changes">
            <text class="text-xs" :class="h.win ? 'text-success' : 'text-error'">
              {{ h.rankDelta > 0 ? '+' : '' }}{{ h.rankDelta }} 分
            </text>
            <text v-if="h.rewardGold > 0" class="text-xs text-primary">+{{ h.rewardGold }} 🪙</text>
          </view>
        </view>
        <view v-if="history.length === 0" class="history-empty">
          <text class="text-sm text-light">暂无对战记录，快去挑战吧！</text>
        </view>
      </view>
    </view>

    <!-- 匹配弹窗 -->
    <tn-popup v-model="matching" mode="center" width="85%" border-radius="32" :close-on-click-overlay="false">
      <view class="matching-popup" :class="matchState">
        <!-- 寻找对手阶段 -->
        <template v-if="matchState === 'searching'">
          <view class="radar-scan">
            <text class="radar-icon animate-spin">📡</text>
          </view>
          <text class="text-lg text-bold" style="margin: 24px 0 8px;">寻找实力相当的对手...</text>
          <view class="candidate-names">
            <text class="candidate-text animate-fade-in-out">{{ currentCandidate }}</text>
          </view>
          <tn-button style="margin-top: 30px;" shape="round" plain @click="cancelMatch">取消匹配</tn-button>
        </template>

        <!-- VS 阶段 -->
        <template v-else-if="matchState === 'vs'">
          <view class="vs-container">
            <view class="vs-clash">
              <view class="vs-avatar left animate-slide-in-left">
                <text class="avatar-emoji">👦</text>
                <text class="avatar-name">{{ userStore.userInfo?.nickname || '我' }}</text>
              </view>
              <view class="vs-text animate-pop-in">VS</view>
              <view class="vs-avatar right animate-slide-in-right">
                <text class="avatar-emoji">{{ matchedOpponent?.opponent?.avatar || '🦁' }}</text>
                <text class="avatar-name">{{ matchedOpponent?.opponent?.nickname || '神秘对手' }}</text>
              </view>
            </view>
            <view class="vs-ready-text animate-pulse">准备进入战场...</view>
          </view>
        </template>
      </view>
    </tn-popup>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { createChallenge, getChallengeDashboard, getChallengeRecords } from '@/api/challenge'
import { normalizeChallengeDashboard, normalizeChallengeRecords } from '@/utils/challengeData.mjs'
import { useUserStore } from '@/store/user'
import { soundManager } from '@/utils/sound'

const userStore = useUserStore()

const challenges = ref([
  { id: 1, type: 'FRIEND', name: '好友对战', icon: '👥', tag: '好友', tagBg: '#E8F0FE', tagColor: '#4A90D9', color: '#4A90D9', desc: '匹配好友或同水平对手', reward: 20 },
  { id: 2, type: 'RANKED', name: '排位赛', icon: '🏆', tag: '排位', tagBg: '#F3E8FF', tagColor: '#9B59B6', color: '#9B59B6', desc: '赢得积分挑战更高段位', reward: 20 },
  { id: 3, type: 'RANKED', name: '限时挑战', icon: '⏱️', tag: '限时', tagBg: '#FFE8E8', tagColor: '#E74C3C', color: '#E74C3C', desc: '快速答题冲击高分', reward: 20 },
  { id: 4, type: 'RANKED', name: '综合挑战', icon: '🎯', tag: '综合', tagBg: '#E8F8F0', tagColor: '#2ECC71', color: '#2ECC71', desc: '随机学科知识对决', reward: 20 }
])

const matching = ref(false)
const matchState = ref('idle') // idle, searching, vs
const matchedOpponent = ref(null)
const currentCandidate = ref('宇宙探索者')
const dashboard = ref(normalizeChallengeDashboard())
const history = ref([])

const candidates = ['数学小达人', '英语狂热者', '逻辑之王', '小小科学家', '诗词才子', '星际旅行者', '知识冒险家']
let candidateTimer = null

function getPlayerCount(type) {
  if (type === 'FRIEND') {
    return dashboard.value.players.friend || 0
  } else {
    // 将排位赛的人数粗略分配给不同的模式以显示不同数字
    const baseRanked = dashboard.value.players.ranked || 0
    return Math.floor(baseRanked * 0.8) + Math.floor(Math.random() * 5)
  }
}

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

let matchTimer = null
async function startChallenge(type) {
  matching.value = true
  matchState.value = 'searching'
  soundManager.play('tap')
  
  // 模拟滚动候选人
  let candidateIdx = 0
  candidateTimer = setInterval(() => {
    candidateIdx = (candidateIdx + 1) % candidates.length
    currentCandidate.value = candidates[candidateIdx]
  }, 200)

  matchTimer = setTimeout(async () => {
    try {
      const result = await createChallenge({ type, opponentId: null })
      clearInterval(candidateTimer)
      
      if (result && result.challengeId) {
        matchedOpponent.value = result
        matchState.value = 'vs'
        soundManager.play('popup') // 匹配成功音效
        
        // 展示 VS 动画 1.5 秒后进入
        setTimeout(() => {
          matching.value = false
          matchState.value = 'idle'
          const levelId = result.level?.id || 1
          const opponentId = result.opponent?.id || ''
          uni.navigateTo({
            url: `/pages/learn/quiz?levelId=${levelId}&challengeId=${result.challengeId}&opponentId=${opponentId}`
          })
        }, 2000)
      } else {
        matching.value = false
        matchState.value = 'idle'
        uni.showToast({ title: '暂时没有找到对手，请重试', icon: 'none' })
      }
    } catch (e) {
      clearInterval(candidateTimer)
      matching.value = false
      matchState.value = 'idle'
      uni.showToast({ title: e.message || '匹配失败', icon: 'none' })
    }
  }, 2500)
}

function cancelMatch() {
  if (matchTimer) clearTimeout(matchTimer)
  if (candidateTimer) clearInterval(candidateTimer)
  matching.value = false
  matchState.value = 'idle'
}

function getTierIcon(tierName) {
  const map = { '青铜': '🥉', '白银': '🥈', '黄金': '🥇', '铂金': '💎', '钻石': '💠', '星耀': '🌟', '王者': '👑' }
  for (let key in map) {
    if (tierName.includes(key)) return map[key]
  }
  return '🏅'
}

onMounted(() => {
  loadDashboard()
  loadRecords()
})

onUnmounted(() => {
  if (candidateTimer) clearInterval(candidateTimer)
  if (matchTimer) clearTimeout(matchTimer)
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.challenge-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 24px;
}

/* 赛季横幅 */
.season-banner {
  background: linear-gradient(135deg, #9B59B6, #8E44AD);
  border-radius: $radius-lg;
  padding: 20px;
  color: #FFF;
}
.season-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.season-time {
  font-size: 12px;
  background: rgba(255,255,255,0.2);
  padding: 4px 10px;
  border-radius: 12px;
}
.season-tier-wrap {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.tier-icon-wrap {
  width: 64px;
  height: 64px;
  background: rgba(255,255,255,0.15);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.tier-icon { font-size: 40px; }
.tier-info { display: flex; flex-direction: column; gap: 4px; }
.tier-name { font-size: 24px; font-weight: 800; text-shadow: 0 2px 4px rgba(0,0,0,0.2); }
.tier-score { font-size: 14px; opacity: 0.9; }

.tier-progress-area {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.progress-labels {
  display: flex;
  justify-content: space-between;
}

/* 模式列表 */
.section-title {
  padding: 0 4px;
}

.challenge-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.challenge-card {
  border-left: 4px solid $primary;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  cursor: pointer;
  transition: transform 0.2s ease;
  &:active { transform: scale(0.96); }
}

.challenge-header {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.challenge-icon { font-size: 28px; }
.challenge-title-area { display: flex; flex-direction: column; gap: 4px; }
.challenge-tag {
  align-self: flex-start;
  padding: 2px 8px;
  border-radius: 6px;
  font-weight: 600;
}

.challenge-desc {
  line-height: 1.4;
  height: 36px;
}

.challenge-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 8px;
  border-top: 1px dashed rgba(0,0,0,0.05);
}

/* 统计 */
.stats-card {
  padding: 16px 0;
}
.stats-row {
  display: flex;
  align-items: center;
}
.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.stat-value { font-size: 24px; }
.stat-divider {
  width: 1px;
  height: 30px;
  background: #F0F0F0;
}

/* 战绩 */
.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-card {
  display: flex;
  align-items: center;
  padding: 0;
  overflow: hidden;
}

.history-result {
  width: 80px;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #FFF;

  &.win { background: linear-gradient(135deg, #2ECC71, #27AE60); }
  &.loss { background: linear-gradient(135deg, #95A5A6, #7F8C8D); }
}

.history-detail {
  flex: 1;
  padding: 0 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.history-changes {
  padding: 0 16px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.history-empty {
  padding: 30px 0;
  text-align: center;
}

/* 匹配弹窗内容 */
.matching-popup {
  padding: 40px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 320px;
  justify-content: center;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.radar-scan {
  width: 110px;
  height: 110px;
  border-radius: 50%;
  background: rgba(74, 144, 217, 0.08);
  border: 3px solid rgba($primary, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    width: 100%;
    height: 100%;
    border-radius: 50%;
    border: 2px solid $primary;
    animation: ping 1.5s cubic-bezier(0, 0, 0.2, 1) infinite;
  }
}

.radar-icon { font-size: 52px; }

.candidate-names {
  height: 28px;
  overflow: hidden;
  margin-top: 4px;
}
.candidate-text {
  font-size: 15px;
  color: $primary;
  font-weight: 800;
  display: block;
}

/* VS 容器 */
.vs-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
}

.vs-clash {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  width: 100%;
}

.vs-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.avatar-emoji {
  font-size: 56px;
  width: 80px;
  height: 80px;
  background: #F8F9FA;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  border: 3px solid #FFF;
}

.avatar-name {
  font-size: 14px;
  font-weight: 800;
  color: #2C3E50;
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.vs-text {
  font-size: 42px;
  font-weight: 900;
  font-style: italic;
  color: #FF6B6B;
  text-shadow: 0 4px 0 #E74C3C, 0 8px 15px rgba(231, 76, 60, 0.4);
  padding: 0 10px;
}

.vs-ready-text {
  font-size: 16px;
  font-weight: 800;
  color: $text-secondary;
  letter-spacing: 1px;
}

/* 动画 */
@keyframes ping {
  75%, 100% {
    transform: scale(1.6);
    opacity: 0;
  }
}

.animate-slide-in-left {
  animation: slideInLeft 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275) both;
}
.animate-slide-in-right {
  animation: slideInRight 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275) both;
}
.animate-fade-in-out {
  animation: fadeInOut 0.2s ease infinite alternate;
}

@keyframes slideInLeft {
  from { transform: translateX(-100px); opacity: 0; }
  to { transform: translateX(0); opacity: 1; }
}
@keyframes slideInRight {
  from { transform: translateX(100px); opacity: 0; }
  to { transform: translateX(0); opacity: 1; }
}
@keyframes fadeInOut {
  from { opacity: 0.6; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

@include respond-sm {
  .challenge-grid {
    grid-template-columns: 1fr;
  }
  .challenge-card {
    border-left-width: 4px;
  }
  .vs-text { font-size: 32px; }
  .avatar-emoji { width: 64px; height: 64px; font-size: 42px; }
}
</style>
