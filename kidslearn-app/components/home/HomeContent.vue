<template>
  <view class="home-content">
    <!-- Loading -->
    <FunLoadingState v-if="loading" title="正在准备星球任务" mascot="🚀" />

    <template v-else>
      <!-- 任务横幅 -->
      <view class="task-banner" @tap="$emit('go-learn')">
        <view class="banner-info">
          <text class="banner-emoji animate-float">🚀</text>
          <view class="banner-text">
            <text class="banner-kicker">今日小目标</text>
            <text class="banner-title">先完成 {{ Math.max(totalTasks - completedTasks, 0) }} 个学习任务</text>
            <text class="banner-desc">已完成 {{ completedTasks }}/{{ totalTasks }} 个，点这里继续闯关</text>
          </view>
        </view>
        <view class="banner-progress">
          <tn-line-progress :percent="taskProgress" active-color="#FFE66D" inactive-color="rgba(255,255,255,0.3)" :height="12" :show-percent="false" />
        </view>
      </view>

      <!-- 签到入口 -->
      <view class="checkin-banner card card-hover" @tap="openCheckin">
        <view class="checkin-icon-wrap">
          <text class="checkin-emoji">📅</text>
        </view>
        <view class="checkin-info">
          <text class="text-md text-bold" style="color: #2C3E50;">每日签到</text>
          <text class="text-xs text-light">{{ checkinStreak > 0 ? `已连续签到 ${checkinStreak} 天` : '点击签到领奖励' }}</text>
        </view>
        <view class="checkin-action-btn" :class="{ 'done': checkinDone }">
          <text class="text-sm text-bold" :style="{ color: checkinDone ? '#2ECC71' : '#FFF' }">{{ checkinDone ? '已签到' : '去签到' }}</text>
        </view>
      </view>

      <!-- 排名横幅 -->
      <view class="rank-banner card card-hover" @tap="goRanking">
        <view class="rank-info">
          <text class="rank-emoji">🏆</text>
          <view class="rank-text">
            <text class="rank-title">本周排行</text>
            <text class="rank-desc">{{ myRankText }}</text>
          </view>
        </view>
        <view class="rank-action">
          <text>查看</text>
        </view>
      </view>

      <!-- 学科网格 (专项练习) -->
      <view class="section-title-row">
        <view class="section-title-copy">
          <text class="text-lg text-bold">📚 今日练习</text>
          <text class="section-hint">选择学科进行无尽刷题</text>
        </view>
        <text class="text-primary text-sm section-link" @tap="$emit('go-learn')">全部模式</text>
      </view>
      <view class="subject-grid stagger-list">
        <view
          v-for="subject in subjects"
          :key="subject.id"
          class="subject-card card card-hover"
          :class="{ locked: subject.locked }"
          :style="{ background: subject.locked ? '#F5F5F5' : subject.bg }"
          @tap="!subject.locked && $emit('go-subject', subject)"
        >
          <view class="subject-icon-wrap" style="background: rgba(255,255,255,0.6);">
            <text class="subject-emoji">{{ subject.icon }}</text>
          </view>
          <text class="subject-name" :style="{ color: subject.color }">{{ subject.name }}</text>
          <view class="subject-practice-tag" :style="{ color: subject.color, background: 'rgba(255,255,255,0.8)' }">
            <text class="text-xs">去练习 →</text>
          </view>
        </view>
      </view>

      <!-- 薄弱点推荐 -->
      <view v-if="weakPoints.length > 0" class="section-title-row">
        <view class="section-title-copy">
          <text class="text-lg text-bold">🎯 薄弱点突击</text>
          <text class="section-hint">AI 针对错题为你推荐</text>
        </view>
        <text class="text-primary text-sm section-link" @tap="goWrongTopics">错题本</text>
      </view>
      <view v-if="weakPoints.length > 0" class="weak-grid stagger-list">
        <view
          v-for="wp in weakPoints"
          :key="wp.subjectId"
          class="weak-card card card-hover"
          @tap="goAdaptivePractice(wp)"
        >
          <view class="weak-header">
            <view class="weak-icon-wrap">
              <text class="weak-icon">{{ wp.subjectIcon }}</text>
            </view>
            <view class="weak-info">
              <text class="text-md text-bold" style="color: #2C3E50;">{{ wp.subjectName }}</text>
              <view class="weak-stats">
                <text class="weak-stat-tag">错题 {{ wp.wrongCount }} 道</text>
                <text class="weak-stat-tag" style="background: #E8F8F0; color: #2ECC71;">正确率 {{ wp.accuracy }}%</text>
              </view>
            </view>
          </view>
          <view class="weak-action-btn">
            <text class="text-white text-sm text-bold">立即突击</text>
          </view>
        </view>
      </view>

      <!-- 快捷入口 -->
      <view class="section-title-row" style="margin-top: 8px;">
        <view class="section-title-copy">
          <text class="text-lg text-bold">⭐ 快捷探索</text>
        </view>
      </view>
      <view class="quick-grid">
        <view class="quick-card card card-hover pet-quick" @tap="goPet">
          <view class="quick-pet-info">
            <text class="pet-emoji animate-bounce">{{ petStore.currentImageUrl }}</text>
            <view class="pet-text-area">
              <text class="pet-name text-bold">{{ petStore.name }}</text>
              <text class="pet-status-text text-xs text-light">{{ petStore.moodText }} · Lv.{{ petStore.level }}</text>
            </view>
          </view>
          <view class="quick-arrow">→</view>
        </view>
        <view class="quick-card card card-hover achievement-quick" @tap="goAchievement">
          <view class="quick-ach-info">
            <text class="ach-emoji animate-pulse">🏅</text>
            <view class="ach-text-area">
              <text class="text-white text-md text-bold">成就中心</text>
              <text class="text-white text-xs" style="opacity: 0.8;">已解锁 {{ achievementCount }} 个</text>
            </view>
          </view>
          <view class="quick-arrow" style="color: rgba(255,255,255,0.5);">→</view>
        </view>
      </view>

      <!-- 排行速览 -->
      <view class="ranking-quick card">
        <view class="ranking-header">
          <text class="text-md text-bold" style="color: #2C3E50;">🏆 排行榜风云</text>
          <text class="text-primary text-sm" @tap="goRanking" style="cursor: pointer;">查看更多</text>
        </view>
        <view class="ranking-list">
          <view v-for="(r, i) in topRankers" :key="r.id || i" class="ranking-item">
            <view class="rank-medal-wrap">
              <text class="rank-medal">{{ ['🥇','🥈','🥉'][i] }}</text>
            </view>
            <text class="rank-name text-sm text-bold">{{ r.name }}</text>
            <text class="rank-score text-xs text-primary">{{ r.score }} 分</text>
          </view>
          <view v-if="topRankers.length === 0" class="ranking-empty">
            <text class="text-xs text-light">暂无排行数据，快去闯关上榜吧！</text>
          </view>
        </view>
      </view>
    </template>

    <!-- 签到弹窗 -->
    <CheckinPopup v-if="showCheckin" :auto-close-if-done="checkinAutoOpen" @close="onCheckinClose" />
  </view>
