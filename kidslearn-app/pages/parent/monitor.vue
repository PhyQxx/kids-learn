<template>
  <AppLayout theme="parent" title="实时监控" :show-back="true" active-nav="/pages/parent/index">
    <template #topbar-right>
      <view class="refresh-btn" @tap="refresh">
        <text>刷新</text>
      </view>
    </template>

    <view class="monitor-page">
      <view class="summary-grid">
        <view class="summary-item">
          <text class="summary-value">{{ summary.onlineCount }}/{{ summary.childCount }}</text>
          <text class="summary-label">在线</text>
        </view>
        <view class="summary-item">
          <text class="summary-value">{{ summary.learningCount }}</text>
          <text class="summary-label">学习中</text>
        </view>
        <view class="summary-item">
          <text class="summary-value">{{ formatMinutes(summary.todayMinutes) }}</text>
          <text class="summary-label">今日学习</text>
        </view>
        <view class="summary-item warning" :class="{ active: summary.alertCount > 0 }">
          <text class="summary-value">{{ summary.alertCount }}</text>
          <text class="summary-label">提醒</text>
        </view>
      </view>

      <view v-if="loading" class="state-panel">
        <text>正在同步...</text>
      </view>
      <view v-else-if="children.length === 0" class="state-panel">
        <text>暂无家庭成员</text>
        <view class="outline-btn" @tap="goFamily">
          <text>家庭管理</text>
        </view>
      </view>

      <view v-else class="child-list">
        <view v-for="child in children" :key="child.childId" class="child-card">
          <view class="child-header">
            <view class="avatar">
              <text>{{ child.avatar || '👧' }}</text>
            </view>
            <view class="child-title">
              <text class="child-name">{{ child.nickname }}</text>
              <text class="activity-time">{{ child.lastActivityAt ? formatTime(child.lastActivityAt) : '暂无活动' }}</text>
            </view>
            <view class="status-chip" :class="statusClass(child.status)">
              <text>{{ child.statusText }}</text>
            </view>
          </view>

          <view class="progress-block">
            <view class="progress-head">
              <text>今日学习 {{ formatMinutes(child.todayMinutes) }}</text>
              <text>剩余 {{ formatMinutes(child.remainingMinutes) }}</text>
            </view>
            <view class="progress-track">
              <view class="progress-fill" :style="{ width: progressWidth(child) }"></view>
            </view>
          </view>

          <view class="metric-row">
            <view class="metric-cell">
              <text class="metric-value">{{ child.completedLevels }}</text>
              <text class="metric-label">完成关卡</text>
            </view>
            <view class="metric-cell">
              <text class="metric-value">{{ child.accuracy }}%</text>
              <text class="metric-label">正确率</text>
            </view>
            <view class="metric-cell">
              <text class="metric-value">{{ child.latestScore }}</text>
              <text class="metric-label">最近得分</text>
            </view>
          </view>

          <view class="course-line">
            <text class="course-name">{{ child.currentCourseName || '未开始课程' }}</text>
            <text class="level-name">{{ child.currentLevelName || '等待学习' }}</text>
          </view>
        </view>
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { getRealtimeMonitor } from '@/api/parent'
import { useRealtimeStore } from '@/store/realtime'

const realtimeStore = useRealtimeStore()
const loading = ref(false)

const monitor = computed(() => realtimeStore.parentMonitor)
const children = computed(() => monitor.value.children || [])
const summary = computed(() => monitor.value.summary || {
  childCount: 0,
  onlineCount: 0,
  learningCount: 0,
  todayMinutes: 0,
  completedLevels: 0,
  alertCount: 0
})

onMounted(() => {
  realtimeStore.connect()
  refresh()
})

async function refresh() {
  loading.value = true
  try {
    const data = await getRealtimeMonitor()
    realtimeStore.setParentMonitor(data)
  } catch (e) {
    uni.showToast({ title: '监控同步失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function goFamily() {
  uni.navigateTo({ url: '/pages/parent/family' })
}

function progressWidth(child) {
  if (!child.dailyLimitMinutes) return '0%'
  return `${Math.min(100, Math.round(child.todayMinutes * 100 / child.dailyLimitMinutes))}%`
}

function statusClass(status) {
  return {
    learning: status === 'LEARNING',
    resting: status === 'RESTING',
    limited: status === 'LIMITED',
    offline: status === 'OFFLINE'
  }
}

function formatMinutes(minutes) {
  const value = Math.max(0, Number(minutes) || 0)
  if (value < 60) return `${value}分钟`
  const hours = Math.floor(value / 60)
  const rest = value % 60
  return rest ? `${hours}小时${rest}分` : `${hours}小时`
}

function formatTime(value) {
  const text = String(value)
  const time = text.includes('T') ? text.split('T')[1] : text
  return time.slice(0, 5)
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.monitor-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.refresh-btn,
.outline-btn {
  min-height: 36px;
  padding: 0 16px;
  border-radius: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: $teal;
  background: $white;
  font-size: 14px;
  font-weight: 700;
}

.outline-btn {
  border: 1px solid rgba(53, 201, 184, 0.35);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  min-height: 96px;
  padding: 16px;
  border-radius: $radius-md;
  background: $white;
  box-shadow: $shadow-sm;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}

.summary-item.warning.active {
  background: #FFF4E2;
  .summary-value { color: $warning; }
}

.summary-value {
  font-size: 28px;
  font-weight: 800;
  color: $teal-dark;
  line-height: 1;
}

.summary-label {
  font-size: 13px;
  color: $text-light;
}

.state-panel {
  min-height: 180px;
  border-radius: $radius-md;
  background: $white;
  box-shadow: $shadow-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: $text-light;
  font-size: 15px;
}

.child-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.child-card {
  padding: 18px;
  border-radius: $radius-md;
  background: $white;
  box-shadow: $shadow-sm;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.child-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #E8F8F8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.child-title {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.child-name {
  font-size: 17px;
  font-weight: 800;
  color: $text;
}

.activity-time {
  font-size: 12px;
  color: $text-light;
}

.status-chip {
  min-width: 64px;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #F1F6FC;
  color: $text-secondary;
  font-size: 12px;
  font-weight: 700;
}

.status-chip.learning {
  background: #E8F8F8;
  color: $teal-dark;
}

.status-chip.resting {
  background: #FFF4E2;
  color: $warning;
}

.status-chip.limited {
  background: #FFECEC;
  color: $error;
}

.status-chip.offline {
  background: #F2F4F7;
  color: $text-light;
}

.progress-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.progress-head {
  display: flex;
  justify-content: space-between;
  color: $text-secondary;
  font-size: 13px;
}

.progress-track {
  height: 8px;
  border-radius: 4px;
  overflow: hidden;
  background: #EDF3F8;
}

.progress-fill {
  height: 100%;
  border-radius: 4px;
  background: linear-gradient(90deg, $teal, $learn-blue);
  transition: width $transition-normal;
}

.metric-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.metric-cell {
  min-height: 62px;
  border-radius: $radius-sm;
  background: #F8FBFF;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.metric-value {
  font-size: 18px;
  font-weight: 800;
  color: $text;
}

.metric-label {
  font-size: 12px;
  color: $text-light;
}

.course-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 2px;
}

.course-name,
.level-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-name {
  color: $text;
  font-size: 14px;
  font-weight: 700;
}

.level-name {
  color: $text-light;
  font-size: 13px;
}

@media (max-width: 800px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .child-list {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
