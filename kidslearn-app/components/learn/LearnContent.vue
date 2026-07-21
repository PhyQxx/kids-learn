<template>
  <view class="learn-content">
    <view class="learn-hero">
      <image class="learn-hero-art" src="/static/redesign/subject-dioramas.png" mode="aspectFill" />
      <view class="learn-hero-copy">
        <text class="learn-kicker">学习航线</text>
        <text class="learn-hero-title">今天想从哪里出发？</text>
        <text class="learn-hero-desc">{{ gradeName ? `${gradeName}内容已为你准备好` : '选择一种方式，开始今天的探索' }}</text>
      </view>
      <view v-if="gradeName" class="grade-chip">
        <text>{{ gradeName }}</text>
        <text>难度自动适配</text>
      </view>
    </view>

    <view class="mode-grid">
      <view class="mode-card level-mode" @click="goMode('level')">
        <image class="mode-art" src="/static/redesign/mission-island.png" mode="aspectFill" />
        <view class="mode-copy">
          <text class="mode-index">航线 01</text>
          <text class="mode-name">闯关模式</text>
          <text class="mode-desc">按课程推进关卡，赢取星星与宠物经验</text>
          <view class="mode-action"><text>选择学科</text></view>
        </view>
      </view>

      <view class="mode-card practice-mode" @click="goMode('practice')">
        <image class="mode-art" src="/static/redesign/subject-dioramas.png" mode="aspectFill" />
        <view class="mode-copy">
          <text class="mode-index">航线 02</text>
          <text class="mode-name">专项练习</text>
          <text class="mode-desc">针对单科反复训练，自由选择练习方式</text>
          <view class="mode-action teal"><text>开始训练</text></view>
        </view>
      </view>

      <view class="mode-card review-mode" @click="goMode('review')">
        <image class="mode-art" src="/static/redesign/weakpoint-console.png" mode="aspectFill" />
        <view class="mode-copy">
          <text class="mode-index">航线 03</text>
          <text class="mode-name">错题回顾</text>
          <text class="mode-desc">根据历史错题智能组卷，集中攻克薄弱点</text>
          <view class="mode-action purple"><text>智能复习</text></view>
        </view>
      </view>
    </view>

    <view v-if="recentLearn.length > 0" class="recent-section">
      <view class="section-title-row">
        <text class="text-lg text-bold">继续上次进度</text>
        <text class="recent-hint">按最近学习时间排列</text>
      </view>
      <view class="recent-list">
        <view
          v-for="item in recentLearn"
          :key="item.courseId"
          class="recent-card card card-hover"
          @tap="goCourse(item)"
        >
          <view class="recent-route-mark"><text>{{ item.subjectName?.slice(0, 1) || '学' }}</text></view>
          <view class="recent-info">
            <text class="text-md text-bold">{{ item.courseName }}</text>
            <text class="text-xs text-light">{{ item.subjectName }} · {{ item.levelName }}</text>
          </view>
          <view class="recent-progress">
            <tn-line-progress :percent="item.progress" :active-color="item.color" :height="8" :show-percent="false" style="width: 120px;" />
            <text class="text-xs text-light">{{ item.progress }}%</text>
          </view>
          <view class="continue-pill">继续学习</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useLearnStore } from '@/store/learn'
import { useUserStore } from '@/store/user'
import { getRecords } from '@/api/learn'
import { getParentReport, getTimeControl } from '@/api/parent'
import { ensureLearningAccess } from '@/utils/learningAccess.mjs'

const learnStore = useLearnStore()
const userStore = useUserStore()

const gradeLevelId = computed(() => userStore.userInfo?.gradeLevelId || null)
const gradeName = computed(() => userStore.userInfo?.gradeLevelName || '')

const iconMap = {
  'CHINESE': { icon: '📖', color: '#FF6B6B', bg: '#FFF0F0' },
  'MATH': { icon: '🔢', color: '#4A90D9', bg: '#E8F0FE' },
  'ENGLISH': { icon: '🔤', color: '#4ECDC4', bg: '#E0F7F7' },
  'LOGIC': { icon: '🧩', color: '#9B59B6', bg: '#F3E8FF' },
  'SCIENCE': { icon: '🔬', color: '#2ECC71', bg: '#E8F8F0' },
  'CODING': { icon: '💻', color: '#FFD700', bg: '#FFF8E0' }
}

