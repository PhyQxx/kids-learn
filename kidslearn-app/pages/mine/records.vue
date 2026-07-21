<template>
  <AppLayout theme="kids" title="闯关记录" :show-back="true" active-nav="/pages/mine/index">
    <view class="records-content">
      <!-- 统计 -->
      <view class="summary-row">
        <view class="summary-item card">
          <text class="text-lg text-bold text-primary">{{ summary.total }}</text>
          <text class="text-xs text-light">总学习次数</text>
        </view>
        <view class="summary-item card">
          <text class="text-lg text-bold text-primary">{{ summary.correctRate }}%</text>
          <text class="text-xs text-light">平均正确率</text>
        </view>
        <view class="summary-item card">
          <text class="text-lg text-bold text-primary">{{ summary.streak }}</text>
          <text class="text-xs text-light">连续天数</text>
        </view>
      </view>

      <!-- 日期筛选 -->
      <tn-tabs v-model="activeTab" active-color="#FF6B6B">
        <tn-tabs-item v-for="tab in tabItems" :key="tab.label" :title="tab.label" />
      </tn-tabs>

      <!-- 记录列表 -->
      <view class="record-list">
        <view v-for="record in records" :key="record.id" class="record-card card">
          <view class="record-subject" :style="{ background: record.bg }">
            <text class="record-mark">{{ record.subject?.slice(0, 1) || '学' }}</text>
          </view>
          <view class="record-info">
            <text class="text-sm text-bold">{{ record.course }}</text>
            <text class="text-xs text-light">{{ record.subject }} · 第{{ record.level }}关</text>
          </view>
          <view class="record-score">
            <text class="text-sm text-bold" :class="record.score >= 80 ? 'text-success' : 'text-warning'">
              {{ record.score }}分
            </text>
            <text class="text-xs text-light">{{ record.time }}</text>
          </view>
          <view class="record-stars">
            <text>{{ record.stars }} 星</text>
          </view>
        </view>
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { getRecords } from '@/api/learn'

const activeTab = ref(0)
const tabItems = ref([
  { label: '今天' },
  { label: '本周' },
  { label: '本月' }
])

const subjectIconMap = {
  '语文': { icon: '📖', bg: '#FFF0F0' },
  '数学': { icon: '🔢', bg: '#E8F0FE' },
  '英语': { icon: '🔤', bg: '#E0F7F7' },
  '逻辑': { icon: '🧩', bg: '#F3E8FF' },
  '科学': { icon: '🔬', bg: '#E8F8F0' }
}

// 全部记录（包含原始时间戳，用于按Tab过滤）
const allRecords = ref([])

// 日期辅助
function startOfDay(d) {
  const x = new Date(d)
  x.setHours(0, 0, 0, 0)
  return x.getTime()
}
function startOfWeek(d) {
  const x = new Date(d)
  x.setHours(0, 0, 0, 0)
  const day = x.getDay() || 7 // 周日记为7
  x.setDate(x.getDate() - day + 1) // 周一作为一周起始
  return x.getTime()
}
function startOfMonth(d) {
  const x = new Date(d)
  return new Date(x.getFullYear(), x.getMonth(), 1).getTime()
}
function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const now = new Date()
  const month = d.getMonth() + 1
  const day = d.getDate()
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  if (startOfDay(d) === startOfDay(now)) return `今天 ${hh}:${mm}`
  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  if (startOfDay(d) === startOfDay(yesterday)) return `昨天 ${hh}:${mm}`
  return `${month}/${day} ${hh}:${mm}`
}

// 按Tab过滤
const records = computed(() => {
  const now = Date.now()
  const rangeStart = activeTab.value === 0
    ? startOfDay(now)
    : activeTab.value === 1
      ? startOfWeek(now)
      : startOfMonth(now)
  return allRecords.value
    .filter(r => (r.timestamp || 0) >= rangeStart)
    .sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0))
})

// 按当前Tab统计
const summary = computed(() => {
  const list = records.value
  const total = list.length
  const passed = list.filter(r => r.score >= 60).length
  const correctRate = total ? Math.round(passed / total * 100) : 0
  return { total, correctRate, streak: computeStreak() }
})

// 连续学习天数（基于全部记录）
function computeStreak() {
  if (!allRecords.value.length) return 0
  const days = new Set(allRecords.value.map(r => startOfDay(r.timestamp || 0)))
  let streak = 0
  const cursor = startOfDay(Date.now())
  while (days.has(cursor)) {
    streak++
    days.delete(cursor)
    cursor.setDate(cursor.getDate() - 1)
  }
  return streak
}

async function loadData() {
  try {
    const res = await getRecords()
    if (res && Array.isArray(res) && res.length > 0) {
      allRecords.value = res.map(r => {
        const si = subjectIconMap[r.subjectName] || { icon: '📚', bg: '#F5F5F5' }
        const ts = r.createTime ? new Date(r.createTime).getTime() : Date.now()
        return {
          id: r.id,
          course: r.courseName || r.levelName || '',
          subject: r.subjectName || '',
          level: r.levelName || '',
          icon: si.icon,
          bg: si.bg,
          score: r.score || 0,
          stars: r.stars || 0,
          timestamp: ts,
          time: formatTime(ts)
        }
      })
      return
    }
  } catch (e) {
    console.log('records: 使用模拟数据')
  }
  // 模拟数据
  const now = Date.now()
  const DAY = 24 * 60 * 60 * 1000
  allRecords.value = [
    { id: 1, course: '加减法进阶', subject: '数学', level: 8, icon: '🔢', bg: '#E8F0FE', score: 95, stars: 3, timestamp: now - 2 * 60 * 60 * 1000 },
    { id: 2, course: '拼音组合', subject: '语文', level: 5, icon: '📖', bg: '#FFF0F0', score: 80, stars: 2, timestamp: now - 3 * 60 * 60 * 1000 },
    { id: 3, course: '字母认知', subject: '英语', level: 3, icon: '🔤', bg: '#E0F7F7', score: 70, stars: 2, timestamp: now - 5 * 60 * 60 * 1000 },
    { id: 4, course: '图形认知', subject: '逻辑', level: 2, icon: '🧩', bg: '#F3E8FF', score: 90, stars: 3, timestamp: now - DAY },
    { id: 5, course: '古诗词', subject: '语文', level: 7, icon: '📖', bg: '#FFF0F0', score: 60, stars: 1, timestamp: now - 2 * DAY },
    { id: 6, course: '科学探秘', subject: '科学', level: 4, icon: '🔬', bg: '#E8F8F0', score: 85, stars: 3, timestamp: now - 4 * DAY },
    { id: 7, course: '乘法口诀', subject: '数学', level: 6, icon: '🔢', bg: '#E8F0FE', score: 88, stars: 3, timestamp: now - 10 * DAY },
    { id: 8, course: '单词拼写', subject: '英语', level: 5, icon: '🔤', bg: '#E0F7F7', score: 92, stars: 3, timestamp: now - 15 * DAY }
  ].map(r => ({ ...r, time: formatTime(r.timestamp) }))
}

onMounted(() => loadData())
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.records-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 16px;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.record-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
}

.record-subject {
  width: 44px;
  height: 44px;
  border-radius: $radius;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.record-emoji { font-size: 22px; }

.record-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.record-score {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.record-stars { display: flex; font-size: 12px; }
</style>