</template>

<script setup>
import { ref, computed, inject, onMounted, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { usePetStore } from '@/store/pet'
import { useLearnStore } from '@/store/learn'
import { useUserStore } from '@/store/user'
import { getDailyTasks, getSubjects, getCheckinStatus, getWeakPoints } from '@/api/learn'
import { getPetStatus } from '@/api/pet'
import { getRanking } from '@/api/ranking'
import { getMyProgress } from '@/api/achievement'
import { createHomeDataRequests } from '@/utils/homeData.mjs'
import { getAutoCheckinDecision } from '@/utils/promptFlow.mjs'
import CheckinPopup from '@/components/common/CheckinPopup.vue'
import FunLoadingState from '@/components/common/FunLoadingState.vue'

defineEmits(['go-subject', 'go-learn'])

const switchTab = inject('switchTab', () => {})
const gradePopupVisible = inject('gradePopupVisible', ref(false))
const petStore = usePetStore()
const learnStore = useLearnStore()
const userStore = useUserStore()

const loading = ref(true)
const completedTasks = ref(0)
const totalTasks = ref(5)
const myRank = ref(null)
const achievementCount = ref(0)
const showCheckin = ref(false)
const checkinAutoOpen = ref(false)
const checkinAutoOpened = ref(false)
const pendingAutoCheckin = ref(false)
const checkinDone = ref(false)
const checkinStreak = ref(0)
const weakPoints = ref([])
const gradeLevelId = computed(() => userStore.userInfo?.gradeLevelId || null)
const gradeSetupRequired = computed(() => !userStore.userInfo?.gradeLevelId)

const taskProgress = computed(() =>
  totalTasks.value ? Math.round(completedTasks.value / totalTasks.value * 100) : 0
)

const myRankText = computed(() =>
  myRank.value ? `第 ${myRank.value} 名 · 继续加油！` : '暂无排名，快去闯关吧'
)

const subjects = ref([
  { id: 1, name: '语文', icon: '📖', bg: '#FFF0F0', color: '#FF6B6B', locked: false },
  { id: 2, name: '数学', icon: '🔢', bg: '#E8F0FE', color: '#4A90D9', locked: false },
  { id: 3, name: '英语', icon: '🔤', bg: '#E0F7F7', color: '#4ECDC4', locked: false },
  { id: 4, name: '逻辑', icon: '🧩', bg: '#F3E8FF', color: '#9B59B6', locked: false },
  { id: 5, name: '科学', icon: '🔬', bg: '#E8F8F0', color: '#2ECC71', locked: false },
  { id: 6, name: '音乐', icon: '🎵', bg: '#FFF8E0', color: '#F1C40F', locked: true }
])

const topRankers = ref([])

// Mock 数据 fallback
function applyMockData() {
  completedTasks.value = 2
  totalTasks.value = 5
  myRank.value = null
  achievementCount.value = 32
}

async function loadData() {
  loading.value = true
  try {
    const results = await Promise.allSettled(createHomeDataRequests({
      getDailyTasks,
      getSubjects,
      getPetStatus,
      getRanking,
      getMyProgress
    }, gradeLevelId.value))

    // 今日任务
    if (results[0].status === 'fulfilled' && results[0].value) {
      const tasks = results[0].value.tasks || results[0].value || []
      learnStore.setDailyTasks(tasks)
      completedTasks.value = tasks.filter(t => t.status === 'COMPLETED').length
      totalTasks.value = tasks.length || 5
    }

    // 学科列表
    if (results[1].status === 'fulfilled' && results[1].value) {
      const list = results[1].value
      subjects.value = (Array.isArray(list) ? list : []).map(s => {
        const bgMap = { 'CHINESE': '#FFF0F0', 'MATH': '#E8F0FE', 'ENGLISH': '#E0F7F7', 'LOGIC': '#F3E8FF', 'SCIENCE': '#E8F8F0', 'MUSIC': '#FFF8E0' }
        const colorMap = { 'CHINESE': '#FF6B6B', 'MATH': '#4A90D9', 'ENGLISH': '#4ECDC4', 'LOGIC': '#9B59B6', 'SCIENCE': '#2ECC71', 'MUSIC': '#F1C40F' }
        const code = s.code || s.subjectCode || ''
        return {
          id: s.id,
          name: s.name || s.subjectName,
          icon: s.icon || s.iconUrl || { 'CHINESE': '📖', 'MATH': '🔢', 'ENGLISH': '🔤', 'LOGIC': '🧩', 'SCIENCE': '🔬', 'MUSIC': '🎵' }[code] || '📚',
          bg: bgMap[code] || '#F5F5F5',
          color: colorMap[code] || '#666',
          locked: s.status === 0 || s.isVip || s.locked
        }
      })
    }

    // 宠物状态
    if (results[2].status === 'fulfilled' && results[2].value) {
      petStore.setPetInfo(results[2].value)
    }

    // 排名
    if (results[3].status === 'fulfilled' && results[3].value) {
      const list = results[3].value
      if (Array.isArray(list)) {
        const me = list.find(r => r.isMe)
        if (me) {
          myRank.value = list.indexOf(me) + 1
        }
        topRankers.value = list.slice(0, 3).map(r => ({
          id: r.id,
          name: r.nickname || r.name || '未知',
          score: r.score || 0
        }))
      } else {
        topRankers.value = []
      }
    }

    // 成就数
    if (results[4].status === 'fulfilled' && results[4].value) {
      const prog = results[4].value
      achievementCount.value = prog.completedAchievements || 0
    }
  } catch (e) {
    console.log('HomeContent: 使用模拟数据', e)
    applyMockData()
  } finally {
    loading.value = false
    // 加载薄弱点推荐（不阻塞主流程）
    getWeakPoints().then(res => {
      if (Array.isArray(res) && res.length > 0) {
        weakPoints.value = res
      }
    }).catch(() => {})
  }
}

let isFirstShow = true

onMounted(() => {
  loadData()
  fetchCheckinStatus()
  setTimeout(() => {
    tryAutoOpenCheckin()
  }, 800)
})

// 返回时刷新数据
onShow(() => {
  if (isFirstShow) {
    isFirstShow = false
    return
  }
  loadData()
  fetchCheckinStatus()
})

watch(gradeLevelId, () => {
  learnStore.clearLearningContext()
  subjects.value = []
  loadData()
})

watch([gradePopupVisible, gradeSetupRequired], () => {
  if (pendingAutoCheckin.value) {
    tryAutoOpenCheckin()
  }
})

async function fetchCheckinStatus() {
  try {
    const res = await getCheckinStatus()
    if (res) {
      checkinDone.value = res.checkedIn || false
      checkinStreak.value = res.streak || 0
    }
  } catch (e) {
    // ignore
  }
}

function openCheckin() {
  checkinAutoOpen.value = false
  showCheckin.value = true
}

function tryAutoOpenCheckin() {
  const decision = getAutoCheckinDecision({
    gradePopupVisible: !!gradePopupVisible.value,
    gradeSetupRequired: gradeSetupRequired.value,
    alreadyAutoOpened: checkinAutoOpened.value
  })

  pendingAutoCheckin.value = decision.shouldRememberPending
  if (!decision.shouldOpen) return

  checkinAutoOpen.value = true
  showCheckin.value = true
  checkinAutoOpened.value = true
}

function onCheckinClose() {
  showCheckin.value = false
  fetchCheckinStatus()
}

function goRanking() { switchTab('ranking') }
function goPet() { switchTab('pet') }
function goAchievement() { switchTab('achievement') }
function goWrongTopics() { uni.navigateTo({ url: '/pages/mine/wrong' }) }
function goAdaptivePractice(wp) {
  uni.navigateTo({ url: `/pages/learn/quiz?practiceModeId=${wp.subjectId}&timeLimit=0` })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.home-content { display: flex; flex-direction: column; gap: 16px; }

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
}

.task-banner {
  background:
    linear-gradient(135deg, rgba(255, 122, 89, 0.96), rgba(255, 160, 111, 0.96)),
    radial-gradient(circle at 80% 18%, rgba(255, 255, 255, 0.34), transparent 28%);
  border-radius: $radius-xl;
  padding: 24px; display: flex; align-items: center; justify-content: space-between; cursor: pointer;
  box-shadow: 0 14px 32px rgba(255, 122, 89, 0.22);
  transition: transform 0.2s ease;
  &:active { transform: scale(0.98); }
}
.banner-info { display: flex; align-items: center; gap: 12px; }
.banner-emoji { font-size: 52px; }
.banner-text { display: flex; flex-direction: column; }
.banner-kicker { font-size: 13px; color: rgba(255,255,255,0.82); font-weight: 700; }
.banner-title { font-size: 22px; font-weight: 800; color: $white; line-height: 1.25; }
.banner-desc { font-size: 14px; color: rgba(255,255,255,0.86); margin-top: 4px; }
.progress-fill-accent { height: 100%; border-radius: 5px; background: $accent; }

.checkin-banner {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 20px;
  border-radius: $radius-lg;
  background: #FFF;
}
.checkin-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  background: #FFF8E6;
  display: flex;
  align-items: center;
  justify-content: center;
}
.checkin-emoji { font-size: 28px; }
.checkin-info { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.checkin-action-btn {
  padding: 8px 16px;
  border-radius: 20px;
  background: $primary;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(74, 144, 217, 0.2);
  &.done {
    background: #E8F8F0;
    box-shadow: none;
  }
}

.rank-banner {
  background: linear-gradient(135deg, #FFF9E6, #FFF0C2);
  border-radius: $radius-lg;
  padding: 16px 20px; display: flex; align-items: center; justify-content: space-between; cursor: pointer;
}
.rank-info { display: flex; align-items: center; gap: 14px; }
.rank-emoji { font-size: 36px; }
.rank-text { display: flex; flex-direction: column; gap: 2px; }
.rank-title { font-size: 16px; font-weight: 800; color: #D4AC0D; }
.rank-desc { font-size: 13px; color: #B7950B; }
.rank-action {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255,255,255,0.6);
  color: #D4AC0D;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 800;
}

.section-title-row { display: flex; align-items: center; justify-content: space-between; margin-top: 4px; }
.section-title-copy { display: flex; flex-direction: column; gap: 2px; }
.section-hint { font-size: 13px; color: $text-light; }
.section-link {
  padding: 6px 14px;
  border-radius: 16px;
  background: #F0F7FF;
  font-weight: 800;
  &:active { background: #E0F0FF; }
}

.subject-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.subject-card {
  min-height: 150px;
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: none;
  transition: transform 0.2s ease;
  &:active { transform: scale(0.95); opacity: 0.9; }
  &.locked { opacity: 0.5; }
}
.subject-icon-wrap {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}
.subject-emoji { font-size: 36px; }
.subject-name { font-size: 17px; font-weight: 800; display: block; margin-bottom: 8px; }
.subject-practice-tag {
  padding: 4px 12px;
  border-radius: 12px;
  font-weight: 800;
}

.weak-grid { display: flex; flex-direction: column; gap: 12px; }
.weak-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border: 1px solid rgba(231, 76, 60, 0.1);
  background: #FFF;
}
.weak-header { display: flex; align-items: center; gap: 14px; }
.weak-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  background: #F8F9FA;
  display: flex;
  align-items: center;
  justify-content: center;
}
.weak-icon { font-size: 28px; }
.weak-info { display: flex; flex-direction: column; gap: 6px; }
.weak-stats { display: flex; gap: 8px; }
.weak-stat-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 6px;
  background: #FFF0F0;
  color: #E74C3C;
  font-weight: bold;
}
.weak-action-btn {
  padding: 8px 16px;
  border-radius: 20px;
  background: linear-gradient(135deg, #E74C3C, #C0392B);
  box-shadow: 0 4px 12px rgba(231, 76, 60, 0.2);
}

.quick-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.quick-card {
  display: flex; align-items: center; justify-content: space-between;
  min-height: 96px;
  padding: 20px 24px; cursor: pointer;
  border: none;
  &:active { transform: scale(0.96); opacity: 0.9; }
}
.pet-quick { background: linear-gradient(135deg, #FFF, #F9F9F9); }
.quick-pet-info { display: flex; align-items: center; gap: 12px; }
.pet-emoji { font-size: 42px; }
.pet-text-area { display: flex; flex-direction: column; gap: 4px; }
.pet-name { font-size: 16px; color: #2C3E50; }
.pet-status-text { color: #7F8C8D; }
.quick-arrow { font-size: 20px; color: #BDC3C7; font-weight: bold; }

.achievement-quick {
  background: linear-gradient(135deg, #9B59B6, #8E44AD);
  box-shadow: 0 8px 24px rgba(155, 89, 182, 0.3);
}
.quick-ach-info { display: flex; align-items: center; gap: 12px; }
.ach-emoji { font-size: 38px; }
.ach-text-area { display: flex; flex-direction: column; gap: 4px; }

.ranking-quick {
  padding: 20px;
  background: #FFF;
  border-top: 4px solid #F1C40F;
}
.ranking-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.ranking-list { display: flex; flex-direction: column; gap: 12px; }
.ranking-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: #F8F9FA;
  border-radius: 12px;
}
.rank-medal-wrap {
  width: 32px;
  height: 32px;
  background: #FFF;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(0,0,0,0.05);
}
.rank-medal { font-size: 18px; }
.rank-name { flex: 1; color: #34495E; }
.rank-score { font-size: 14px; }
.ranking-empty { padding: 12px 0; text-align: center; }

/* 响应式 */
@media (max-width: 800px) {
  .subject-grid { grid-template-columns: repeat(3, 1fr); gap: 10px; }
  .subject-emoji { font-size: 32px; }
  .subject-card { padding: 16px 12px; min-height: 130px; }
  .quick-grid { grid-template-columns: 1fr; }
  .banner-emoji { font-size: 42px; }
  .rank-emoji { font-size: 32px; }
  .pet-emoji { font-size: 36px; }
  .ranking-quick { display: none; }
}

@media (max-width: 640px) {
  .home-content { gap: 16px; }
  .task-banner {
    align-items: flex-start;
    flex-direction: column;
    gap: 16px;
    padding: 20px;
  }
  .banner-progress { width: 100%; }
  .banner-title { font-size: 20px; }
  .rank-banner { padding: 16px; }
  .subject-grid { grid-template-columns: repeat(2, 1fr); }
  .subject-card { min-height: 140px; }
  .weak-card { flex-wrap: wrap; gap: 12px; }
}
</style>
