<template>
  <view class="practice-quiz-page">
    <view class="quiz-topbar">
      <view class="close-btn" @click="exitQuiz">✕</view>
      <view class="mode-info">
        <text class="mode-name">{{ modeName }}</text>
        <text v-if="isTimed" class="timer" :class="{ warning: countdown <= 10 }">{{ countdown }}s</text>
      </view>
      <view class="stats">
        <text>✅ {{ correctCount }}</text>
        <text>❌ {{ wrongCount }}</text>
      </view>
    </view>

    <view class="question-area" v-if="currentQuestion">
      <view class="progress">第 {{ currentIndex + 1 }} / {{ questions.length }} 题</view>
      <view class="question-text">{{ currentQuestion.questionText }}</view>
      <view class="options-grid">
        <view
          v-for="(opt, i) in currentQuestion.options"
          :key="i"
          class="option-btn"
          :class="getOptionClass(opt)"
          @click="selectOption(opt)"
        >
          <text class="option-label">{{ opt.label }}</text>
          <text class="option-text">{{ opt.optionContent }}</text>
        </view>
      </view>
    </view>

    <view v-if="showSummary" class="summary-overlay">
      <view class="summary-card">
        <text class="summary-title">练习结束</text>
        <view class="summary-stats">
          <text>正确: {{ correctCount }}</text>
          <text>错误: {{ wrongCount }}</text>
          <text>用时: {{ usedTime }}s</text>
        </view>
        <button class="summary-btn" @click="exitQuiz">返回</button>
      </view>
    </view>
  </view>
</template>

<script>
import { startPractice, submitPracticeAnswer } from '@/api/learn'

export default {
  data() {
    return {
      modeId: null,
      modeName: '专项练习',
      isTimed: false,
      timeLimit: 0,
      countdown: 0,
      timer: null,

      sessionId: null,
      questions: [],
      currentIndex: 0,
      selectedAnswer: null,

      correctCount: 0,
      wrongCount: 0,
      startTime: null,
      usedTime: 0,

      showSummary: false,
    }
  },
  computed: {
    currentQuestion() {
      return this.questions[this.currentIndex] || null
    }
  },
  onLoad(options) {
    this.modeId = options.modeId
    this.isTimed = options.type === 'TIMED'
    this.timeLimit = parseInt(options.timeLimit) || 0
    this.loadInitialQuestion()
  },
  onUnmounted() {
    clearInterval(this.timer)
  },
  methods: {
    async loadInitialQuestion() {
      try {
        const res = await startPractice(this.modeId)
        this.sessionId = res.practiceSessionId

        if (res.questions && res.questions.length > 0) {
          this.questions = res.questions.map(q => this.formatQuestion(q))
        } else {
           throw new Error("无题目")
        }

        this.modeName = res.modeName || this.modeName
        this.startTime = Date.now()
        if (this.isTimed) {
          this.countdown = this.timeLimit
          this.startTimer()
        }
      } catch (e) {
        uni.showToast({ title: '开始练习失败', icon: 'none' })
        setTimeout(() => uni.navigateBack(), 1500)
      }
    },
    startTimer() {
      this.timer = setInterval(() => {
        this.countdown--
        if (this.countdown <= 0) {
          this.finishPractice()
        }
      }, 1000)
    },
    formatQuestion(q) {
      if (!q) return null
      return {
        ...q,
        options: q.options.map(opt => ({
          ...opt,
          optionContent: opt.optionContent || opt.text,
          label: opt.label || opt.optionLabel
        }))
      }
    },
    getOptionClass(opt) {
      if (!this.selectedAnswer) return ''
      if (opt.isCorrect) return 'correct'
      if (opt.label === this.selectedAnswer.label && !opt.isCorrect) return 'wrong'
      return ''
    },
    async selectOption(opt) {
      if (this.selectedAnswer) return
      this.selectedAnswer = opt

      try {
        const res = await submitPracticeAnswer(this.sessionId, {
          questionId: this.currentQuestion.id,
          answer: opt.label
        })

        if (res.correct) {
          this.correctCount++
          opt.isCorrect = true
        } else {
          this.wrongCount++
          // Mark the correct answer
          const correctOpt = this.currentQuestion.options.find(o => o.label === res.correctAnswer)
          if (correctOpt) correctOpt.isCorrect = true
        }

        setTimeout(() => {
          this.selectedAnswer = null
          if (this.currentIndex < this.questions.length - 1) {
            this.currentIndex++
          } else {
            this.finishPractice()
          }
        }, 1500)

      } catch (e) {
        uni.showToast({ title: '提交失败', icon: 'none' })
        this.selectedAnswer = null
      }
    },
    finishPractice() {
      clearInterval(this.timer)
      this.usedTime = Math.round((Date.now() - this.startTime) / 1000)
      this.showSummary = true
    },
    exitQuiz() {
      uni.navigateBack()
    }
  }
}
</script>

<style lang="scss" scoped>
.practice-quiz-page {
  height: 100vh;
  background: #F0F7FF;
  display: flex;
  flex-direction: column;
  padding: 32rpx;
  box-sizing: border-box;
}

.quiz-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.close-btn {
  font-size: 40rpx;
  color: #999;
}

.mode-info {
  text-align: center;
}

.mode-name {
  font-size: 32rpx;
  font-weight: bold;
}

.timer {
  font-size: 28rpx;
  color: #E74C3C;
  font-weight: bold;
}

.stats {
  display: flex;
  gap: 24rpx;
  font-size: 32rpx;
}

.question-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.progress {
  font-size: 28rpx;
  color: #666;
  margin-bottom: 16rpx;
}

.question-text {
  font-size: 48rpx;
  font-weight: bold;
  text-align: center;
  margin-bottom: 60rpx;
}

.options-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32rpx;
  width: 100%;
  max-width: 800rpx;
}

.option-btn {
  background: #FFF;
  border-radius: 24rpx;
  padding: 40rpx;
  display: flex;
  align-items: center;
  gap: 24rpx;
  border: 2px solid transparent;

  &.correct {
    background: #E8FFF0;
    border-color: #2ECC71;
  }

  &.wrong {
    background: #FFF0F0;
    border-color: #E74C3C;
  }
}

.option-label {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background: #F0F2F5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

.option-text {
  font-size: 32rpx;
  font-weight: 500;
}

.summary-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}

.summary-card {
  background: #FFF;
  border-radius: 32rpx;
  padding: 60rpx;
  width: 400rpx;
  text-align: center;
}

.summary-title {
  font-size: 40rpx;
  font-weight: bold;
  display: block;
  margin-bottom: 40rpx;
}

.summary-stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: 40rpx;
  font-size: 32rpx;
}

.summary-btn {
  background: #4A90D9;
  color: #FFF;
  border-radius: 40rpx;
}
</style>
