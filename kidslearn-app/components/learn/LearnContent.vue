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
      <text class="text-sm text-light">当前年级：{{ gradeName }}</text>
    </view>

    <!-- 学科网格 -->
    <view class="subject-section">
      <view class="section-title-row">
        <text class="text-lg text-bold">📚 选择学科</text>
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
          :key="item.id"
          class="recent-card card card-hover"
          @tap="goCourse(item)"
        >
          <view class="recent-icon-wrap" :style="{ background: item.bg }">
            <text class="recent-emoji">{{ item.icon }}</text>
          </view>
          <view class="recent-info">
            <text class="text-md text-bold">{{ item.course }}</text>
            <text class="text-xs text-light">{{ item.subject }} · 第{{ item.level }}关</text>
          </view>
          <view class="recent-progress">
            <tn-line-progress :percent="item.progress" :active-color="item.color" :height="8" :show-percent="false" style="width: 120px;" />
            <text class="text-xs text-light">{{ item.progress }}%</text>
          </view>
          <tn-button type="primary" size="sm" shape="round">继续</tn-button>
        </view>
      </view>
    </view>
    </template>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useLearnStore } from '@/store/learn'
import { useUserStore } from '@/store/user'
import { getSubjects, getRecords } from '@/api/learn'

defineEmits(['go-courses'])

const learnStore = useLearnStore()
const userStore = useUserStore()

const loading = ref(true)
const gradeName = ref(userStore.userInfo?.gradeLevelName || '')

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
    { id: 1, course: '拼音入门', subject: '语文', level: 8, progress: 60, icon: '📖', color: '#FF6B6B', bg: '#FFF0F0' },
    { id: 2, course: '加减法', subject: '数学', level: 5, progress: 40, icon: '🔢', color: '#4A90D9', bg: '#E8F0FE' },
    { id: 3, course: '字母认知', subject: '英语', level: 3, progress: 25, icon: '🔤', color: '#4ECDC4', bg: '#E0F7F7' }
  ]
}

async function loadData() {
  loading.value = true
  try {
    const gradeLevelId = userStore.userInfo?.gradeLevelId || null
    const results = await Promise.allSettled([
      getSubjects(gradeLevelId),
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
            progress: 0,
            color: s.color || mapped.color,
            bg: mapped.bg,
            locked: false
          }
        })
      }
    }

    // 学习记录（最近学习）
    if (results[1].status === 'fulfilled' && results[1].value) {
      const records = results[1].value
      if (Array.isArray(records) && records.length > 0) {
        recentLearn.value = records.slice(0, 5).map(r => {
          const mapped = iconMap[r.subjectName] || { icon: '📚', color: '#9E9E9E', bg: '#F5F5F5' }
          return {
            id: r.id,
            course: r.courseName || r.levelName || '',
            subject: r.subjectName || '',
            level: r.levelName || '',
            progress: r.isPass ? 100 : Math.min(r.score || 0, 100),
            icon: mapped.icon,
            color: mapped.color,
            bg: mapped.bg
          }
        })
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

function goCourse(item) {
  learnStore.setSubject({ id: item.id, name: item.subject })
  uni.navigateTo({ url: `/pages/learn/courses?subjectId=${item.id}` })
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
  background: $white;
  border-radius: $radius-md;
  padding: 12px 20px;
  text-align: center;
}

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.subject-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.subject-card {
  padding: 20px 16px;
  text-align: center;
  cursor: pointer;
  transition: all $transition-fast;
  position: relative;

  &:active { transform: scale(0.97); }
  &.locked { opacity: 0.5; background: #F5F5F5; box-shadow: none; }
}

.subject-icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 10px;
}

.subject-emoji { font-size: 28px; }
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
  padding: 14px 20px;
  cursor: pointer;

  &:active { transform: scale(0.99); }
}

.recent-icon-wrap {
  width: 44px;
  height: 44px;
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

/* 响应式 */
@media (max-width: 800px) {
  .subject-grid { grid-template-columns: repeat(3, 1fr); gap: 8px; }
  .subject-emoji { font-size: 22px; }
  .subject-icon-wrap { width: 44px; height: 44px; }
  .subject-card { padding: 14px 10px; }
  .recent-card { flex-wrap: wrap; }
}
</style>
