<template>
  <view class="parent-content">
    <!-- Loading -->
    <view v-if="loading" class="loading-state">
      <tn-loading size="60" />
      <text class="text-sm text-light" style="margin-top: 12px;">加载中...</text>
    </view>

    <template v-else>
    <view class="parent-grid">
      <!-- 左列 -->
      <view class="parent-left">
        <!-- 孩子信息 -->
        <view class="child-card">
          <text class="child-avatar">👦</text>
          <view>
            <text class="text-lg text-bold text-white">{{ childInfo.name }}</text>
            <view class="status-badge">
              <text class="text-xs text-white" style="opacity: 0.9;">{{ childInfo.status }}</text>
            </view>
          </view>
        </view>

        <!-- 学习报告 -->
        <view class="report-card card">
          <view class="report-header">
            <text class="text-md text-bold">📊 今日学习报告</text>
          </view>
          <view class="stats-grid">
            <view class="stat-box">
              <text class="stat-value text-xl text-bold text-teal">{{ report.learnMinutes }}</text>
              <text class="text-xs text-light">学习时长(分)</text>
            </view>
            <view class="stat-box">
              <text class="stat-value text-xl text-bold text-teal">{{ report.completedLevels }}</text>
              <text class="text-xs text-light">完成关卡</text>
            </view>
            <view class="stat-box">
              <text class="stat-value text-xl text-bold text-teal">{{ report.accuracy }}%</text>
              <text class="text-xs text-light">正确率</text>
            </view>
          </view>
          <!-- 柱状图 -->
          <view class="bar-chart">
            <view v-for="(bar, i) in weeklyData" :key="i" class="bar-item">
              <view class="bar" :style="{ height: bar.value + '%', background: bar.today ? 'var(--teal)' : '#E0F0F0' }"></view>
              <text class="text-xs text-light">{{ bar.label }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 右列 -->
      <view class="parent-right">
        <!-- 时间控制 -->
        <view class="control-card card">
          <text class="text-md text-bold" style="margin-bottom: 12px;">⏰ 时间控制</text>
          <view class="control-row">
            <text class="text-sm">每日时长限制</text>
            <text class="text-sm text-bold">{{ timeControl.dailyLimit }}分钟</text>
          </view>
          <view class="control-row">
            <text class="text-sm">禁止使用时段</text>
            <text class="text-sm text-bold">{{ timeControl.forbiddenStart }}-{{ timeControl.forbiddenEnd }}</text>
          </view>
          <view class="control-row">
            <text class="text-sm">休息提醒</text>
            <text class="text-sm text-bold text-success">已开启</text>
          </view>
          <tn-button type="primary" size="sm" shape="round" @click="goTimeControl" style="margin-top: 8px; align-self: flex-start;">修改设置</tn-button>
        </view>

        <!-- 使用记录 -->
        <view class="control-card card">
          <text class="text-md text-bold" style="margin-bottom: 12px;">📱 今日使用记录</text>
          <view v-for="record in usageRecords" :key="record.id" class="record-row">
            <text class="record-icon">{{ record.icon }}</text>
            <view class="record-info">
              <text class="text-sm">{{ record.app }}</text>
              <text class="text-xs text-light">{{ record.time }}</text>
            </view>
            <text class="text-sm text-light">{{ record.duration }}</text>
          </view>
        </view>

        <!-- 操作按钮 -->
        <view class="action-row">
          <tn-button type="primary" shape="round" style="flex: 1;" @click="goReport">📊 学习报告</tn-button>
          <tn-button shape="round" style="flex: 1;" @click="goAlert">⚠️ 使用提醒</tn-button>
        </view>
      </view>
    </view>
    </template>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getReport, getTimeControl, getFamilyMembers } from '@/api/parent'

const loading = ref(true)

const childInfo = ref({ name: '小明', status: '学习中 · 数学' })
const report = ref({ learnMinutes: 0, completedLevels: 0, accuracy: 0 })

const weeklyData = ref([
  { label: '周一', value: 40, today: false },
  { label: '周二', value: 65, today: false },
  { label: '周三', value: 50, today: false },
  { label: '周四', value: 70, today: false },
  { label: '周五', value: 30, today: false },
  { label: '周六', value: 80, today: false },
  { label: '今天', value: 55, today: true }
])

