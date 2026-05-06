<template>
  <view class="home-content">
    <!-- Loading -->
    <view v-if="loading" class="loading-state">
      <tn-loading size="60" />
      <text class="text-sm text-light" style="margin-top: 12px;">加载中...</text>
    </view>

    <template v-else>
      <!-- 任务横幅 -->
      <view class="task-banner" @tap="$emit('go-learn')">
        <view class="banner-info">
          <text class="banner-emoji">🚀</text>
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
        <text class="checkin-emoji">📅</text>
        <view class="checkin-info">
          <text class="text-md text-bold">每日签到</text>
          <text class="text-xs text-light">{{ checkinStreak > 0 ? `已连续签到 ${checkinStreak} 天` : '点击签到领奖励' }}</text>
        </view>
        <text class="checkin-action text-primary text-sm">{{ checkinDone ? '已签到 ✅' : '签到 →' }}</text>
      </view>

      <!-- 排名横幅 -->
      <view class="rank-banner" @tap="goRanking">
        <view class="rank-info">
          <text class="rank-emoji">🏆</text>
          <view class="rank-text">
            <text class="rank-title">本周排行</text>
            <text class="rank-desc">{{ myRankText }}</text>
          </view>
        </view>
        <view class="rank-action">查看</view>
      </view>

      <!-- 学科网格 -->
      <view class="section-title-row">
        <view class="section-title-copy">
          <text class="text-lg text-bold">📚 今日学习</text>
          <text class="section-hint">选择一个喜欢的学科</text>
        </view>
        <text class="text-primary text-sm section-link" @tap="$emit('go-learn')">全部</text>
      </view>
      <view class="subject-grid stagger-list">
        <view
          v-for="subject in subjects"
          :key="subject.id"
          class="subject-card card card-hover"
          :class="{ locked: subject.locked }"
          @tap="!subject.locked && $emit('go-subject', subject)"
        >
          <text class="subject-emoji">{{ subject.icon }}</text>
          <text class="subject-name">{{ subject.name }}</text>
          <view class="subject-progress-wrap" v-if="subject.progress > 0">
            <view class="subject-progress-bar">
              <view class="subject-progress-fill" :style="{ width: subject.progress + '%' }"></view>
            </view>
            <text class="subject-progress-text text-xs">{{ subject.progress }}%</text>
          </view>
          <text v-else class="subject-progress-text text-xs text-light">未开始</text>
        </view>
      </view>

      <!-- 薄弱点推荐 -->
      <view v-if="weakPoints.length > 0" class="section-title-row">
        <view class="section-title-copy">
          <text class="text-lg text-bold">🎯 薄弱点练习</text>
          <text class="section-hint">针对错题推荐</text>
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
            <text class="weak-icon">{{ wp.subjectIcon }}</text>
            <view class="weak-info">
              <text class="text-md text-bold">{{ wp.subjectName }}</text>
              <text class="text-xs text-light">错题 {{ wp.wrongCount }} 道 · 正确率 {{ wp.accuracy }}%</text>
            </view>
          </view>
          <view class="weak-action">
            <text class="text-primary text-sm">去练习 →</text>
          </view>
        </view>
      </view>

      <!-- 快捷入口 -->
      <view class="quick-grid">
        <view class="quick-card card card-hover" @tap="goPet">
          <view class="quick-pet-info">
            <text class="pet-emoji animate-float">🐱</text>
            <view>
              <text class="pet-name">{{ petStore.name }}</text>
              <text class="pet-status-text text-base text-light">{{ petStore.moodText }} · Lv.{{ petStore.level }}</text>
            </view>
          </view>
          <text class="text-primary text-sm">去看看 →</text>
        </view>
        <view class="quick-card card achievement-quick" @tap="goAchievement">
          <text class="ach-emoji">🏅</text>
          <text class="text-white text-md text-bold">成就中心</text>
          <text class="text-white text-xs" style="opacity: 0.8;">{{ achievementCount }} 个成就</text>
        </view>
      </view>

      <!-- 排行速览 -->
      <view class="ranking-quick card">
        <view class="ranking-header">
          <text class="text-md text-bold">🏆 排行榜</text>
          <text class="text-primary text-sm" @tap="goRanking">更多 →</text>
        </view>
        <view class="ranking-list">
          <view v-for="(r, i) in topRankers" :key="r.id || i" class="ranking-item">
            <text class="rank-medal">{{ ['🥇','🥈','🥉'][i] }}</text>
            <text class="rank-name text-sm">{{ r.name }}</text>
            <text class="rank-score text-xs text-light">{{ r.score }} 分</text>
          </view>
          <view v-if="topRankers.length === 0" class="ranking-empty">
            <text class="text-xs text-light">暂无排行数据</text>
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
import CheckinPopup from '@/components/common/CheckinPopup.vue'