const recentLearn = ref([])

function showLearningBlocked(message) {
  uni.showModal({
    title: '休息一下',
    content: message,
    showCancel: false
  })
}

async function canStartLearning() {
  return ensureLearningAccess({
    fetchTimeControl: getTimeControl,
    fetchReport: getParentReport,
    showBlockedMessage: showLearningBlocked
  })
}

async function goMode(mode) {
  if (!(await canStartLearning())) return

  if (mode === 'level') {
    uni.navigateTo({ url: '/pages/learn/subjects' })
  } else if (mode === 'practice') {
    uni.navigateTo({ url: '/pages/learn/subjects?practice=true' })
  } else if (mode === 'review') {
    uni.navigateTo({ url: '/pages/mine/wrong' })
  }
}

async function loadData() {
  try {
    const recordsRes = await getRecords().catch(() => null)
    // 学习记录（最近学习）
    if (recordsRes) {
      const records = (Array.isArray(recordsRes) ? recordsRes : [])
        .filter(r => {
          if (!gradeLevelId.value) return true
          if (!Array.isArray(r.gradeLevelIds) || r.gradeLevelIds.length === 0) return false
          return r.gradeLevelIds.some(id => Number(id) === Number(gradeLevelId.value))
        })
      if (records.length > 0) {
        recentLearn.value = records.slice(0, 5).map(r => {
          const mapped = iconMap[r.subjectName] || { icon: '📚', color: '#9E9E9E', bg: '#F5F5F5' }
          return {
            courseId: r.courseLevelId,
            courseName: r.courseName || '',
            subjectName: r.subjectName || '',
            levelName: r.levelName || '',
            progress: r.isPass ? 100 : Math.min(r.score || 0, 100),
            icon: mapped.icon,
            color: mapped.color,
            bg: mapped.bg
          }
        })
      } else {
        recentLearn.value = []
      }
    }
  } catch (e) {
    console.log('LearnContent load error', e)
  }
}

onMounted(() => {
  loadData()
})

onShow(() => {
  loadData()
})

watch(gradeLevelId, () => {
  learnStore.clearLearningContext()
  recentLearn.value = []
  loadData()
})

