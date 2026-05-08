<template>
  <AppLayout theme="kids" :show-topbar="false">
    <view class="adaptive-container">
      <!-- 开始屏 -->
      <view v-if="screen === 'start'" class="screen start-screen">
        <text class="start-emoji animate-pulse">🎯</text>
        <text class="start-title text-title text-bold">薄弱点练习</text>
        <text class="start-subtitle text-light">根据你的错题智能组题</text>
        <text v-if="!totalQuestions" class="start-tip">正在分析你的学习数据...</text>
        <text v-else class="start-info text-sm">为你准备了 {{ totalQuestions }} 道针对性题目</text>
        <tn-button type="primary" size="xl" shape="round" :disabled="!totalQuestions" @click="startQuiz" style="background: linear-gradient(135deg, #FF6B6B, #FF8E53);">开始练习</tn-button>
      </view>

      <!-- 答题屏 -->
      <view v-if="screen === 'quiz'" class="screen quiz-screen">
        <view class="quiz-topbar">
          <view class="close-btn" @tap="exitQuiz"><text>✕</text></view>
          <view class="quiz-progress">
            <tn-line-progress :percent="(currentIndex + 1) / totalQuestions * 100" active-color="#FF6B6B" :height="10" :show-percent="false" />
          </view>
          <view class="progress-text">
            <text>{{ currentIndex + 1 }}/{{ totalQuestions }}</text>
          </view>
        </view>

        <view class="question-area">
          <view class="question-speech" :class="{ speaking: isSpeaking }" @tap="questionToSpeech()">
            <text class="question-emoji">❓</text>
            <text class="speech-hint">{{ isSpeaking ? '正在播放...' : '点这里听题' }}</text>
          </view>
          <view class="question-text text-title text-bold" @tap="questionToSpeech()">
            <rich-text :nodes="currentQuestion.nodes" />
          </view>

          <view class="options-grid">
            <view
              v-for="(opt, i) in currentQuestion.options"
              :key="i"
              class="option-btn"
              :class="getOptionClass(opt)"
              @tap="selectOption(opt)"
            >
              <text class="option-label">{{ opt.label }}</text>
              <rich-text class="option-text" :nodes="opt.nodes" />
            </view>
          </view>
        </view>
      </view>

      <!-- 结果屏 -->
      <view v-if="screen === 'result'" class="screen result-screen">
        <text class="result-emoji animate-pop-in">{{ correctCount >= totalQuestions ? '🎉' : correctCount > 0 ? '👍' : '💪' }}</text>
        <text class="result-title text-title text-bold">{{ correctCount >= totalQuestions ? '全部掌握！' : '继续加油！' }}</text>
        <text class="result-subtitle text-light">答对 {{ correctCount }}/{{ totalQuestions }} 题{{ masteredCount > 0 ? '，掌握 ' + masteredCount + ' 道错题' : '' }}</text>

        <view class="result-stats card">
          <view class="stat-row">
            <text class="text-sm text-light">正确率</text>
            <text class="text-sm text-bold">{{ accuracy }}%</text>
          </view>
          <view class="stat-row">
            <text class="text-sm text-light">已掌握错题</text>
            <text class="text-sm text-bold text-primary">{{ masteredCount }} 道</text>
          </view>
        </view>

        <view class="result-actions">
          <tn-button type="primary" size="lg" shape="round" @click="retryQuiz" style="background: linear-gradient(135deg, #FF6B6B, #FF8E53);">再练一次</tn-button>
          <tn-button size="lg" shape="round" @click="goBack">返回</tn-button>
        </view>
      </view>

      <!-- 答对反馈 -->
      <view v-if="showCorrect" class="feedback-overlay correct-overlay">
        <view class="feedback-content animate-pop-in">
          <text class="feedback-text text-title text-bold text-white">✅ 正确！</text>
          <text v-if="masteredThis" class="mastered-badge animate-pop-in">🎯 已掌握</text>
        </view>
      </view>

      <!-- 答错反馈 -->
      <view v-if="showWrong" class="feedback-overlay wrong-overlay">
        <view class="feedback-content animate-pop-in">
          <text class="feedback-text text-title text-bold text-white animate-shake">❌ 再想想</text>
        </view>
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { getAdaptiveQuestions, retryWrong } from '@/api/learn'
import { richContentToNodes, richContentToText } from '@/utils/richContent.mjs'
import { resolveQuestionSpeech } from '@/utils/questionSpeech.mjs'
import {
  loadQuestionsWithOfflineCache,
  prefetchAudioFile,
  readCachedAudioUrl
} from '@/utils/offlineQuizCache.mjs'

const screen = ref('start')
const currentIndex = ref(0)
const selectedAnswer = ref('')
const showCorrect = ref(false)
const showWrong = ref(false)
const masteredThis = ref(false)
const masteredCount = ref(0)
const correctCount = ref(0)
const subjectIdParam = ref(null)
const questions = ref([])
let questionAudio = null
const isSpeaking = ref(false)

