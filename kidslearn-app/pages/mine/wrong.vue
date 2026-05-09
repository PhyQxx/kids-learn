<template>
  <AppLayout theme="kids" title="智能错题本" :show-back="true" active-nav="/pages/mine/index">
    <view class="wrong-content">
      <!-- 学科筛选 -->
      <tn-tabs v-model="activeTab" active-color="#FF6B6B">
        <tn-tabs-item v-for="tab in tabItems" :key="tab.label" :title="tab.label" />
      </tn-tabs>

      <!-- 错题统计 & 智能复习 -->
      <view class="wrong-stats">
        <text class="text-sm text-light">共 {{ wrongList.length }} 道错题待掌握</text>
        <view class="action-btns">
          <tn-button type="warning" size="sm" shape="round" @click="startSmartReview" style="background: linear-gradient(135deg, #F39C12, #F1C40F); color: #FFF; margin-right: 8px;">✨ 智能复习</tn-button>
          <tn-button type="primary" size="sm" shape="round" @click="retryAll">全部重做</tn-button>
        </view>
      </view>

      <!-- 错题列表 -->
      <view class="wrong-list">
        <view v-for="item in wrongList" :key="item.id" class="wrong-card card">
          <view class="wrong-header">
            <view style="display: flex; gap: 8px; align-items: center;">
              <view class="subject-tag" :style="{ background: item.bg, color: item.color }">
                <text class="text-xs">{{ item.subject }}</text>
              </view>
              <view class="mastery-tag" :class="'mastery-' + item.masteryLevel">
                <text class="text-xs">{{ getMasteryText(item.masteryLevel) }}</text>
              </view>
            </view>
            <text class="text-xs text-light">已做对 {{ item.continuousCorrectCount || 0 }}/3 次</text>
          </view>

          <rich-text class="wrong-question text-sm" :nodes="item.questionNodes" />

          <view class="answer-row wrong-answer">
            <text class="text-xs">❌ 你的答案：{{ item.yourAnswer }}</text>
          </view>
          <view class="answer-row correct-answer">
            <text class="text-xs">✅ 正确答案：{{ item.correctAnswer }}</text>
          </view>

          <view class="card-actions">
            <tn-button type="primary" size="sm" shape="round" @click="retryOne(item)">重做消灭错题</tn-button>
            <tn-button size="sm" shape="round" @click="loadExplanation(item)" style="background: #F0F7FF; color: #4A90D9; border: 1px solid #D0E3F7; margin-left: 8px;">
              {{ item.explanationLoading ? '思考中...' : (item.aiExplanation ? '收起讲解' : '🤖 AI讲解') }}
            </tn-button>
          </view>

          <view v-if="item.analysisText && !item.aiExplanation" class="analysis-section">
            <text class="analysis-label text-xs text-light">📝 解析</text>
            <text class="analysis-text text-sm">{{ item.analysisText }}</text>
          </view>

          <view v-if="item.aiExplanation" class="ai-explanation animate-slide-up">
            <text class="text-sm">{{ item.aiExplanation }}</text>
          </view>
          <view v-if="item.explanationError" class="ai-explanation-error">
            <text class="text-xs text-light">{{ item.explanationError }}</text>
          </view>
        </view>
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { getWrongTopics, getExplainWrong, getSmartReviewQuiz } from '@/api/learn'
import { richContentToNodes, richContentToText } from '@/utils/richContent.mjs'

const activeTab = ref(0)
const tabItems = ref([
  { label: '全部' },
  { label: '语文' },
  { label: '数学' },
  { label: '英语' }
])

const wrongList = ref([])

onMounted(async () => {
  loadWrongTopics()
})

async function loadWrongTopics() {
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
        const questionContent = w.questionContent || w.questionText || ''
        return {
          id: w.id,
          questionId: w.questionId,
          subject: w.subjectName || '未知',
          question: w.questionText || richContentToText(questionContent),
          questionNodes: richContentToNodes(questionContent),
          analysisText: w.analysisText || '',
          yourAnswer: w.userAnswer || '',
          correctAnswer: w.correctAnswer || '',
          masteryLevel: w.masteryLevel || 0,
          continuousCorrectCount: w.continuousCorrectCount || 0,
          time: '',
          bg: sc.bg,
          color: sc.color,
          aiExplanation: '',
          explanationLoading: false,
          explanationError: ''
        }
      })
    }
  } catch (e) {
    console.log('wrong: 暂无错题')
  }
}

function getMasteryText(level) {
  if (level === 2) return '已掌握'
  if (level === 1) return '练习中'
  return '未掌握'
}

function startSmartReview() {
  uni.showToast({ title: '智能生成复习卷中...', icon: 'none' })
  setTimeout(() => {
    uni.navigateTo({ url: '/pages/learn/adaptive' })
  }, 1000)
}

function retryOne(item) {
  uni.navigateTo({ url: '/pages/learn/adaptive' })
}

function retryAll() {
  uni.navigateTo({ url: '/pages/learn/adaptive' })
}

async function loadExplanation(item) {
  if (item.aiExplanation) {
    item.aiExplanation = ''
    return
  }
  if (!item.questionId) {
    item.explanationError = '暂无法获取讲解'
    return
  }
  item.explanationLoading = true
  item.explanationError = ''
  try {
    const res = await getExplainWrong(item.questionId)
    if (res?.aiExplanation) {
      item.aiExplanation = res.aiExplanation
    } else if (res?.analysisText) {
      item.aiExplanation = res.analysisText
    } else {
      item.explanationError = 'AI讲解暂不可用'
    }
  } catch (e) {
    item.explanationError = '讲解加载失败，请稍后重试'
  } finally {
    item.explanationLoading = false
  }
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

.action-btns {
  display: flex;
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
  margin-bottom: 8px;
}

.subject-tag {
  padding: 2px 10px;
  border-radius: 100px;
}

.mastery-tag {
  padding: 2px 10px;
  border-radius: 100px;
  font-weight: bold;

  &.mastery-0 {
    background: #FFF0F0;
    color: #E74C3C;
  }
  &.mastery-1 {
    background: #FFF8E6;
    color: #F39C12;
  }
  &.mastery-2 {
    background: #E8F8F0;
    color: #2ECC71;
  }
}

.wrong-question {
  display: block;
  margin: 10px 0;
  line-height: 1.45;
  font-size: 16px;
  font-weight: bold;
}

.answer-row {
  padding: 6px 12px;
  border-radius: $radius;
  margin-top: 4px;
}

.wrong-answer { background: #FFF0F0; }
.correct-answer { background: #E8F8F0; }

.card-actions {
  display: flex;
  margin-top: 12px;
}

.analysis-section {
  margin-top: 8px;
  padding: 8px 12px;
  background: #F8F9FA;
  border-radius: $radius-sm;
}
.analysis-label { display: block; margin-bottom: 4px; }
.analysis-text { display: block; line-height: 1.45; color: $text-secondary; }

.ai-explanation {
  margin-top: 6px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #F0F7FF, #EEF6FF);
  border-radius: $radius;
  border-left: 3px solid $learn-blue;
  line-height: 1.5;
}
.ai-explanation-error {
  margin-top: 4px;
  padding: 4px 8px;
}
</style>