const timeControl = ref({
  dailyLimit: 60,
  forbiddenStart: '21:00',
  forbiddenEnd: '07:00'
})

const usageRecords = ref([
  { id: 1, icon: '📚', app: '学习 - 数学', time: '14:00-14:20', duration: '20分钟' },
  { id: 2, icon: '🐱', app: '宠物互动', time: '14:20-14:25', duration: '5分钟' },
  { id: 3, icon: '📚', app: '学习 - 语文', time: '14:25-14:40', duration: '15分钟' }
])

function applyMockData() {
  report.value = { learnMinutes: 35, completedLevels: 3, accuracy: 85 }
  timeControl.value = { dailyLimit: 60, forbiddenStart: '21:00', forbiddenEnd: '07:00' }
  usageRecords.value = [
    { id: 1, icon: '📚', app: '学习 - 数学', time: '14:00-14:20', duration: '20分钟' },
    { id: 2, icon: '🐱', app: '宠物互动', time: '14:20-14:25', duration: '5分钟' },
    { id: 3, icon: '📚', app: '学习 - 语文', time: '14:25-14:40', duration: '15分钟' }
  ]
}

async function loadData() {
  loading.value = true
  try {
    const results = await Promise.allSettled([
      getReport({}),
      getTimeControl()
    ])

    // 学习报告
    if (results[0].status === 'fulfilled' && results[0].value) {
      const data = results[0].value
      const stats = data.stats || data
      report.value = {
        learnMinutes: stats.totalTime || 0,
        completedLevels: stats.totalQuestions || 0,
        accuracy: stats.accuracy || 0
      }
      if (data.dailyList && Array.isArray(data.dailyList)) {
        const days = ['周一', '周二', '周三', '周四', '周五', '周六', '今天']
        weeklyData.value = data.dailyList.slice(-7).map((d, i) => ({
          label: days[i] || d.date,
          value: Math.min((d.time || 0) * 100 / 60, 100),
          today: i === data.dailyList.slice(-7).length - 1
        }))
      }
    }

    // 时间控制
    if (results[1].status === 'fulfilled' && results[1].value) {
      const data = results[1].value
      timeControl.value = {
        dailyLimit: data.dailyLimitMinutes || 60,
        forbiddenStart: data.allowedEndTime || '21:00',
        forbiddenEnd: data.allowedStartTime || '07:00'
      }
    }
  } catch (e) {
    console.log('ParentContent: 使用模拟数据', e)
    applyMockData()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})

function goTimeControl() { uni.navigateTo({ url: '/pages/parent/time-control' }) }
function goReport() { uni.navigateTo({ url: '/pages/parent/report' }) }
function goAlert() { uni.navigateTo({ url: '/pages/parent/family' }) }
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.parent-content { height: 100%; }

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
}

.parent-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  height: 100%;
}

.parent-left, .parent-right {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 孩子信息 */
.child-card {
  background: linear-gradient(135deg, $teal, $teal-dark);
  border-radius: $radius-md;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.child-avatar { font-size: 48px; }
.status-badge {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 100px;
  padding: 2px 10px;
  display: inline-flex;
  margin-top: 4px;
}

/* 报告 */
.report-header { margin-bottom: 12px; }

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.stat-box {
  text-align: center;
  padding: 12px;
  background: #F0FAFA;
  border-radius: $radius;
}

.stat-value { display: block; }

.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: 80px;
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
  max-width: 24px;
  border-radius: 4px 4px 0 0;
  min-height: 4px;
}

/* 控制 */
.control-card {
  padding: 16px 20px;
}

.control-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  & + .control-row { border-top: 1px solid #F5F5F5; }
}

.record-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  & + .record-row { border-top: 1px solid #F5F5F5; }
}

.record-icon { font-size: 20px; }
.record-info { flex: 1; display: flex; flex-direction: column; }

.action-row {
  display: flex;
  gap: 12px;
}

/* 响应式 */
@media (max-width: 800px) {
  .parent-grid { grid-template-columns: 1fr; }
  .child-card { padding: 14px 18px; }
  .child-avatar { font-size: 36px; }
}

@media (max-width: 640px) {
  .stats-grid { grid-template-columns: repeat(3, 1fr); gap: 6px; }
  .stat-box { padding: 8px; }
  .control-card { padding: 12px 14px; }
}
</style>