function stopQuestionSpeech() {
  isSpeaking.value = false
  if (questionAudio) {
    questionAudio.stop()
    questionAudio.destroy()
    questionAudio = null
  }
  if (typeof window !== 'undefined' && window.speechSynthesis) {
    window.speechSynthesis.cancel()
  }
}

const totalQuestions = computed(() => questions.value.length)
const currentQuestion = computed(() =>
  questions.value[currentIndex.value] || { text: '', nodes: [], options: [] }
)
const accuracy = computed(() =>
  totalQuestions.value ? Math.round(correctCount.value / totalQuestions.value * 100) : 0
)

async function loadQuestions() {
  try {
    const res = await loadQuestionsWithOfflineCache({
      levelId: `adaptive:${subjectIdParam.value || 'all'}`,
      gradeLevelId: 'wrong-topics',
      fetchQuestions: () => getAdaptiveQuestions(subjectIdParam.value),
      storage: uni
    })
    if (res.fromCache) {
      uni.showToast({ title: '已加载离线题目', icon: 'none' })
    }
    if (res.questions && Array.isArray(res.questions) && res.questions.length > 0) {
      questions.value = res.questions.map(q => ({
        id: q.id,
        text: q.questionText || richContentToText(q.questionContent),
        questionContent: q.questionContent,
        questionSpeechText: q.questionSpeechText || '',
        nodes: richContentToNodes(q.questionContent),
        questionAudioUrl: q.questionAudioUrl || '',
        score: q.score || 10,
        options: (q.options || []).map((opt, i) => ({
          label: opt.optionLabel || String.fromCharCode(65 + i),
          answerValue: opt.answerValue || opt.optionLabel || String.fromCharCode(65 + i),
          text: opt.optionText || richContentToText(opt.optionContent),
          nodes: richContentToNodes(opt.optionContent),
          correct: false
        }))
      }))
      if (questions.value[0]) {
        preloadQuestionAudio(questions.value[0])
      }
    }
  } catch (e) {
    console.log('adaptive: 加载题目失败')
  }
}

onMounted(async () => {
  const pages = getCurrentPages()
  const page = pages[pages.length - 1]
  const sid = page.$page?.options?.subjectId
  if (sid) subjectIdParam.value = sid
  await loadQuestions()
})

function fallbackToSpeech(text) {
  if (!speakQuestionText(text)) {
    uni.showToast({ title: '语音文件准备中', icon: 'none' })
  }
}

function playQuestionAudio(audioUrl, fallbackText) {
  if (!audioUrl || typeof uni.createInnerAudioContext !== 'function') {
    return false
  }
  stopQuestionSpeech()
  isSpeaking.value = true
  questionAudio = uni.createInnerAudioContext()
  questionAudio.src = audioUrl
  questionAudio.onEnded(() => { stopQuestionSpeech() })
  questionAudio.onError(() => {
    stopQuestionSpeech()
    fallbackToSpeech(fallbackText)
  })
  try {
    const result = questionAudio.play()
    if (result && typeof result.catch === 'function') {
      result.catch(() => {
        stopQuestionSpeech()
        fallbackToSpeech(fallbackText)
      })
    }
  } catch (e) {
    return false
  }
  return true
}

function speakQuestionText(text) {
  // #ifdef H5
  if (typeof window !== 'undefined' && window.speechSynthesis && window.SpeechSynthesisUtterance && text) {
    window.speechSynthesis.cancel()
    isSpeaking.value = true
    const utterance = new window.SpeechSynthesisUtterance(text)
    utterance.lang = 'zh-CN'
    utterance.rate = 0.9
    utterance.onend = () => { isSpeaking.value = false }
    utterance.onerror = () => { isSpeaking.value = false }
    window.speechSynthesis.speak(utterance)
    return true
  }
  // #endif
  return false
}

async function preloadQuestionAudio(question) {
  const speech = resolveQuestionSpeech(question)
  if (!speech.audioUrl) return
  const source = await prefetchAudioFile(speech.audioUrl, uni)
  if (source && source !== speech.audioUrl) {
    question.cachedQuestionAudioUrl = source
  }
}

function questionToSpeech() {
  const q = currentQuestion.value
  const speech = resolveQuestionSpeech(q)
  let audioUrl = speech.audioUrl
  const cachedAudioUrl = readCachedAudioUrl(speech.audioUrl, uni)
  if (cachedAudioUrl) {
    q.cachedQuestionAudioUrl = cachedAudioUrl
    audioUrl = cachedAudioUrl
  } else if (speech.audioUrl) {
    prefetchAudioFile(speech.audioUrl, uni).then((source) => {
      if (source && source !== speech.audioUrl) {
        q.cachedQuestionAudioUrl = source
      }
    })
  }
  if (playQuestionAudio(audioUrl, speech.text)) {
    return
  }
  fallbackToSpeech(speech.text)
}

function getOptionClass(opt) {
  if (!selectedAnswer.value) return ''
  if (opt.correct) return 'correct'
  if (opt.label === selectedAnswer.value && !opt.correct) return 'wrong'
  return ''
}