async function goCourse(item) {
  if (!(await canStartLearning())) return

  learnStore.setLevel({ id: item.courseId, name: item.levelName })
  const grade = gradeLevelId.value || ''
  uni.navigateTo({ url: `/pages/learn/quiz?levelId=${item.courseId}&gradeLevelId=${grade}` })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.learn-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.grade-banner {
  background: linear-gradient(135deg, #FFFFFF, #F0F7FF);
  border: 1px solid rgba(74, 144, 217, 0.12);
  border-radius: $radius-lg;
  padding: 14px 18px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: $shadow-sm;
}
.grade-emoji { font-size: 34px; }
.grade-copy { display: flex; flex-direction: column; gap: 2px; }
.grade-title { font-size: 15px; color: $text; font-weight: 800; }
.grade-desc { font-size: 12px; color: $text-light; }

.mode-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mode-card {
  padding: 24px;
  border-radius: $radius-xl;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  border: none;
  box-shadow: 0 8px 24px rgba(0,0,0,0.06);
}

.mode-icon-wrap {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mode-emoji {
  font-size: 32px;
}

.mode-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mode-arrow {
  font-size: 24px;
  color: rgba(0,0,0,0.2);
  font-weight: bold;
}

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.recent-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.recent-card {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 76px;
  padding: 16px 20px;
  cursor: pointer;

  &:active { transform: scale(0.99); }
}

.recent-icon-wrap {
  width: 50px;
  height: 50px;
  border-radius: $radius;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.recent-emoji { font-size: 22px; }

.recent-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.recent-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: 8px;
}
.continue-pill {
  min-width: 56px;
  height: 38px;
  border-radius: 19px;
  background: linear-gradient(135deg, $learn-blue, $learn-blue-light);
  color: $white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 800;
  box-shadow: 0 8px 16px rgba(74, 144, 217, 0.22);
}

/* 响应式 */
@include respond-md {
  .recent-card { flex-wrap: wrap; }
}

@include respond-sm {
  .learn-content { gap: 16px; }
  .recent-progress { width: 100%; margin-left: 64px; }
}

/* Planet learning routes */
.learn-content { gap: 16px; }

.learn-hero {
  position: relative;
  min-height: 190px;
  overflow: hidden;
  border-radius: 28px;
  background: #EEF5FF;
  border: 1px solid rgba(63, 111, 229, 0.10);
  box-shadow: 0 12px 32px rgba(69, 91, 124, 0.10);
}

.learn-hero::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(255,255,255,.98) 0%, rgba(255,255,255,.82) 35%, rgba(255,255,255,.08) 70%);
}

.learn-hero-art { position: absolute; inset: 0; width: 100%; height: 100%; }
.learn-hero-copy {
  position: absolute;
  z-index: 2;
  left: 28px;
  top: 28px;
  display: flex;
  flex-direction: column;
  gap: 7px;
}
.learn-kicker { color: #3F6FE5; font-size: 13px; font-weight: 850; letter-spacing: 1px; }
.learn-hero-title { color: #18212F; font-size: 28px; font-weight: 850; }
.learn-hero-desc { color: #5D6A7A; font-size: 14px; }
.grade-chip {
  position: absolute;
  z-index: 2;
  left: 28px;
  bottom: 22px;
  min-height: 42px;
  padding: 0 16px;
  border-radius: 14px;
  background: rgba(255,255,255,.90);
  display: flex;
  align-items: center;
  gap: 10px;
  color: #315EBA;
  font-size: 12px;
  font-weight: 750;
}
.grade-chip text:first-child { font-size: 14px; color: #18212F; }

.mode-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.mode-card {
  position: relative;
  min-height: 260px;
  padding: 0;
  overflow: hidden;
  border-radius: 24px;
  border: 1px solid rgba(63,111,229,.10);
  background: #FFFFFF;
  box-shadow: 0 10px 28px rgba(69,91,124,.10);
}
.mode-card:active { transform: scale(.985); }
.mode-art { position: absolute; inset: 0; width: 100%; height: 100%; }
.mode-card::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255,255,255,.96) 0%, rgba(255,255,255,.70) 48%, rgba(255,255,255,.04) 76%);
}
.mode-copy {
  position: absolute;
  z-index: 2;
  inset: 18px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}
.mode-index { font-size: 11px; color: #7A8797; font-weight: 800; letter-spacing: .8px; }
.mode-name { margin-top: 4px; font-size: 21px; color: #18212F; font-weight: 850; }
.mode-desc { margin-top: 5px; max-width: 88%; font-size: 12px; line-height: 1.55; color: #5D6A7A; }
.mode-action {
  margin-top: auto;
  min-width: 126px;
  min-height: 42px;
  padding: 0 16px;
  border-radius: 14px;
  background: #3F7CE5;
  color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 850;
  box-shadow: 0 8px 18px rgba(63,124,229,.24);
}
.mode-action.teal { background: #22AFA2; }
.mode-action.purple { background: #7754D8; }
.recent-hint { color: #7A8797; font-size: 12px; }
.recent-route-mark {
  width: 46px;
  height: 46px;
  flex-shrink: 0;
  border-radius: 15px;
  background: #EAF1FF;
  color: #315EBA;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  font-weight: 850;
}
.continue-pill { min-width: 84px; background: #3F7CE5; }

@media (min-width: 1200px) and (min-height: 900px) {
  .learn-content { height: calc(100vh - 110px); }
  .learn-hero { min-height: 220px; }
  .mode-grid { flex: 1; min-height: 0; }
  .mode-card { min-height: 300px; height: 100%; }
}

@include respond-md {
  .mode-grid { grid-template-columns: 1fr; }
  .mode-card { min-height: 220px; }
}
</style>
