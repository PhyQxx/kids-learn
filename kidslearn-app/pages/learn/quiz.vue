<template>
  <AppLayout theme="kids" :show-topbar="false">
    <view class="quiz-container">
      <!-- 开始屏 -->
      <view v-if="screen === 'start'" class="screen start-screen">
        <text class="start-emoji animate-pulse">{{ levelEmoji }}</text>
        <text class="start-title text-title text-bold">{{ levelName }}</text>
        <text class="start-subtitle text-light">准备好了吗？</text>
        <view class="info-row">
          <view class="info-item card">
            <text class="info-emoji">📝</text>
            <text class="info-text text-sm">{{ totalQuestions }} 题</text>
          </view>
          <view class="info-item card">
            <text class="info-emoji">⭐</text>
            <text class="info-text text-sm">目标 3 星</text>
          </view>
          <view class="info-item card">
            <text class="info-emoji">⏱️</text>
            <text class="info-text text-sm">{{ timeLimit }} 秒</text>
          </view>
        </view>
        <tn-button type="primary" size="xl" shape="round" @click="startQuiz" style="background: linear-gradient(135deg, #4A90D9, #6BA3E0);">开始答题</tn-button>
      </view>

      <!-- 答题屏 -->
      <view v-if="screen === 'quiz'" class="screen quiz-screen">
        <!-- 顶部栏 -->
        <view class="quiz-topbar">
          <view class="close-btn" @tap="exitQuiz">
            <text>✕</text>
          </view>
          <view class="quiz-progress">
            <tn-line-progress :percent="(currentIndex + 1) / totalQuestions * 100" active-color="#4A90D9" :height="10" :show-percent="false" />
          </view>
          <view class="timer" :class="{ warning: countdown <= 10 }">
            <text>{{ countdown }}s</text>
          </view>
        </view>

        <!-- 题目区域 -->
        <view class="question-area">
          <text class="question-emoji" @click="questionToSpeech(currentQuestion.text)">{{ currentQuestion.emoji }}</text>
          <text class="question-text text-title text-bold" @click="questionToSpeech(currentQuestion.text)">{{ currentQuestion.text }}</text>

          <!-- 选项网格 -->
          <view class="options-grid">
            <view
              v-for="(opt, i) in currentQuestion.options"
              :key="i"
              class="option-btn"
              :class="getOptionClass(opt)"
              @tap="selectOption(opt)"
            >
              <text class="option-label">{{ opt.label }}</text>
              <text class="option-text">{{ opt.text }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 结果屏 -->
      <view v-if="screen === 'result'" class="screen result-screen">
        <text class="result-emoji animate-pop-in">{{ resultEmoji }}</text>

        <!-- 星级 -->
        <view class="result-stars">
          <text
            v-for="s in 3"
            :key="s"
            class="result-star"
            :class="{ filled: s <= earnedStars, animate: s <= earnedStars }"
          >⭐</text>
        </view>

        <text class="result-title text-title text-bold">{{ resultTitle }}</text>
        <text class="result-subtitle text-light">{{ resultSubtitle }}</text>

        <!-- 奖励卡片 -->
        <view class="reward-row">
          <view class="reward-card card">
            <text class="reward-emoji">🪙</text>
            <text class="reward-value text-md text-bold">+{{ rewards.gold }}</text>
            <text class="reward-label text-xs text-light">金币</text>
          </view>
          <view class="reward-card card">
            <text class="reward-emoji">⚡</text>
            <text class="reward-value text-md text-bold">+{{ rewards.exp }}</text>
            <text class="reward-label text-xs text-light">经验</text>
          </view>
          <view class="reward-card card">
            <text class="reward-emoji">🎨</text>
            <text class="reward-value text-md text-bold">x{{ rewards.stickers }}</text>
            <text class="reward-label text-xs text-light">贴纸</text>
          </view>
        </view>

        <!-- 统计 -->
        <view class="result-stats card">
          <view class="stat-row">
            <text class="text-sm text-light">正确率</text>
            <text class="text-sm text-bold">{{ accuracy }}%</text>
          </view>
          <view class="stat-row">
            <text class="text-sm text-light">用时</text>
            <text class="text-sm text-bold">{{ usedTime }}秒</text>
          </view>
          <view class="stat-row">
            <text class="text-sm text-light">得分</text>
            <text class="text-sm text-bold text-primary">{{ totalScore }}分</text>
          </view>
        </view>

        <view class="result-actions">
          <tn-button type="primary" size="lg" shape="round" @click="goNextLevel" style="background: linear-gradient(135deg, #4A90D9, #6BA3E0);">下一关</tn-button>
          <tn-button size="lg" shape="round" @click="goBack">返回关卡</tn-button>
        </view>
      </view>

      <!-- 答对反馈 -->
      <view v-if="showCorrect" class="feedback-overlay correct-overlay">
        <text class="feedback-text text-title text-bold text-white animate-pop-in">✅ 正确！</text>
      </view>

      <!-- 答错反馈 -->
      <view v-if="showWrong" class="feedback-overlay wrong-overlay">
        <text class="feedback-text text-title text-bold text-white animate-shake">❌ 再想想</text>
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useLearnStore } from '@/store/learn'
import { getQuestions, submitAnswer, completeLevel } from '@/api/learn'

