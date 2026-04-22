<template>
  <AppLayout theme="kids" title="错题本" :show-back="true" active-nav="/pages/mine/index">
    <view class="wrong-content">
      <!-- 学科筛选 -->
      <tn-tabs v-model="activeTab" active-color="#FF6B6B">
        <tn-tabs-item v-for="tab in tabItems" :key="tab.label" :title="tab.label" />
      </tn-tabs>

      <!-- 错题统计 -->
      <view class="wrong-stats">
        <text class="text-sm text-light">共 {{ wrongList.length }} 道错题</text>
        <tn-button type="primary" size="sm" shape="round" @click="retryAll">全部重做</tn-button>
      </view>

      <!-- 错题列表 -->
      <view class="wrong-list">
        <view v-for="item in wrongList" :key="item.id" class="wrong-card card">
          <view class="wrong-header">
            <view class="subject-tag" :style="{ background: item.bg, color: item.color }">
              <text class="text-xs">{{ item.subject }}</text>
            </view>
            <text class="text-xs text-light">{{ item.time }}</text>
          </view>
          <text class="text-sm" style="margin: 10px 0;">{{ item.question }}</text>
          <view class="answer-row wrong-answer">
            <text class="text-xs">❌ 你的答案：{{ item.yourAnswer }}</text>
          </view>
          <view class="answer-row correct-answer">
            <text class="text-xs">✅ 正确答案：{{ item.correctAnswer }}</text>
          </view>
          <tn-button type="primary" size="sm" shape="round" @click="retryOne(item)" style="align-self: flex-start; margin-top: 8px;">重做</tn-button>
        </view>
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { getWrongTopics } from '@/api/learn'

const activeTab = ref(0)
const tabItems = ref([
  { label: '全部' },
  { label: '语文' },
  { label: '数学' },
  { label: '英语' }
])

const wrongList = ref([
  { id: 1, subject: '数学', question: '25 + 37 = ?', yourAnswer: '52', correctAnswer: '62', time: '今天', bg: '#E8F0FE', color: '#4A90D9' },
  { id: 2, subject: '语文', question: '"春眠不觉晓"的下一句是？', yourAnswer: '处处闻啼鸟', correctAnswer: '处处闻啼鸟', time: '昨天', bg: '#FFF0F0', color: '#FF6B6B' },
  { id: 3, subject: '英语', question: 'Apple 的意思是？', yourAnswer: '香蕉', correctAnswer: '苹果', time: '昨天', bg: '#E0F7F7', color: '#4ECDC4' },
  { id: 4, subject: '数学', question: '8 × 7 = ?', yourAnswer: '54', correctAnswer: '56', time: '3天前', bg: '#E8F0FE', color: '#4A90D9' },
  { id: 5, subject: '语文', question: '"飞流直下三千尺"出自哪首诗？', yourAnswer: '《静夜思》', correctAnswer: '《望庐山瀑布》', time: '3天前', bg: '#FFF0F0', color: '#FF6B6B' }
])

onMounted(async () => {
  try {
    const res = await getWrongTopics()
    if (res && Array.isArray(res) && res.length > 0) {
      const subjectColors = {
        '语文': { bg: '#FFF0F0', color: '#FF6B6B' },
        '数学': { bg: '#E8F0FE', color: '#4A90D9' },
        '英语': { bg: '#E0F7F7', color: '#4ECDC4' },
        '逻辑': { bg: '#F3E8FF', color: '#9B59B6' },
        '科学': { bg: '#E8F8F0', color: '#2ECC71' }
      }
      wrongList.value = res.map(w => {
        const sc = subjectColors[w.subjectName] || { bg: '#F5F5F5', color: '#9E9E9E' }
        return {
          id: w.id,
          subject: w.subjectName || '未知',
          question: w.questionContent || '',
          yourAnswer: w.userAnswer || '',
          correctAnswer: w.correctAnswer || '',
          time: '',
          bg: sc.bg,
          color: sc.color
        }
      })
    }
  } catch (e) {
    console.log('wrong: 使用模拟数据')
  }
})

function retryOne(item) {
  uni.showToast({ title: `重做：${item.question}`, icon: 'none' })
}

function retryAll() {
  uni.showToast({ title: '开始重做全部错题', icon: 'none' })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.wrong-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.wrong-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.wrong-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.wrong-card {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
}

.wrong-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.subject-tag {
  padding: 2px 10px;
  border-radius: 100px;
}

.answer-row {
  padding: 6px 12px;
  border-radius: $radius;
  margin-top: 4px;
}

.wrong-answer { background: #FFF0F0; }
.correct-answer { background: #E8F8F0; }
</style>
