<template>
  <view class="learn-content">
    <!-- 当前年级提示 -->
    <view v-if="gradeName" class="grade-banner">
      <text class="grade-emoji">🎒</text>
      <view class="grade-copy">
        <text class="grade-title">当前年级：{{ gradeName }}</text>
        <text class="grade-desc">题目会按你的年级自动调整</text>
      </view>
    </view>

    <!-- 模式网格 -->
    <view class="mode-grid">
      <view class="mode-card card card-hover" style="background: linear-gradient(135deg, #E8F0FE, #DBEAFE);" @click="goMode('level')">
        <view class="mode-icon-wrap" style="background: rgba(74, 144, 217, 0.1);">
          <text class="mode-emoji">⚔️</text>
        </view>
        <view class="mode-info">
          <text class="mode-name text-lg text-bold" style="color: #2C3E50;">闯关模式</text>
          <text class="mode-desc text-xs" style="color: #496280;">挑战关卡，赢取星星和宠物经验</text>
        </view>
        <text class="mode-arrow">→</text>
      </view>

      <view class="mode-card card card-hover" style="background: linear-gradient(135deg, #E8FFF8, #D1F2EB);" @click="goMode('practice')">
        <view class="mode-icon-wrap" style="background: rgba(46, 204, 113, 0.1);">
          <text class="mode-emoji">📝</text>
        </view>
        <view class="mode-info">
          <text class="mode-name text-lg text-bold" style="color: #1E8449;">专项练习</text>
          <text class="mode-desc text-xs" style="color: #27AE60;">针对学科巩固知识，无限题库</text>
        </view>
        <text class="mode-arrow">→</text>
      </view>

      <view class="mode-card card card-hover" style="background: linear-gradient(135deg, #FFF0F0, #FADBD8);" @click="goMode('review')">
        <view class="mode-icon-wrap" style="background: rgba(231, 76, 60, 0.1);">
          <text class="mode-emoji">💡</text>
        </view>
        <view class="mode-info">
          <text class="mode-name text-lg text-bold" style="color: #C0392B;">错题回顾</text>
          <text class="mode-desc text-xs" style="color: #E74C3C;">智能组卷复习，攻克薄弱点</text>
        </view>
        <text class="mode-arrow">→</text>
      </view>
    </view>

    <!-- 最近学习 -->
    <view v-if="recentLearn.length > 0" class="recent-section">
      <view class="section-title-row">
        <text class="text-lg text-bold">📖 继续学习</text>
      </view>
      <view class="recent-list">
        <view
          v-for="item in recentLearn"
          :key="item.courseId"
          class="recent-card card card-hover"
          @tap="goCourse(item)"
        >
          <view class="recent-icon-wrap" :style="{ background: item.bg }">
            <text class="recent-emoji">{{ item.icon }}</text>
          </view>
          <view class="recent-info">
            <text class="text-md text-bold">{{ item.courseName }}</text>
            <text class="text-xs text-light">{{ item.subjectName }} · {{ item.levelName }}</text>
          </view>
          <view class="recent-progress">
            <tn-line-progress :percent="item.progress" :active-color="item.color" :height="8" :show-percent="false" style="width: 120px;" />
            <text class="text-xs text-light">{{ item.progress }}%</text>
          </view>
          <view class="continue-pill">继续</view>
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
@media (max-width: 800px) {
  .recent-card { flex-wrap: wrap; }
}

@media (max-width: 640px) {
  .learn-content { gap: 16px; }
  .recent-progress { width: 100%; margin-left: 64px; }
}
</style>