const learnStore = useLearnStore()

const screen = ref('start')
const currentIndex = ref(0)
const selectedAnswer = ref('')
const showCorrect = ref(false)
const showWrong = ref(false)
const countdown = ref(60)
const totalTime = ref(60)
let timer = null

const levelId = ref(null)
const levelName = ref(learnStore.currentLevel?.name || '第 1 关')
const levelEmoji = ref('🎮')
const timeLimit = ref(60)

// 题目列表，由后端加载
const questions = ref([])
// 科大讯飞语音播报插件（仅在 App 端初始化）
let xfyunVoicePlugin = null
// #ifdef APP-PLUS
xfyunVoicePlugin = uni.requireNativePlugin('Tellsea-XfyunVoicePlugin');
console.log('科大讯飞语音播报插件：' + JSON.stringify(xfyunVoicePlugin));
// #endif

// 初始化插件
const init = () => {
  // #ifdef APP-PLUS
  if (!xfyunVoicePlugin) return
  // APPID：你在讯飞语音控制台创建应用的APPID，下面是测试APPID，需要更换成你自己的
  xfyunVoicePlugin.init('ae085245', (e) => {
    let res = JSON.parse(e);
    console.log(res);
    if (res.code == 200) {
      uni.showToast({ title: res.msg, icon: 'none' });
    } else {
      uni.showToast({ title: res.msg, icon: 'none' });
    }
  });
  // #endif
}

// 语音播报
const questionToSpeech = (text) => {
  // #ifdef APP-PLUS
  if (!xfyunVoicePlugin) return
  xfyunVoicePlugin.textToSpeech(text + '测试播放', (e) => {
    let res = JSON.parse(e);
    console.log(res);
    if (res.code == 200) {
      uni.showToast({ title: res.msg, icon: 'none' });
    } else {
      uni.showToast({ title: res.msg, icon: 'none' });
    }
  });
  // #endif
}

onMounted(async () => {
  init();
  const pages = getCurrentPages()
  const page = pages[pages.length - 1]
  levelId.value = page.$page?.options?.levelId || learnStore.currentLevel?.id
  if (levelId.value) {
    try {
      const res = await getQuestions(levelId.value)
      if (res && Array.isArray(res) && res.length > 0) {
        questions.value = res.map(q => ({
          id: q.id,
          emoji: '❓',
          text: q.questionContent,
          options: (q.options || []).map((opt, i) => ({
            label: opt.optionLabel || String.fromCharCode(65 + i),
            text: opt.optionContent,
            correct: false // 后端不返回正确答案，由 submitAnswer 判定
          }))
        }))
      }
    } catch (e) {
      console.log('quiz: 使用模拟题目')
    }
  }
})

