<template>
  <AppLayout theme="kids" title="学习记录" :show-back="true" active-nav="/pages/mine/index">
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
            <text class="record-emoji">{{ record.icon }}</text>
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
            <text v-for="s in record.stars" :key="s">⭐</text>
          </view>
        </view>
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { getRecords } from '@/api/learn'

const activeTab = ref(0)
const tabItems = ref([
  { label: '今天' },
  { label: '本周' },
  { label: '本月' }
])

const summary = ref({ total: 0, correctRate: 0, streak: 0 })

const records = ref([
  { id: 1, course: '加减法进阶', subject: '数学', level: 8, icon: '🔢', bg: '#E8F0FE', score: 95, stars: 3, time: '今天 14:30' },
  { id: 2, course: '拼音组合', subject: '语文', level: 5, icon: '📖', bg: '#FFF0F0', score: 80, stars: 2, time: '今天 13:45' },
  { id: 3, course: '字母认知', subject: '英语', level: 3, icon: '🔤', bg: '#E0F7F7', score: 70, stars: 2, time: '今天 11:20' },
  { id: 4, course: '图形认知', subject: '逻辑', level: 2, icon: '🧩', bg: '#F3E8FF', score: 90, stars: 3, time: '昨天 15:00' },
  { id: 5, course: '古诗词', subject: '语文', level: 7, icon: '📖', bg: '#FFF0F0', score: 60, stars: 1, time: '昨天 14:20' }
])

const subjectIconMap = {
  '语文': { icon: '📖', bg: '#FFF0F0' },
  '数学': { icon: '🔢', bg: '#E8F0FE' },
  '英语': { icon: '🔤', bg: '#E0F7F7' },
  '逻辑': { icon: '🧩', bg: '#F3E8FF' },
  '科学': { icon: '🔬', bg: '#E8F8F0' }
}

async function loadData() {
  try {
    const res = await getRecords()
    if (res && Array.isArray(res) && res.length > 0) {
      summary.value = {
        total: res.length,
        correctRate: res.length ? Math.round(res.filter(r => r.isPass).length / res.length * 100) : 0,
        streak: 0
      }
      records.value = res.map(r => {
        const si = subjectIconMap[r.subjectName] || { icon: '📚', bg: '#F5F5F5' }
        return {
          id: r.id,
          course: r.courseName || r.levelName || '',
          subject: r.subjectName || '',
          level: r.levelName || '',
          icon: si.icon,
          bg: si.bg,
          score: r.score || 0,
          stars: r.stars || 0,
          time: r.createTime || ''
        }
      })
    }
  } catch (e) {
    console.log('records: 使用模拟数据')
    summary.value = { total: 156, correctRate: 82, streak: 12 }
  }
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
