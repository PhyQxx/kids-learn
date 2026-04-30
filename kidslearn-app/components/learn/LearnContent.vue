<template>
  <view class="learn-content">
    <!-- Loading -->
    <view v-if="loading" class="loading-state">
      <tn-loading size="60" />
      <text class="text-sm text-light" style="margin-top: 12px;">加载中...</text>
    </view>

    <template v-else>
    <!-- 当前年级提示 -->
    <view v-if="gradeName" class="grade-banner">
      <text class="grade-emoji">🎒</text>
      <view class="grade-copy">
        <text class="grade-title">当前年级：{{ gradeName }}</text>
        <text class="grade-desc">题目会按你的年级自动调整</text>
      </view>
    </view>

    <!-- 学科网格 -->
    <view class="subject-section">
      <view class="section-title-row">
        <view class="section-title-copy">
          <text class="text-lg text-bold">📚 选择学科</text>
          <text class="section-hint">先选熟悉的，再挑战新的</text>
        </view>
      </view>
      <view class="subject-grid stagger-list">
        <view
          v-for="subject in subjects"
          :key="subject.id"
          class="subject-card card card-hover"
          :class="{ locked: subject.locked }"
          @tap="!subject.locked && $emit('go-courses', subject)"
        >
          <view class="subject-icon-wrap" :style="{ background: subject.bg }">
            <text class="subject-emoji">{{ subject.icon }}</text>
          </view>
          <text class="subject-name text-md text-bold">{{ subject.name }}</text>
          <view class="subject-progress-row">
            <tn-line-progress :percent="subject.progress" :active-color="subject.color" :height="8" :show-percent="false" style="flex: 1;" />
            <text class="text-xs text-light">{{ subject.progress }}%</text>
          </view>
          <view v-if="subject.locked" class="lock-badge">
            <text class="text-xs text-light">🔒 VIP</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 最近学习 -->
    <view class="recent-section">
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
    </template>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useLearnStore } from '@/store/learn'
import { useUserStore } from '@/store/user'
import { getSubjects, getRecords } from '@/api/learn'

defineEmits(['go-courses'])

const learnStore = useLearnStore()
const userStore = useUserStore()

const loading = ref(true)
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

const subjects = ref([])
const recentLearn = ref([])

function applyMockData() {
  subjects.value = [
    { id: 1, name: '语文', icon: '📖', progress: 50, color: '#FF6B6B', bg: '#FFF0F0', locked: false },
    { id: 2, name: '数学', icon: '🔢', progress: 64, color: '#4A90D9', bg: '#E8F0FE', locked: false },
    { id: 3, name: '英语', icon: '🔤', progress: 20, color: '#4ECDC4', bg: '#E0F7F7', locked: false },
    { id: 4, name: '逻辑', icon: '🧩', progress: 45, color: '#9B59B6', bg: '#F3E8FF', locked: false },
    { id: 5, name: '科学', icon: '🔬', progress: 17, color: '#2ECC71', bg: '#E8F8F0', locked: false },
    { id: 6, name: '编程', icon: '💻', progress: 0, color: '#FFD700', bg: '#FFF8E0', locked: true }
  ]
  recentLearn.value = [
    { courseId: 1, courseName: '拼音入门', subjectName: '语文', levelName: '第8关', progress: 60, icon: '📖', color: '#FF6B6B', bg: '#FFF0F0' },
    { courseId: 2, courseName: '加减法', subjectName: '数学', levelName: '第5关', progress: 40, icon: '🔢', color: '#4A90D9', bg: '#E8F0FE' },
    { courseId: 3, courseName: '字母认知', subjectName: '英语', levelName: '第3关', progress: 25, icon: '🔤', color: '#4ECDC4', bg: '#E0F7F7' }
  ]
}

async function loadData() {
  loading.value = true
  try {
    const results = await Promise.allSettled([
      getSubjects(gradeLevelId.value),
      getRecords().catch(() => null)
    ])

    // 学科列表
    if (results[0].status === 'fulfilled' && results[0].value) {
      const list = results[0].value
      if (Array.isArray(list) && list.length > 0) {
        subjects.value = list.map(s => {
          const mapped = iconMap[s.code] || { icon: s.icon || '📚', color: s.color || '#9E9E9E', bg: '#F5F5F5' }
          return {
            id: s.id,
            name: s.name,
            icon: s.icon || mapped.icon,
            progress: s.progress,
            color: s.color || mapped.color,
            bg: mapped.bg,
            locked: false
          }
        })
      }
    }

    // 学习记录（最近学习）
    if (results[1].status === 'fulfilled' && results[1].value) {
      const records = (Array.isArray(results[1].value) ? results[1].value : [])
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
    console.log('LearnContent: 使用模拟数据', e)
    applyMockData()
  } finally {
    loading.value = false
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
  subjects.value = []
  recentLearn.value = []
  loadData()
})

function goCourse(item) {
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

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
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

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.section-title-copy { display: flex; flex-direction: column; gap: 2px; }
.section-hint { font-size: 13px; color: $text-light; }

.subject-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.subject-card {
  min-height: 154px;
  padding: 22px 16px;
  text-align: center;
  cursor: pointer;
  transition: all $transition-fast;
  position: relative;

  &:active { transform: scale(0.96); }
  &.locked { opacity: 0.5; background: #F5F5F5; box-shadow: none; }
}

.subject-icon-wrap {
  width: 64px;
  height: 64px;
  border-radius: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 10px;
}

.subject-emoji { font-size: 32px; }
.subject-name { display: block; margin-bottom: 8px; }

.subject-progress-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.lock-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  background: #FFF8E0;
  padding: 2px 8px;
  border-radius: 100px;
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
  .subject-grid { grid-template-columns: repeat(3, 1fr); gap: 8px; }
  .subject-emoji { font-size: 22px; }
  .subject-icon-wrap { width: 44px; height: 44px; }
  .subject-card { padding: 14px 10px; }
  .recent-card { flex-wrap: wrap; }
}

@media (max-width: 640px) {
  .learn-content { gap: 16px; }
  .subject-grid { grid-template-columns: repeat(2, 1fr); }
  .subject-card { min-height: 138px; }
  .subject-icon-wrap { width: 56px; height: 56px; border-radius: 18px; }
  .recent-progress { width: 100%; margin-left: 64px; }
}
</style>