const totalQuestions = computed(() => questions.value.length)
const currentQuestion = computed(() => {
  questionToSpeech(questions.value[currentIndex.value].text)
  return questions.value[currentIndex.value] || {}
})
const correctCount = ref(0)
const totalScore = ref(0)
const earnedStars = ref(0)
const startTime = ref(0)
const questionStartTime = ref(0)
const usedTime = ref(0)

const rewards = ref({ gold: 15, exp: 10, stickers: 1 })

const accuracy = computed(() =>
  totalQuestions.value ? Math.round(correctCount.value / totalQuestions.value * 100) : 0
)

const resultEmoji = computed(() => earnedStars.value >= 3 ? '🎉' : earnedStars.value >= 1 ? '👍' : '💪')
const resultTitle = computed(() => earnedStars.value >= 3 ? '太棒了！' : earnedStars.value >= 1 ? '不错哦！' : '继续加油！')
const resultSubtitle = computed(() => earnedStars.value >= 3 ? '你获得了满星评价！' : '再努力一下就能获得更多星星！')

function getOptionClass(opt) {
  if (!selectedAnswer.value) return ''
  if (opt.correct) return 'correct'
  if (opt.label === selectedAnswer.value && !opt.correct) return 'wrong'
  return ''
}

function selectOption(opt) {
  if (selectedAnswer.value) return
  selectedAnswer.value = opt.label
  const q = currentQuestion.value
  const answerTime = Math.round((Date.now() - (questionStartTime.value || startTime.value)) / 1000)

  // 提交答案到后端判定
  if (levelId.value && q.id) {
    submitAnswer({
      levelId: levelId.value,
      questionId: q.id,
      answer: opt.label,
      answerTime
    }).then(res => {
      const isCorrect = res?.correct || false
      if (isCorrect) {
        correctCount.value++
        totalScore.value += (q.score || 10)
        showCorrect.value = true
      } else {
        showWrong.value = true
      }
      setTimeout(() => {
        showCorrect.value = false
        showWrong.value = false
        nextQuestion()
      }, isCorrect ? 800 : 1000)
    }).catch(() => {
      showWrong.value = true
      setTimeout(() => { showWrong.value = false; nextQuestion() }, 1000)
    })
  } else {
    // 无 levelId 时随机判定（fallback mock 题目）
    showWrong.value = true
    setTimeout(() => { showWrong.value = false; nextQuestion() }, 1000)
  }
}

function nextQuestion() {
  selectedAnswer.value = ''
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value++
  } else {
    finishQuiz()
  }
}

function startQuiz() {
  screen.value = 'quiz'
  startTime.value = Date.now()
  questionStartTime.value = Date.now()
  countdown.value = totalTime.value
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      finishQuiz()
    }
  }, 1000)
}

async function finishQuiz() {
  clearInterval(timer)
  usedTime.value = Math.round((Date.now() - startTime.value) / 1000)

  // Calculate stars
  const acc = correctCount.value / questions.value.length
  if (acc >= 0.9) earnedStars.value = 3
  else if (acc >= 0.7) earnedStars.value = 2
  else if (acc >= 0.5) earnedStars.value = 1
  else earnedStars.value = 0

  const wrongCount = questions.value.length - correctCount.value

  // 提交关卡完成
  if (levelId.value) {
    try {
      const res = await completeLevel(levelId.value, totalScore.value, usedTime.value, wrongCount)
      if (res) {
        earnedStars.value = res.stars || earnedStars.value
        rewards.value = {
          gold: res.gold || 10 + earnedStars.value * 10,
          exp: res.exp || 10 + earnedStars.value * 5,
          stickers: res.stickerId ? 1 : (earnedStars.value >= 2 ? 1 : 0)
        }
      }
    } catch (e) {
      rewards.value = {
        gold: 10 + earnedStars.value * 10,
        exp: 10 + earnedStars.value * 5,
        stickers: earnedStars.value >= 2 ? 1 : 0
      }
    }
  } else {
    rewards.value = {
      gold: 10 + earnedStars.value * 10,
      exp: 10 + earnedStars.value * 5,
      stickers: earnedStars.value >= 2 ? 1 : 0
    }
  }

  screen.value = 'result'
}

