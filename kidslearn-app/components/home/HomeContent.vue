<template>
  <view class="home-content">
    <FunLoadingState v-if="loading" title="正在准备星球任务" mascot="" />

    <template v-else>
      <view class="mission-grid">
        <view class="mission-panel" @tap="$emit('go-learn')">
          <image class="mission-art" src="/static/redesign/mission-island.png" mode="aspectFill" />
          <view class="mission-copy">
            <view class="mission-title-row">
              <text class="mission-eyebrow">今日小目标</text>
              <view class="mission-detail-trigger" @tap.stop="openTaskDrawer">
                <text class="mission-detail-icon">📋</text>
              </view>
            </view>
            <text class="mission-progress-copy">已完成 <text class="mission-progress-number">{{ completedTasks }}/{{ totalTasks }}</text> 个</text>
          </view>
          <view class="mission-action">
            <text>继续学习</text>
          </view>
          <view class="mission-steps" aria-label="今日任务进度">
            <view
              v-for="step in Math.max(totalTasks, 5)"
              :key="step"
              class="mission-step"
              :class="{ completed: step <= completedTasks, current: step === completedTasks + 1 }"
            />
          </view>
        </view>

        <view class="daily-stack">
          <view class="daily-panel checkin-panel" @tap="openCheckin">
            <image class="daily-art" src="/static/redesign/checkin-island.png" mode="aspectFill" />
            <view class="daily-copy">
              <text class="daily-title">每日签到</text>
              <text class="daily-value">{{ checkinDone ? '今天已完成' : `连续 ${checkinStreak || 0} 天` }}</text>
            </view>
          </view>
          <view class="daily-panel rank-panel" @tap="goRanking">
            <image class="daily-art" src="/static/redesign/rank-island.png" mode="aspectFill" />
            <view class="daily-copy">
              <text class="daily-title">本周排行</text>
              <text class="daily-value rank-value">{{ myRank ? `第 ${myRank} 名` : '等待上榜' }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="section-heading">
        <view>
          <text class="section-title">今日练习</text>
          <text class="section-subtitle">选择学科进行专项练习</text>
        </view>
        <view class="section-action" @tap="$emit('go-learn')"><text>查看全部模式</text></view>
      </view>

      <view class="subject-stage">
        <image class="subject-art" src="/static/redesign/subject-dioramas.png" mode="aspectFill" />
        <view class="subject-hit-grid">
          <view
            v-for="subject in subjects.slice(0, 6)"
            :key="subject.id"
            class="subject-hit"
            :class="{ locked: subject.locked }"
            @tap="!subject.locked && $emit('go-subject', subject)"
          >
            <view v-if="subject.locked" class="subject-status"><text>会员解锁</text></view>
            <text class="subject-label">{{ subject.name }}</text>
          </view>
        </view>
      </view>

      <view class="explore-grid">
        <view class="explore-card weak-explore" @tap="weakPoints[0] ? goAdaptivePractice(weakPoints[0]) : goWrongTopics()">
          <image class="explore-art" src="/static/redesign/weakpoint-console.png" mode="aspectFill" />
          <view class="explore-copy">
            <text class="explore-title">薄弱点突击</text>
            <text class="explore-name">{{ weakPoints[0]?.subjectName || '分数比较' }}</text>
            <text class="explore-detail">错题 {{ weakPoints[0]?.wrongCount || 6 }} 道 · 正确率 {{ weakPoints[0]?.accuracy || 58 }}%</text>
            <view class="explore-action purple"><text>立即突击</text></view>
          </view>
        </view>

        <view class="explore-card pet-explore" @tap="goPet">
          <image class="explore-art" src="/static/redesign/pet-habitat.png" mode="aspectFill" />
          <view class="explore-copy">
            <text class="explore-title teal-title">{{ petStore.name || '小芽兽' }}</text>
            <text class="explore-detail">{{ petStore.moodText || '心情很好' }} · Lv.{{ petStore.level }}</text>
            <view class="explore-action teal"><text>进入小窝</text></view>
          </view>
        </view>

        <view class="explore-card achievement-explore" @tap="goAchievement">
          <image class="explore-art" src="/static/redesign/achievement-cabinet.png" mode="aspectFill" />
          <view class="explore-copy">
            <text class="explore-title">已解锁 <text class="achievement-number">{{ achievementCount }}</text> 个</text>
            <text class="explore-detail">成就收藏</text>
            <view class="explore-action gold"><text>查看成就</text></view>
          </view>
        </view>

        <view class="explore-card ranking-explore" @tap="goRanking">
          <image class="explore-art" src="/static/redesign/ranking-podium.png" mode="aspectFill" />
          <view class="explore-copy ranking-copy">
            <text class="explore-title blue-title">排行榜风云</text>
            <view class="mini-ranking">
              <text v-for="(r, i) in topRankers.slice(0, 3)" :key="r.id || i" class="mini-ranker">{{ i + 1 }} {{ r.name }}</text>
              <text v-if="topRankers.length === 0" class="mini-ranker">完成练习后查看排名</text>
            </view>
            <view class="explore-action blue"><text>查看排行</text></view>
          </view>
        </view>
      </view>
    </template>

    <CheckinPopup v-if="showCheckin" :auto-close-if-done="checkinAutoOpen" @close="onCheckinClose" />
    <TaskDetailDrawer
      v-if="showTaskDrawer"
      :tasks="learnStore.dailyTasks"
      @close="onTaskDrawerClose"
      @go-learn="onTaskDrawerGoLearn"
    />
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
import TaskDetailDrawer from '@/components/home/TaskDetailDrawer.vue'
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
const showTaskDrawer = ref(false)
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
          // 学科锁定：仅由后端 status(0=下线) 或 locked 标记决定；后端当前不返回 isVip 字段
          locked: s.status === 0 || s.locked
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
    console.log('HomeContent: 数据加载失败', e)
    completedTasks.value = 0
    totalTasks.value = 0
    myRank.value = null
    achievementCount.value = 0
    topRankers.value = []
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

function openTaskDrawer() {
  showTaskDrawer.value = true
}

function onTaskDrawerClose() {
  showTaskDrawer.value = false
}

function onTaskDrawerGoLearn() {
  showTaskDrawer.value = false
  switchTab('learn')
}

function goRanking() { switchTab('ranking') }
function goPet() { switchTab('pet') }
function goAchievement() { switchTab('achievement') }
function goWrongTopics() { uni.navigateTo({ url: '/pages/mine/wrong' }) }
function goAdaptivePractice(wp) {
  uni.navigateTo({ url: `/pages/learn/practice/index?subjectId=${wp.subjectId}&subjectName=${encodeURIComponent(wp.subjectName || '')}` })
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
@include respond-md {
  .subject-grid { grid-template-columns: repeat(3, 1fr); gap: 10px; }
  .subject-emoji { font-size: 32px; }
  .subject-card { padding: 16px 12px; min-height: 130px; }
  .quick-grid { grid-template-columns: 1fr; }
  .banner-emoji { font-size: 42px; }
  .rank-emoji { font-size: 32px; }
  .pet-emoji { font-size: 36px; }
  .ranking-quick { display: none; }
}

@include respond-sm {
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

/* 2026 Planet Control Deck redesign */
.home-content {
  gap: 14px;
  color: #18212F;
}

.mission-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(236px, 1fr);
  gap: 12px;
  min-height: 232px;
}

.mission-panel,
.daily-panel,
.subject-stage,
.explore-card {
  position: relative;
  overflow: hidden;
  background: #FFFFFF;
  border: 1px solid rgba(63, 111, 229, 0.10);
  box-shadow: 0 10px 30px rgba(69, 91, 124, 0.09);
}

.mission-panel {
  min-height: 232px;
  border-radius: 26px;
  cursor: pointer;
}

.mission-panel:active,
.daily-panel:active,
.subject-hit:active,
.explore-card:active {
  transform: scale(0.985);
}

.mission-art {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.mission-copy {
  position: absolute;
  top: 22px;
  left: 24px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  z-index: 2;
}

.mission-eyebrow {
  font-size: 25px;
  line-height: 1.2;
  font-weight: 800;
  letter-spacing: -0.5px;
}

.mission-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mission-detail-trigger {
  width: 28px;
  height: 28px;
  min-width: 28px;
  min-height: 28px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 10px rgba(69, 91, 124, 0.10);
  cursor: pointer;
  &:active { transform: scale(0.92); }
}

.mission-detail-icon {
  font-size: 15px;
  line-height: 1;
}

.mission-progress-copy {
  font-size: 14px;
  font-weight: 650;
  color: #5D6A7A;
}

.mission-progress-number {
  color: #FF6B4A;
  font-size: 19px;
  font-weight: 850;
}

.mission-action {
  position: absolute;
  left: 24px;
  bottom: 44px;
  z-index: 2;
  min-width: 210px;
  min-height: 54px;
  padding: 0 28px;
  border-radius: 18px;
  background: #FF6B4A;
  color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 850;
  box-shadow: 0 12px 24px rgba(255, 107, 74, 0.28);
}

.mission-steps {
  position: absolute;
  left: 40px;
  right: 40px;
  bottom: 15px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 2;
}

.mission-steps::before {
  content: '';
  position: absolute;
  left: 8px;
  right: 8px;
  height: 3px;
  background: rgba(255, 255, 255, 0.72);
  border-radius: 3px;
}

.mission-step {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #FFFFFF;
  border: 3px solid rgba(93, 106, 122, 0.24);
  z-index: 1;
}

.mission-step.completed {
  background: #FF6B4A;
  border-color: #FFFFFF;
}

.mission-step.current {
  width: 22px;
  height: 22px;
  background: #FFFFFF;
  border-color: #FF6B4A;
}

.daily-stack {
  display: grid;
  grid-template-rows: 1fr 1fr;
  gap: 12px;
}

.daily-panel {
  min-height: 110px;
  border-radius: 22px;
  cursor: pointer;
}

.daily-art {
  position: absolute;
  right: 0;
  top: 0;
  width: 62%;
  height: 100%;
}

.daily-copy {
  position: absolute;
  left: 18px;
  top: 18px;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.daily-title {
  font-size: 16px;
  font-weight: 800;
  color: #18212F;
}

.daily-value {
  font-size: 15px;
  font-weight: 750;
  color: #168F85;
}

.rank-value { color: #3F6FE5; }

.section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 0 4px;
}

.section-heading > view:first-child {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.section-title {
  font-size: 21px;
  line-height: 1.2;
  font-weight: 850;
}

.section-subtitle {
  font-size: 12px;
  color: #7A8797;
}

.section-action {
  min-height: 44px;
  padding: 0 16px;
  border-radius: 15px;
  background: #EEF3FF;
  color: #3F6FE5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 750;
}

.subject-stage {
  min-height: 150px;
  border-radius: 24px;
}

.subject-art {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.subject-hit-grid {
  position: absolute;
  inset: 0;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.subject-hit {
  position: relative;
  min-height: 150px;
  cursor: pointer;
}

.subject-hit.locked::after {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(246, 248, 252, 0.54);
}

.subject-label {
  position: absolute;
  left: 10px;
  right: 10px;
  bottom: 8px;
  min-height: 36px;
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.92);
  color: #18212F;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 800;
  box-shadow: 0 5px 12px rgba(69, 91, 124, 0.10);
  z-index: 2;
}

.subject-status {
  position: absolute;
  top: 10px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 3;
  padding: 5px 10px;
  border-radius: 12px;
  background: #18212F;
  color: #FFFFFF;
  font-size: 10px;
  white-space: nowrap;
}

.explore-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.explore-card {
  min-height: 178px;
  border-radius: 22px;
  cursor: pointer;
}

.explore-art {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.explore-copy {
  position: absolute;
  inset: 14px;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.explore-title {
  font-size: 17px;
  line-height: 1.2;
  font-weight: 850;
  color: #5F3BB8;
}

.explore-title,
.explore-name,
.explore-detail,
.mini-ranking {
  max-width: 92%;
  padding: 2px 5px;
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.84);
  box-sizing: border-box;
}

.teal-title { color: #168F85; }
.blue-title { color: #245CCB; }

.explore-name {
  margin-top: 4px;
  font-size: 13px;
  font-weight: 750;
}

.explore-detail {
  margin-top: 2px;
  font-size: 11px;
  color: #5D6A7A;
}

.achievement-number {
  color: #E59A12;
  font-size: 22px;
}

.explore-action {
  margin-top: auto;
  width: 100%;
  min-height: 42px;
  border-radius: 14px;
  color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 800;
  box-shadow: 0 8px 16px rgba(69, 91, 124, 0.16);
}

.explore-action.purple { background: #7754D8; }
.explore-action.teal { background: #22AFA2; }
.explore-action.gold { background: #F0AE2B; }
.explore-action.blue { background: #3F7CE5; }

.ranking-copy { right: 10px; }

.mini-ranking {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 3px;
}

.mini-ranker {
  font-size: 10px;
  line-height: 1.25;
  color: #5D6A7A;
}

@include respond-md-lg {
  .mission-grid { grid-template-columns: minmax(0, 1.7fr) minmax(210px, 1fr); }
  .mission-action { min-width: 172px; }
  .explore-card { min-height: 184px; }
  .explore-copy { inset: 12px; }
}

/* iPad Pro / 大屏横板：内容随可用高度生长，避免固定卡片堆在顶部。 */
@media (min-width: 1200px) and (min-height: 900px) {
  .home-content {
    display: grid;
    grid-template-rows:
      minmax(250px, 1.18fr)
      auto
      minmax(180px, 0.82fr)
      minmax(210px, 1fr);
    gap: 16px;
    height: calc(100vh - 110px);
    min-height: 0;
  }

  .mission-grid,
  .mission-panel,
  .daily-panel,
  .subject-stage,
  .subject-hit,
  .explore-card {
    min-height: 0;
    height: 100%;
  }

  .mission-grid {
    grid-template-columns: minmax(0, 2.05fr) minmax(300px, 1fr);
    gap: 16px;
  }

  .daily-stack {
    gap: 16px;
  }

  .mission-copy {
    top: 28px;
    left: 30px;
  }

  .mission-eyebrow { font-size: 28px; }
  .mission-action {
    left: 30px;
    bottom: 54px;
    min-width: 220px;
    min-height: 58px;
  }

  .daily-copy {
    left: 22px;
    top: 22px;
  }

  .section-title { font-size: 23px; }
  .section-subtitle { font-size: 13px; }

  .subject-label {
    left: 12px;
    right: 12px;
    bottom: 10px;
    min-height: 40px;
    font-size: 16px;
  }

  .explore-grid { gap: 16px; }
  .explore-copy { inset: 16px; }
  .explore-title { font-size: 18px; }
  .explore-name { font-size: 14px; }
  .explore-detail { font-size: 12px; }
  .explore-action {
    min-height: 48px;
    font-size: 14px;
  }
}

@include respond-md {
  .mission-grid { grid-template-columns: 1fr; }
  .daily-stack { grid-template-columns: 1fr 1fr; grid-template-rows: none; }
  .subject-stage { overflow-x: auto; }
  .subject-art, .subject-hit-grid { min-width: 760px; }
  .explore-grid { grid-template-columns: repeat(2, 1fr); }
}

@include respond-sm {
  .mission-panel { min-height: 230px; }
  .daily-stack { grid-template-columns: 1fr; }
  .section-heading { align-items: center; }
  .section-subtitle { display: none; }
  .explore-grid { grid-template-columns: 1fr; }
}
</style>
