<template>
  <AppLayout theme="parent" title="学习报告" :show-back="true" active-nav="/pages/parent/index">
    <view class="report-content">
      <!-- 统计概览 -->
      <view class="stats-row">
        <view class="stat-card card">
          <text class="stat-icon">⏰</text>
          <text class="stat-value text-xl text-bold text-teal">{{ stats.totalMinutes }}</text>
          <text class="text-xs text-light">学习时长(分)</text>
        </view>
        <view class="stat-card card">
          <text class="stat-icon">📚</text>
          <text class="stat-value text-xl text-bold text-teal">{{ stats.completedLevels }}</text>
          <text class="text-xs text-light">完成关卡</text>
        </view>
        <view class="stat-card card">
          <text class="stat-icon">🎯</text>
          <text class="stat-value text-xl text-bold text-teal">{{ stats.accuracy }}%</text>
          <text class="text-xs text-light">正确率</text>
        </view>
        <view class="stat-card card">
          <text class="stat-icon">🔥</text>
          <text class="stat-value text-xl text-bold text-teal">{{ stats.streakDays }}</text>
          <text class="text-xs text-light">连续天数</text>
        </view>
      </view>

      <!-- 每周趋势 -->
      <view class="trend-card card">
        <view class="trend-header">
          <text class="text-md text-bold">📊 每周学习时长</text>
          <view class="period-tabs">
            <text class="period-tab" :class="{ active: period === 'week' }" @tap="period = 'week'">本周</text>
            <text class="period-tab" :class="{ active: period === 'month' }" @tap="period = 'month'">本月</text>
          </view>
        </view>
        <view class="bar-chart">
          <view v-for="(bar, i) in weeklyData" :key="i" class="bar-item">
            <view class="bar" :style="{ height: bar.value + '%', background: bar.today ? '#4ECDC4' : '#E0F0F0' }"></view>
            <text class="text-xs text-light">{{ bar.label }}</text>
          </view>
        </view>
      </view>

      <!-- 学科进度 -->
      <view class="subject-card card">
        <text class="text-md text-bold" style="margin-bottom: 12px;">📈 学科进度</text>
        <view v-for="s in subjectProgress" :key="s.id" class="subject-row">
          <text class="subject-icon">{{ s.icon }}</text>
          <text class="text-sm" style="width: 50px;">{{ s.name }}</text>
          <view class="progress-bar" style="flex: 1;">
            <view class="progress-fill" :style="{ width: s.progress + '%', background: s.color }"></view>
          </view>
          <text class="text-sm text-bold" style="width: 40px; text-align: right;">{{ s.progress }}%</text>
        </view>
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { getReport } from '@/api/parent'

const period = ref('week')

const stats = ref({
  totalMinutes: 0,
  completedLevels: 0,
  accuracy: 0,
  streakDays: 0
})

const weeklyData = ref([
  { label: '周一', value: 0, today: false },
  { label: '周二', value: 0, today: false },
  { label: '周三', value: 0, today: false },
  { label: '周四', value: 0, today: false },
  { label: '周五', value: 0, today: false },
  { label: '周六', value: 0, today: false },
  { label: '今天', value: 0, today: true }
])

const subjectProgress = ref([])

async function loadData() {
  try {
    const res = await getReport({})
    if (res) {
      const s = res.stats || {}
      stats.value = {
        totalMinutes: s.totalTime || 0,
        completedLevels: s.totalQuestions || 0,
        accuracy: s.accuracy || 0,
        streakDays: s.totalDays || 0
      }

      if (res.dailyList && Array.isArray(res.dailyList)) {
        const days = ['周一', '周二', '周三', '周四', '周五', '周六', '今天']
        const dailySlice = res.dailyList.slice(-7)
        weeklyData.value = dailySlice.map((d, i) => ({
          label: days[i] || d.date,
          value: Math.min((d.time || 0) * 100 / 60, 100),
          today: i === dailySlice.length - 1
        }))
      }

      if (res.subjectStats && Array.isArray(res.subjectStats)) {
        subjectProgress.value = res.subjectStats.map((s, i) => ({
          id: i + 1,
          name: s.name,
          icon: ['📖', '🔢', '🔤', '🧩', '🔬'][i] || '📚',
          progress: s.percent || 0,
          color: ['#FF6B6B', '#4A90D9', '#4ECDC4', '#9B59B6', '#2ECC71'][i] || '#9E9E9E'
        }))
      }
    }
  } catch (e) {
    console.log('report: 使用模拟数据', e)
    stats.value = { totalMinutes: 245, completedLevels: 28, accuracy: 85, streakDays: 12 }
    subjectProgress.value = [
      { id: 1, name: '语文', icon: '📖', progress: 75, color: '#FF6B6B' },
      { id: 2, name: '数学', icon: '🔢', progress: 64, color: '#4A90D9' },
      { id: 3, name: '英语', icon: '🔤', progress: 45, color: '#4ECDC4' },
      { id: 4, name: '逻辑', icon: '🧩', progress: 30, color: '#9B59B6' },
      { id: 5, name: '科学', icon: '🔬', progress: 20, color: '#2ECC71' }
    ]
  }
}

onMounted(() => loadData())
watch(period, () => loadData())
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.report-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 16px;
}

.stat-icon { font-size: 28px; }
.stat-value { display: block; }

.trend-card { padding: 16px 20px; }
.trend-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.period-tabs {
  display: flex;
  gap: 8px;
}

.period-tab {
  padding: 4px 14px;
  border-radius: 100px;
  font-size: 13px;
  cursor: pointer;
  background: #F5F5F5;

  &.active { background: #4ECDC4; color: #fff; }
}

.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  height: 100px;
  padding-top: 8px;
}

.bar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  height: 100%;
}

.bar {
  width: 100%;
  max-width: 28px;
  border-radius: 4px 4px 0 0;
  min-height: 4px;
}

.subject-card { padding: 16px 20px; }

.subject-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  & + .subject-row { border-top: 1px solid #F5F5F5; }
}

.subject-icon { font-size: 20px; }
</style>