function exitQuiz() {
  clearInterval(timer)
  uni.navigateBack()
}

function goNextLevel() {
  uni.navigateBack()
}

function goBack() {
  uni.navigateBack()
}

onUnmounted(() => {
  clearInterval(timer)
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.quiz-container {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
}

.screen {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

/* ===== 开始屏 ===== */
.start-screen {
  gap: 16px;
}

.start-emoji { font-size: 96px; display: block; }
.start-title { text-align: center; }
.start-subtitle { text-align: center; }

.info-row {
  display: flex;
  gap: 16px;
  margin: 8px 0 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: $radius;
}

.info-emoji { font-size: 20px; }

/* ===== 答题屏 ===== */
.quiz-screen {
  justify-content: flex-start;
  padding-top: 16px;
}

.quiz-topbar {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.close-btn {
  width: 36px;
  height: 36px;
  border-radius: $radius;
  background: #F5F5F5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  cursor: pointer;
  flex-shrink: 0;
}

.quiz-progress { flex: 1; }

.timer {
  background: #F0F7FF;
  color: $learn-blue;
  padding: 6px 14px;
  border-radius: 100px;
  font-size: 16px;
  font-weight: 600;
  flex-shrink: 0;

  &.warning {
    background: #FFF0F0;
    color: $error;
  }
}

.question-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  max-width: 600px;
}

.question-emoji { font-size: 80px; }
.question-text { text-align: center; }

.options-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  width: 100%;
}

.option-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  border: 2px solid #E8F0FE;
  border-radius: $radius-md;
  background: $white;
  cursor: pointer;
  transition: all $transition-fast;

  &:active { transform: scale(0.97); }

  &.correct {
    background: #E8FFF0;
    border-color: $success;
  }

  &.wrong {
    background: #FFF0F0;
    border-color: $error;
    animation: shake 0.3s ease;
  }
}

.option-label {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #F5F5F5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: $text;
  text-align: center;
  line-height: 28px;
}

.option-text {
  font-size: 16px;
  color: $text;
}

/* ===== 结果屏 ===== */
.result-screen {
  gap: 12px;
}

.result-emoji { font-size: 80px; display: block; }

.result-stars {
  display: flex;
  gap: 8px;
  font-size: 40px;
}

.result-star {
  color: #E0E0E0;
  transition: all 0.3s ease;

  &.filled { color: $gold; }
  &.animate { animation: popIn 0.5s ease backwards; }
  &:nth-child(1).animate { animation-delay: 0.2s; }
  &:nth-child(2).animate { animation-delay: 0.4s; }
  &:nth-child(3).animate { animation-delay: 0.6s; }
}

.reward-row {
  display: flex;
  gap: 12px;
  margin: 4px 0;
}

.reward-card {
  padding: 12px 20px;
  text-align: center;
  min-width: 80px;
}

.reward-emoji { font-size: 24px; display: block; margin-bottom: 4px; }
.reward-value { display: block; }
.reward-label { display: block; margin-top: 2px; }

.result-stats {
  width: 300px;
  padding: 16px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;

  & + .stat-row { border-top: 1px solid #F5F5F5; }
}

.result-actions {
  display: flex;
  gap: 12px;
  margin-top: 4px;
}

/* ===== 反馈蒙层 ===== */
.feedback-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  pointer-events: none;

  &.correct-overlay { background: rgba(46, 204, 113, 0.15); }
  &.wrong-overlay { background: rgba(231, 76, 60, 0.15); }
}

.feedback-text { text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2); }
</style>