async function selectOption(opt) {
  if (selectedAnswer.value) return
  stopQuestionSpeech()
  selectedAnswer.value = opt.label

  try {
    const res = await retryWrong(currentQuestion.value.id, opt.answerValue || opt.label)
    const isCorrect = res?.correct || false
    masteredThis.value = res?.mastered || false

    if (isCorrect) {
      correctCount.value++
      showCorrect.value = true
      if (masteredThis.value) masteredCount.value++
    } else {
      showWrong.value = true
    }

    setTimeout(() => {
      showCorrect.value = false
      showWrong.value = false
      nextQuestion()
    }, isCorrect ? 2000 : 2500)
  } catch (e) {
    showWrong.value = true
    setTimeout(() => { showWrong.value = false; nextQuestion() }, 2000)
  }
}

function nextQuestion() {
  selectedAnswer.value = ''
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value++
    preloadQuestionAudio(questions.value[currentIndex.value])
  } else {
    screen.value = 'result'
  }
}

function startQuiz() {
  if (!totalQuestions.value) return
  screen.value = 'quiz'
  currentIndex.value = 0
  correctCount.value = 0
  masteredCount.value = 0
}

async function retryQuiz() {
  screen.value = 'start'
  currentIndex.value = 0
  correctCount.value = 0
  masteredCount.value = 0
  await loadQuestions()
}

function exitQuiz() {
  stopQuestionSpeech()
  uni.navigateBack()
}

function goBack() {
  uni.navigateBack()
}

onUnmounted(() => {
  stopQuestionSpeech()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.adaptive-container { width: 100%; height: 100%; position: relative; overflow: hidden; }
.screen { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 28px; }

.start-screen { gap: 16px; }
.start-emoji { font-size: 96px; display: block; }
.start-title { text-align: center; }
.start-subtitle { text-align: center; }
.start-tip { min-height: 36px; padding: 8px 14px; border-radius: 18px; background: #FFF8E6; color: $warning; font-size: 13px; font-weight: 700; }
.start-info { padding: 8px 14px; border-radius: 18px; background: #F0F7FF; color: $learn-blue; font-size: 14px; font-weight: 600; }

.quiz-screen { justify-content: flex-start; padding-top: 16px; }
.quiz-topbar { width: 100%; display: flex; align-items: center; gap: 12px; margin-bottom: 18px; }
.close-btn { width: 48px; height: 48px; border-radius: $radius; background: #F5F5F5; display: flex; align-items: center; justify-content: center; font-size: 16px; cursor: pointer; flex-shrink: 0; }
.quiz-progress { flex: 1; }
.progress-text { min-width: 48px; text-align: center; font-size: 14px; font-weight: 600; color: $text-secondary; }

.question-area { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 14px; width: 100%; max-width: 680px; }
.question-speech { display: flex; flex-direction: column; align-items: center; gap: 4px; padding: 10px 18px; border-radius: $radius-xl; background: #FFF0F0; }
.speech-hint { font-size: 13px; color: #FF6B6B; font-weight: 800; }
.question-text { text-align: center; line-height: 1.35; }

.options-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; width: 100%; }
.option-btn {
  display: flex; align-items: center; gap: 10px; min-height: 78px; padding: 18px 20px;
  border: 2px solid #E8F0FE; border-radius: $radius-md; background: $white;
  cursor: pointer; transition: all $transition-fast;
  &:active { transform: scale(0.97); }
  &.correct { background: #E8FFF0; border-color: $success; }
  &.wrong { background: #FFF0F0; border-color: $error; animation: shake 0.3s ease; }
}
.option-label { width: 34px; height: 34px; border-radius: 50%; background: #F5F5F5; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 600; color: $text; text-align: center; line-height: 34px; }
.option-text { flex: 1; display: block; font-size: 18px; font-weight: 700; color: $text; line-height: 1.35; }

.result-screen { gap: 12px; }
.result-emoji { font-size: 80px; display: block; }
.result-stats { width: 300px; padding: 16px; }
.stat-row { display: flex; justify-content: space-between; padding: 6px 0; & + .stat-row { border-top: 1px solid #F5F5F5; } }
.result-actions { display: flex; gap: 12px; margin-top: 4px; }

.feedback-overlay {
  position: absolute; top: 0; left: 0; right: 0; bottom: 0;
  display: flex; align-items: center; justify-content: center; z-index: 999; pointer-events: none;
  &.correct-overlay { background: rgba(46, 204, 113, 0.15); }
  &.wrong-overlay { background: rgba(231, 76, 60, 0.15); }
}
.feedback-text { text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2); }
.feedback-content { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.mastered-badge {
  display: inline-flex; align-items: center; padding: 4px 14px; border-radius: 20px;
  background: rgba(255, 255, 255, 0.3); color: $success; font-size: 18px;
  font-weight: 800; text-shadow: 0 1px 4px rgba(0,0,0,0.2);
}

@keyframes shake { 0%, 100% { transform: translateX(0); } 20% { transform: translateX(-8px); } 40% { transform: translateX(8px); } 60% { transform: translateX(-4px); } 80% { transform: translateX(4px); } }
</style>