defineEmits(['go-subject', 'go-learn'])

const switchTab = inject('switchTab', () => {})
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
const checkinDone = ref(false)
const checkinStreak = ref(0)
const weakPoints = ref([])
const gradeLevelId = computed(() => userStore.userInfo?.gradeLevelId || null)

const taskProgress = computed(() =>
  totalTasks.value ? Math.round(completedTasks.value / totalTasks.value * 100) : 0
)

const myRankText = computed(() =>
  myRank.value ? `第 ${myRank.value} 名 · 继续加油！` : '暂无我的排名'
)

const subjects = ref([
  { id: 1, name: '语文', icon: '📖', progress: 0, locked: false },
  { id: 2, name: '数学', icon: '🔢', progress: 0, locked: false },
  { id: 3, name: '英语', icon: '🔤', progress: 0, locked: false },
  { id: 4, name: '逻辑', icon: '🧩', progress: 0, locked: false },
  { id: 5, name: '科学', icon: '🔬', progress: 0, locked: false },
  { id: 6, name: '音乐', icon: '🎵', progress: 0, locked: true }
])

const topRankers = ref([])

// Mock 数据 fallback
function applyMockData() {
  completedTasks.value = 2
  totalTasks.value = 5
  myRank.value = null
  achievementCount.value = 32
  subjects.value = [
    { id: 1, name: '语文', icon: '📖', progress: 75, locked: false },
    { id: 2, name: '数学', icon: '🔢', progress: 60, locked: false },
    { id: 3, name: '英语', icon: '🔤', progress: 45, locked: false },
    { id: 4, name: '逻辑', icon: '🧩', progress: 30, locked: false },
    { id: 5, name: '科学', icon: '🔬', progress: 20, locked: false },
    { id: 6, name: '音乐', icon: '🎵', progress: 0, locked: true }
  ]
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
      subjects.value = (Array.isArray(list) ? list : []).map(s => ({
        id: s.id,
        name: s.name || s.subjectName,
        icon: s.icon || s.iconUrl || { 'CHINESE': '📖', 'MATH': '🔢', 'ENGLISH': '🔤', 'LOGIC': '🧩', 'SCIENCE': '🔬', 'MUSIC': '🎵' }[s.code || s.subjectCode] || '📚',
        progress: s.progress || 0,
        locked: s.status === 0 || s.isVip || s.locked
      }))
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
      achievementCount.value = prog.completedCount || prog.length || 0
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

onMounted(() => {
  loadData()
  fetchCheckinStatus()
  // Show checkin popup on first load (will auto-close if already done)
  setTimeout(() => {
    checkinAutoOpen.value = true
    showCheckin.value = true
  }, 800)
})

// 返回时刷新数据
onShow(() => {
  loadData()
  fetchCheckinStatus()
})

watch(gradeLevelId, () => {
  learnStore.clearLearningContext()
  subjects.value = []
  loadData()
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

function onCheckinClose() {
  showCheckin.value = false
  fetchCheckinStatus()
}

function goRanking() { switchTab('ranking') }
function goPet() { switchTab('pet') }
function goAchievement() { switchTab('achievement') }
function goWrongTopics() { uni.navigateTo({ url: '/pages/mine/wrong' }) }
function goAdaptivePractice(wp) {
  uni.navigateTo({ url: `/pages/learn/adaptive?subjectId=${wp.subjectId}` })
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
  gap: 12px;
  padding: 14px 20px;
  border-radius: $radius-lg;
}
.checkin-emoji { font-size: 32px; }
.checkin-info { flex: 1; display: flex; flex-direction: column; }
.checkin-action { flex-shrink: 0; }

.rank-banner {
  background: linear-gradient(135deg, #FFECA6, #FFD95A); border-radius: $radius-lg;
  padding: 16px 20px; display: flex; align-items: center; justify-content: space-between; cursor: pointer;
  box-shadow: $shadow-sm;
}
.rank-info { display: flex; align-items: center; gap: 12px; }
.rank-emoji { font-size: 36px; }
.rank-text { display: flex; flex-direction: column; }
.rank-title { font-size: 16px; font-weight: 600; color: #5D4E60; }
.rank-desc { font-size: 13px; color: #7D6E80; }
.rank-action {
  min-width: 58px;
  height: 36px;
  border-radius: 18px;
  background: rgba(255,255,255,0.48);
  color: #5D4E60;
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
  min-width: 48px;
  min-height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  background: #FFF0E8;
  font-weight: 800;
}
.subject-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.subject-card {
  min-height: 142px;
  padding: 22px 16px; text-align: center; cursor: pointer;
  border: 1px solid rgba(84, 108, 138, 0.08);
  &:active { transform: scale(0.96); opacity: 0.9; }
  &.locked { opacity: 0.5; background: #F5F5F5; box-shadow: none; }
}
.subject-emoji { font-size: 52px; display: block; margin-bottom: 8px; }
.subject-name { font-size: 17px; font-weight: 800; color: $text; display: block; }
.subject-progress-text { font-size: 13px; color: $text-light; display: block; margin-top: 4px; }
.subject-progress-wrap { display: flex; align-items: center; gap: 6px; margin-top: 6px; }
.subject-progress-bar { flex: 1; height: 6px; background: rgba(0,0,0,0.1); border-radius: 3px; overflow: hidden; }
.subject-progress-fill { height: 100%; background: $primary; border-radius: 3px; transition: width 0.3s; }

.weak-grid { display: flex; flex-direction: column; gap: 10px; }
.weak-card { padding: 14px 18px; }
.weak-header { display: flex; align-items: center; gap: 12px; }
.weak-icon { font-size: 32px; }
.weak-info { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.weak-action { flex-shrink: 0; }

.quick-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.quick-card {
  display: flex; align-items: center; justify-content: space-between;
  min-height: 92px;
  padding: 20px 24px; cursor: pointer;
  &:active { transform: scale(0.96); opacity: 0.9; }
}
.quick-pet-info { display: flex; align-items: center; gap: 12px; }
.pet-emoji { font-size: 48px; }
.pet-name { font-size: 16px; font-weight: 600; color: $text; display: block; }
.pet-status-text { display: block; margin-top: 2px; }
.achievement-quick {
  background: linear-gradient(135deg, #7B68EE, #9B8BFF); flex-direction: column;
  align-items: flex-start; gap: 4px; box-shadow: 0 4px 20px rgba(123,104,238,0.3);
}
.ach-emoji { font-size: 32px; display: block; margin-bottom: 4px; }

.ranking-quick { padding: 16px 20px; }
.ranking-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.ranking-list { display: flex; flex-direction: column; gap: 8px; }
.ranking-item { display: flex; align-items: center; gap: 10px; }
.rank-medal { font-size: 20px; width: 28px; text-align: center; }
.rank-name { flex: 1; }
.ranking-empty { padding: 8px 0; text-align: center; }

/* 响应式 */
@media (max-width: 800px) {
  .subject-grid { grid-template-columns: repeat(3, 1fr); gap: 8px; }
  .subject-emoji { font-size: 36px; }
  .subject-card { padding: 16px 10px; }
  .quick-grid { grid-template-columns: 1fr; }
  .banner-emoji { font-size: 28px; }
  .rank-emoji { font-size: 28px; }
  .pet-emoji { font-size: 36px; }
  .ranking-quick { display: none; }
}

@media (max-width: 640px) {
  .home-content { gap: 14px; }
  .task-banner {
    align-items: flex-start;
    flex-direction: column;
    gap: 14px;
    padding: 18px;
  }
  .banner-progress { width: 100%; }
  .banner-title { font-size: 19px; }
  .rank-banner { padding: 14px 16px; }
  .rank-action { min-width: 52px; }
  .subject-grid { grid-template-columns: repeat(2, 1fr); }
  .subject-card { min-height: 128px; }
}
</style>
