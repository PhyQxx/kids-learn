<template>
  <AppLayout theme="kids" :show-topbar="false">
    <view class="quiz-container">
      <!-- 开始屏 -->
      <view v-if="screen === 'start'" class="screen start-screen">
        <text class="start-emoji animate-pulse">{{ levelEmoji }}</text>
        <text class="start-title text-title text-bold">{{ levelName }}</text>
        <text class="start-subtitle text-light">准备好了吗？</text>
        <text v-if="!totalQuestions" class="start-tip">题目正在准备中，请稍等一下</text>
        <view class="info-row">
          <view class="info-item card">
            <text class="info-emoji">📝</text>
            <text class="info-text text-sm">{{ totalQuestions }} 题</text>
          </view>
          <view class="info-item card" v-if="!isPractice">
            <text class="info-emoji">⭐</text>
            <text class="info-text text-sm">目标 3 星</text>
          </view>
          <view class="info-item card" v-if="timeLimit > 0">
            <text class="info-emoji">⏱️</text>
            <text class="info-text text-sm">{{ timeLimit }} 秒</text>
          </view>
          <view class="info-item card" v-else-if="isPractice">
            <text class="info-emoji">🔄</text>
            <text class="info-text text-sm">无尽模式</text>
          </view>
        </view>
        <tn-button type="primary" size="xl" shape="round" :disabled="!totalQuestions" @click="startQuiz" style="background: linear-gradient(135deg, #4A90D9, #6BA3E0);">开始答题</tn-button>
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
          <view class="hint-btn" :class="{ used: hintUsed }" @tap="useHint">
            <text>{{ hintUsed ? '🐾 已用' : '🐾 提示' }}</text>
          </view>
          <view class="timer" :class="{ warning: timeLimit > 0 && countdown <= 10 }">
            <text v-if="timeLimit > 0">{{ countdown }}s</text>
            <text v-else>{{ usedTimeInQuiz }}s</text>
          </view>
        </view>

        <!-- 题目区域 -->
        <view class="question-area">
          <view class="question-speech" :class="{ speaking: isSpeaking }" @tap="questionToSpeech()">
            <text class="question-emoji">{{ currentQuestion.emoji }}</text>
            <view v-if="isSpeaking" class="sound-wave">
              <view class="sound-bar" v-for="i in 4" :key="i"></view>
            </view>
            <text class="speech-hint">{{ isSpeaking ? '正在播放...' : '点这里听题' }}</text>
          </view>
          <text class="question-count">第 {{ currentIndex + 1 }} / {{ totalQuestions }} 题</text>
          <view class="question-text text-title text-bold" @tap="questionToSpeech()">
            <rich-text :nodes="currentQuestion.nodes" />
          </view>

          <!-- 选项网格 (单选/判断) -->
          <view v-if="currentQuestion.interactionType === 'single'" class="options-grid">
            <view
              v-for="(opt, i) in currentQuestion.options"
              :key="i"
              class="option-btn"
              :class="getOptionClass(opt)"
              @tap="selectOption(opt)"
            >
              <text class="option-label" v-if="opt.label">{{ opt.label }}</text>
              <rich-text class="option-text" :nodes="opt.nodes" />
            </view>
          </view>

          <!-- 填空题 -->
          <view v-else-if="currentQuestion.interactionType === 'fill'" class="fill-panel card">
            <input class="fill-input" v-model="fillAttempt" placeholder="请输入答案..." />
            <tn-button type="primary" shape="round" size="lg" :disabled="!fillAttempt || !!selectedAnswer" @click="submitFillAnswer">提交答案</tn-button>
          </view>

          <!-- 排序题 -->
          <view v-else-if="currentQuestion.interactionType === 'order'" class="order-panel card">
            <view v-for="(item, i) in orderItems" :key="item.answerValue" class="order-row">
              <text class="order-index">{{ i + 1 }}</text>
              <text class="order-text">{{ item.text }}</text>
              <view class="order-actions">
                <view class="mini-btn" :class="{ disabled: i === 0 }" @tap="moveOrderItem(i, -1)">↑</view>
                <view class="mini-btn" :class="{ disabled: i === orderItems.length - 1 }" @tap="moveOrderItem(i, 1)">↓</view>
              </view>
            </view>
            <tn-button type="primary" shape="round" size="lg" :disabled="!!selectedAnswer" @click="submitOrderAnswer">提交排序</tn-button>
          </view>

          <!-- 连线题 -->
          <view v-else-if="currentQuestion.interactionType === 'match'" class="match-panel">
            <view class="match-column card">
              <view
                v-for="left in currentQuestion.options"
                :key="left.answerValue"
                class="match-item"
                :class="{ active: selectedMatchLeft === left.answerValue, paired: matchPairs[left.answerValue] }"
                @tap="selectMatchLeft(left)"
              >
                <text class="match-label">{{ left.pairLeft }}</text>
                <text class="match-pair">{{ matchRightText(matchPairs[left.answerValue]) }}</text>
              </view>
            </view>
            <view class="match-column card">
              <view
                v-for="right in matchRightItems"
                :key="right.answerValue"
                class="match-item right"
                :class="{ paired: matchedRightValues.has(right.answerValue) }"
                @tap="selectMatchRight(right)"
              >
                <text class="match-label">{{ right.pairRight }}</text>
              </view>
            </view>
            <tn-button class="match-submit" type="primary" shape="round" size="lg" :disabled="!canSubmitMatch || !!selectedAnswer" @click="submitMatchAnswer">提交连线</tn-button>
          </view>

          <!-- 语音题 -->
          <view v-else-if="currentQuestion.interactionType === 'voice'" class="voice-panel card">
            <text class="voice-target">{{ currentQuestion.voiceText }}</text>
            <view class="voice-actions">
              <tn-button shape="round" size="lg" @click="questionToSpeech()">听一遍</tn-button>
              <tn-button type="primary" shape="round" size="lg" :loading="voiceListening" @click="startVoicePractice">{{ voiceListening ? '聆听中' : '开始跟读' }}</tn-button>
            </view>
            <text v-if="voiceAttempt" class="voice-attempt">{{ voiceAttempt }}</text>
            <tn-button type="primary" shape="round" size="lg" :disabled="!voiceAttempt || !!selectedAnswer" @click="submitVoiceAnswer">提交跟读</tn-button>
          </view>
        </view>
      </view>

      <!-- 结果屏 -->
      <view v-if="screen === 'result'" class="screen result-screen">
        <!-- 3星宝箱动画 -->
        <view v-if="showTreasureChest" class="treasure-chest animate-chest-open">
          <text class="chest-emoji">🎁</text>
          <view class="chest-coins" v-if="showRewardAnimation">
            <text
              v-for="p in coinParticles"
              :key="'chest-' + p.id"
              class="chest-coin animate-coin-scatter"
              :style="p.style"
            >🪙</text>
          </view>
        </view>
        <text v-else class="result-emoji animate-pop-in">{{ resultEmoji }}</text>

        <!-- 星级 (闯关时显示) -->
        <view class="result-stars" v-if="!isPractice">
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
        <view class="reward-row" :class="{ 'animate-slide-up': showRewardAnimation }">
          <view class="reward-card card">
            <text class="reward-emoji">🪙</text>
            <AnimatedNumber v-if="showRewardAnimation" :value="rewards.gold" :duration="800" prefix="+" class="reward-value text-md text-bold" />
            <text v-else class="reward-value text-md text-bold">+{{ rewards.gold }}</text>
            <text class="reward-label text-xs text-light">金币</text>
          </view>
          <view class="reward-card card">
            <text class="reward-emoji">⚡</text>
            <AnimatedNumber v-if="showRewardAnimation" :value="rewards.exp" :duration="800" prefix="+" class="reward-value text-md text-bold" />
            <text v-else class="reward-value text-md text-bold">+{{ rewards.exp }}</text>
            <text class="reward-label text-xs text-light">经验</text>
          </view>
          <view class="reward-card card" v-if="!isPractice">
            <text class="reward-emoji">🎨</text>
            <AnimatedNumber v-if="showRewardAnimation" :value="rewards.stickers" :duration="600" prefix="x" class="reward-value text-md text-bold" />
            <text v-else class="reward-value text-md text-bold">x{{ rewards.stickers }}</text>
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

        <view v-if="challengeResult" class="challenge-result card">
          <view class="challenge-result-main">
            <text class="challenge-result-icon">{{ challengeResult.isWin ? '🏆' : '🛡️' }}</text>
            <view>
              <text class="text-md text-bold">{{ challengeResult.isWin ? 'PK 获胜' : 'PK 已完成' }}</text>
              <text class="text-xs text-light">对手得分 {{ challengeResult.opponentScore }} · 段位积分 {{ challengeResult.rankDelta >= 0 ? '+' : '' }}{{ challengeResult.rankDelta }}</text>
            </view>
          </view>
          <text class="text-sm text-bold text-primary">+{{ challengeResult.rewardGold }} 金币</text>
        </view>

        <view class="result-actions">
          <tn-button type="primary" size="lg" shape="round" @click="goNextLevel" style="background: linear-gradient(135deg, #4A90D9, #6BA3E0);">{{ isPractice ? '再来一次' : '下一关' }}</tn-button>
          <tn-button size="lg" shape="round" @click="goBack">{{ isPractice ? '结束练习' : '返回' }}</tn-button>
        </view>
      </view>

      <!-- 答对反馈 -->
      <view v-if="showCorrect" class="feedback-overlay correct-overlay">
        <view class="feedback-content animate-pop-in">
          <text class="feedback-text text-title text-bold text-white">✅ 正确！</text>
          <text v-if="petExpGained" class="pet-exp-badge animate-pop-in">🐾 +{{ petExpGained }}</text>
        </view>
        <!-- 金币/星星飞入粒子 -->
        <view class="coin-particles" v-if="coinParticles.length">
          <text
            v-for="p in coinParticles"
            :key="p.id"
            class="coin-particle animate-coin-scatter"
            :style="p.style"
          >{{ p.emoji }}</text>
        </view>
        <!-- 得分飞入 -->
        <view class="score-fly-up positive">
          <text>+{{ currentQuestion.score || 10 }}分</text>
        </view>
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
import RewardOverlay from '@/components/common/RewardOverlay.vue'
import AnimatedNumber from '@/components/common/AnimatedNumber.vue'
import { useLearnStore } from '@/store/learn'
import { useUserStore } from '@/store/user'
import { usePetStore } from '@/store/pet'
import { getQuestions, submitAnswer, completeLevel, getHint, startPractice } from '@/api/learn'
import { submitChallengeResult } from '@/api/challenge'
import { getUserInfo } from '@/api/user'
import { resolveQuestionSpeech } from '@/utils/questionSpeech.mjs'
import {
  loadQuestionsWithOfflineCache,
  prefetchAudioFile,
  readCachedAudioUrl
} from '@/utils/offlineQuizCache.mjs'
import {
  buildMatchAnswer,
  buildOrderAnswer,
  normalizeQuizQuestion,
  normalizeSpeechAttempt
} from '@/utils/questionInteraction.mjs'

const learnStore = useLearnStore()
const userStore = useUserStore()
const petStore = usePetStore()

const FEEDBACK_AUDIO_BASE = 'https://ftp.pnkx.top:8/ftp/kids-learn/question/audio/seed/feedback'
const CORRECT_FEEDBACKS = ['correct-1', 'correct-2', 'correct-3', 'correct-4', 'correct-5']
const WRONG_FEEDBACKS = ['wrong-1', 'wrong-2', 'wrong-3']
let lastCorrectIdx = -1
let lastWrongIdx = -1
let feedbackAudio = null

function feedbackAudioUrl(name) {
  return `${FEEDBACK_AUDIO_BASE}/${name}.wav`
}

function preloadFeedbackAudio() {
  const preload = async (name) => {
    if (typeof uni.createInnerAudioContext !== 'function') return
    const source = await prefetchAudioFile(feedbackAudioUrl(name), uni)
    const a = uni.createInnerAudioContext()
    a.src = source
    a.onCanplay(() => { a.destroy() })
    a.onError(() => { a.destroy() })
  }
  CORRECT_FEEDBACKS.forEach(preload)
  WRONG_FEEDBACKS.forEach(preload)
}

function stopFeedbackAudio() {
  if (feedbackAudio) {
    feedbackAudio.stop()
    feedbackAudio.destroy()
    feedbackAudio = null
  }
}

async function playFeedbackAudio(type) {
  if (typeof uni.createInnerAudioContext !== 'function') return
  const list = type === 'correct' ? CORRECT_FEEDBACKS : WRONG_FEEDBACKS
  let prevIdx = type === 'correct' ? lastCorrectIdx : lastWrongIdx
  let idx = Math.floor(Math.random() * list.length)
  if (idx === prevIdx && list.length > 1) idx = (idx + 1) % list.length
  if (type === 'correct') lastCorrectIdx = idx; else lastWrongIdx = idx

  const source = await prefetchAudioFile(feedbackAudioUrl(list[idx]), uni)
  stopFeedbackAudio()
  feedbackAudio = uni.createInnerAudioContext()
  feedbackAudio.src = source
  feedbackAudio.onEnded(() => { feedbackAudio.destroy(); feedbackAudio = null })
  feedbackAudio.onError(() => { feedbackAudio.destroy(); feedbackAudio = null })
  try {
    const result = feedbackAudio.play()
    if (result && typeof result.catch === 'function') {
      result.catch(() => { feedbackAudio.destroy(); feedbackAudio = null })
    }
  } catch (e) {
    feedbackAudio = null
  }
}

const screen = ref('start')
const currentIndex = ref(0)
const selectedAnswer = ref('')
const orderItems = ref([])
const matchPairs = ref({})
const matchRightItems = ref([])
const selectedMatchLeft = ref('')
const voiceAttempt = ref('')
const fillAttempt = ref('')
const voiceListening = ref(false)
const showCorrect = ref(false)
const showWrong = ref(false)
const countdown = ref(60)
const timeLimit = ref(60)
const usedTimeInQuiz = ref(0)
let timer = null

const levelId = ref(null)
const pageGradeLevelId = ref(null)
const challengeId = ref(null)
const opponentId = ref(null)
const isPractice = ref(false)
const practiceModeId = ref(null)
const levelName = ref(learnStore.currentLevel?.name || '第 1 关')
const levelEmoji = ref('🎮')

// 题目列表，由后端加载
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

function fallbackToSpeech(text) {
  if (!speakQuestionText(text)) {
    uni.showToast({ title: '语音暂不可用', icon: 'none' })
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
  questionAudio.onEnded(() => {
    stopQuestionSpeech()
  })
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

// 语音播报
async function preloadQuestionAudio(question) {
  const speech = resolveQuestionSpeech(question)
  if (!speech.audioUrl) return
  const source = await prefetchAudioFile(speech.audioUrl, uni)
  if (source && source !== speech.audioUrl) {
    question.cachedQuestionAudioUrl = source
  }
  if (typeof uni.createInnerAudioContext !== 'function') return
  const audio = uni.createInnerAudioContext()
  audio.src = source || speech.audioUrl
  audio.onCanplay(() => { audio.destroy() })
  audio.onError(() => { audio.destroy() })
}

const questionToSpeech = async (question = currentQuestion.value) => {
  const speech = resolveQuestionSpeech(question)
  let audioUrl = speech.audioUrl
  const cachedAudioUrl = readCachedAudioUrl(speech.audioUrl, uni)
  if (cachedAudioUrl) {
    question.cachedQuestionAudioUrl = cachedAudioUrl
    audioUrl = cachedAudioUrl
  } else if (speech.audioUrl) {
    prefetchAudioFile(speech.audioUrl, uni).then((source) => {
      if (source && source !== speech.audioUrl) {
        question.cachedQuestionAudioUrl = source
      }
    })
  }
  if (playQuestionAudio(audioUrl, speech.text)) {
    return
  }
  if (!speakQuestionText(speech.text)) {
    uni.showToast({ title: '语音文件准备中', icon: 'none' })
  }
}

onMounted(async () => {
  const pages = getCurrentPages()
  const page = pages[pages.length - 1]
  const options = page.$page?.options || {}

  if (options.practiceModeId) {
    isPractice.value = true
    practiceModeId.value = options.practiceModeId
    timeLimit.value = parseInt(options.timeLimit) || 0
    try {
      const res = await startPractice(practiceModeId.value)
      levelName.value = res.modeName || '专项练习'
      levelEmoji.value = '📝'
      if (res.questions && Array.isArray(res.questions) && res.questions.length > 0) {
        questions.value = res.questions.map(q => normalizeQuizQuestion(q))
        if (questions.value[0]) {
          preloadQuestionAudio(questions.value[0])
          resetInteractionState()
        }
      } else {
        uni.showToast({ title: '该练习下没有题目', icon: 'none' })
      }
    } catch (e) {
      uni.showToast({ title: '加载练习失败', icon: 'none' })
    }
  } else {
    levelId.value = options.levelId || learnStore.currentLevel?.id
    pageGradeLevelId.value = options.gradeLevelId || userStore.userInfo?.gradeLevelId || null
    challengeId.value = options.challengeId || null
    opponentId.value = options.opponentId || null
    if (levelId.value) {
      try {
        const res = await loadQuestionsWithOfflineCache({
          levelId: levelId.value,
          gradeLevelId: pageGradeLevelId.value,
          fetchQuestions: getQuestions,
          storage: uni
        })
        if (res.fromCache) {
          uni.showToast({ title: '已加载离线题目', icon: 'none' })
        }
        if (res.questions && Array.isArray(res.questions) && res.questions.length > 0) {
          questions.value = res.questions.map(q => normalizeQuizQuestion(q))
          // Preload first question audio
          if (questions.value[0]) {
            preloadQuestionAudio(questions.value[0])
            resetInteractionState()
          }
        }
      } catch (e) {
        console.log('quiz: 使用模拟题目')
      }
    }
  }
})

const totalQuestions = computed(() => questions.value.length)
const currentQuestion = computed(() => {
  return questions.value[currentIndex.value] || { emoji: '❓', interactionType: 'single', text: '', plainText: '', nodes: [], options: [] }
})
const correctCount = ref(0)
const totalScore = ref(0)
const earnedStars = ref(0)
const startTime = ref(0)
const questionStartTime = ref(0)
const usedTime = ref(0)

const rewards = ref({ gold: 15, exp: 10, stickers: 1 })
const challengeResult = ref(null)

// 奖励动画状态
const showRewardAnimation = ref(false)
const showTreasureChest = ref(false)
const coinParticles = ref([])

// 宠物提示技能
const hintUsed = ref(false)
const hintLoading = ref(false)
const eliminatedOptions = ref(new Set()) // option labels to gray out
const showPetExp = ref(false)
const petExpGained = ref(0)

const accuracy = computed(() =>
  totalQuestions.value ? Math.round(correctCount.value / totalQuestions.value * 100) : 0
)
const matchedRightValues = computed(() => new Set(Object.values(matchPairs.value)))
const canSubmitMatch = computed(() =>
  currentQuestion.value.options.length > 0
  && Object.keys(matchPairs.value).length === currentQuestion.value.options.length
)

const resultEmoji = computed(() => {
  if (isPractice.value) return '🏆'
  return earnedStars.value >= 3 ? '🎉' : earnedStars.value >= 1 ? '👍' : '💪'
})
const resultTitle = computed(() => {
  if (isPractice.value) return '练习完成！'
  return earnedStars.value >= 3 ? '太棒了！' : earnedStars.value >= 1 ? '不错哦！' : '继续加油！'
})
const resultSubtitle = computed(() => {
  if (isPractice.value) return `本次练习你答对了 ${correctCount.value} 题`
  return earnedStars.value >= 3 ? '你获得了满星评价！' : '再努力一下就能获得更多星星！'
})

function getOptionClass(opt) {
  if (eliminatedOptions.value.has(opt.label)) return 'eliminated'
  if (!selectedAnswer.value) return ''
  if (opt.correct) return 'correct'
  if (opt.label === selectedAnswer.value && !opt.correct) return 'wrong'
  return ''
}

async function useHint() {
  if (hintUsed.value || hintLoading.value || selectedAnswer.value) return
  const q = currentQuestion.value
  if (q.interactionType !== 'single') return
  if (!q.id) return
  hintLoading.value = true
  try {
    const res = await getHint(q.id)
    if (res?.keepOptions) {
      // Eliminate options NOT in keepOptions
      const keep = new Set(res.keepOptions)
      q.options.forEach(opt => {
        if (!keep.has(opt.label)) {
          eliminatedOptions.value.add(opt.label)
        }
      })
      hintUsed.value = true
    }
  } catch (e) {
    uni.showToast({ title: e.message || '提示失败', icon: 'none' })
  } finally {
    hintLoading.value = false
  }
}

function showPetExpGain(exp) {
  petExpGained.value = exp
  showPetExp.value = true
  setTimeout(() => { showPetExp.value = false }, 1500)
}

// 生成答对时的粒子
function generateCoinParticles(count = 8, isStar = true) {
  const particles = []
  for (let i = 0; i < count; i++) {
    const angle = (360 / count) * i
    const tx = Math.cos(angle * Math.PI / 180) * (60 + Math.random() * 40)
    const ty = Math.sin(angle * Math.PI / 180) * (60 + Math.random() * 40) - 20
    particles.push({
      id: i,
      emoji: isStar ? '⭐' : '🪙',
      style: {
        '--coin-tx': `${tx}px`,
        '--coin-ty': `${ty}px`,
        animationDelay: `${Math.random() * 0.3}s`
      }
    })
  }
  coinParticles.value = particles
}

function resetInteractionState() {
  const q = currentQuestion.value
  orderItems.value = q.interactionType === 'order' ? [...q.options] : []
  matchPairs.value = {}
  matchRightItems.value = q.interactionType === 'match' ? [...q.options].reverse() : []
  selectedMatchLeft.value = ''
  voiceAttempt.value = ''
  fillAttempt.value = ''
  voiceListening.value = false
}

function moveOrderItem(index, direction) {
  if (selectedAnswer.value) return
  const nextIndex = index + direction
  if (nextIndex < 0 || nextIndex >= orderItems.value.length) return
  const next = [...orderItems.value]
  const temp = next[index]
  next[index] = next[nextIndex]
  next[nextIndex] = temp
  orderItems.value = next
}

function selectMatchLeft(item) {
  if (selectedAnswer.value) return
  selectedMatchLeft.value = item.answerValue
}

function selectMatchRight(item) {
  if (selectedAnswer.value || !selectedMatchLeft.value) return
  matchPairs.value = {
    ...matchPairs.value,
    [selectedMatchLeft.value]: item.answerValue
  }
  selectedMatchLeft.value = ''
}

function matchRightText(answerValue) {
  if (!answerValue) return ''
  const right = currentQuestion.value.options.find(item => item.answerValue === answerValue)
  return right ? right.pairRight : ''
}

function startVoicePractice() {
  if (selectedAnswer.value || voiceListening.value) return
  const target = currentQuestion.value.voiceText || currentQuestion.value.questionSpeechText || currentQuestion.value.plainText
  const SpeechRecognition = typeof window !== 'undefined' && (window.SpeechRecognition || window.webkitSpeechRecognition)

  if (!SpeechRecognition) {
    voiceListening.value = true
    questionToSpeech()
    setTimeout(() => {
      voiceAttempt.value = target
      voiceListening.value = false
    }, 900)
    return
  }

  const recognition = new SpeechRecognition()
  recognition.lang = /[a-zA-Z]/.test(target) ? 'en-US' : 'zh-CN'
  recognition.interimResults = false
  recognition.maxAlternatives = 1
  recognition.onresult = (event) => {
    const transcript = event.results?.[0]?.[0]?.transcript || ''
    voiceAttempt.value = normalizeSpeechAttempt(transcript) ? transcript : target
  }
  recognition.onerror = () => {
    voiceAttempt.value = target
    voiceListening.value = false
  }
  recognition.onend = () => {
    voiceListening.value = false
  }
  voiceListening.value = true
  recognition.start()
}

function selectOption(opt) {
  if (selectedAnswer.value) return
  submitCurrentAnswer(opt.answerValue || opt.label, opt.label)
}

function submitOrderAnswer() {
  if (selectedAnswer.value) return
  submitCurrentAnswer(buildOrderAnswer(orderItems.value), 'ORDER')
}

function submitMatchAnswer() {
  if (selectedAnswer.value || !canSubmitMatch.value) return
  submitCurrentAnswer(buildMatchAnswer(matchPairs.value), 'MATCH')
}

function submitVoiceAnswer() {
  if (selectedAnswer.value || !voiceAttempt.value) return
  submitCurrentAnswer(voiceAttempt.value, 'VOICE')
}

function submitFillAnswer() {
  if (selectedAnswer.value || !fillAttempt.value) return
  submitCurrentAnswer(fillAttempt.value, 'FILL')
}

function submitCurrentAnswer(answer, displayAnswer = answer) {
  if (selectedAnswer.value) return
  stopQuestionSpeech()
  selectedAnswer.value = displayAnswer
  const q = currentQuestion.value
  const answerTime = Math.round((Date.now() - (questionStartTime.value || startTime.value)) / 1000)

  // 提交答案到后端判定
  if (q.id) {
    submitAnswer({
      questionId: q.id,
      answer,
      answerTime
    }).then(res => {
      const isCorrect = res?.correct || false
      if (isCorrect) {
        correctCount.value++
        totalScore.value += (q.score || 10)
        showCorrect.value = true
        // 增加震动反馈
        uni.vibrateShort()
        // 同时播放 UI 音效和语音反馈
        soundManager.play('success')
        playFeedbackAudio('correct')
        generateCoinParticles(8) // 增加粒子数量
        if (res?.petExp) showPetExpGain(res.petExp)
      } else {
        showWrong.value = true
        // 增加较明显的震动反馈
        uni.vibrateLong()
        soundManager.play('fail')
        playFeedbackAudio('wrong')
        // Mark correct answer in options
        if (res?.correctAnswer) {
          const cOpt = q.options.find(o => (o.answerValue || o.label) === res.correctAnswer)
          if (cOpt) cOpt.correct = true
        }
      }
      setTimeout(() => {
        showCorrect.value = false
        showWrong.value = false
        nextQuestion()
      }, isCorrect ? 2500 : 3000)
    }).catch(() => {
      showWrong.value = true
      playFeedbackAudio('wrong')
      setTimeout(() => { showWrong.value = false; nextQuestion() }, 3000)
    })
  } else {
    // 无 levelId 或 questionId 时随机判定（fallback mock 题目）
    showWrong.value = true
    setTimeout(() => { showWrong.value = false; nextQuestion() }, 1000)
  }
}

function nextQuestion() {
  selectedAnswer.value = ''
  eliminatedOptions.value = new Set()
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value++
    resetInteractionState()
    questionStartTime.value = Date.now()
    preloadQuestionAudio(questions.value[currentIndex.value])
    setTimeout(() => questionToSpeech(), 250)
  } else {
    finishQuiz()
  }
}

function startQuiz() {
  if (!totalQuestions.value) return
  screen.value = 'quiz'
  startTime.value = Date.now()
  questionStartTime.value = Date.now()
  resetInteractionState()
  preloadFeedbackAudio()
  setTimeout(() => questionToSpeech(), 250)

  if (timeLimit.value > 0) {
    countdown.value = timeLimit.value
  }

  timer = setInterval(() => {
    usedTimeInQuiz.value++
    if (timeLimit.value > 0) {
      countdown.value--
      if (countdown.value <= 0) {
        finishQuiz()
      }
    }
  }, 1000)
}

async function finishQuiz() {
  clearInterval(timer)
  stopQuestionSpeech()
  usedTime.value = Math.round((Date.now() - startTime.value) / 1000)

  // Calculate stars
  const acc = totalQuestions.value > 0 ? correctCount.value / totalQuestions.value : 0
  if (acc >= 0.9) earnedStars.value = 3
  else if (acc >= 0.7) earnedStars.value = 2
  else if (acc >= 0.5) earnedStars.value = 1
  else earnedStars.value = 0

  const wrongCount = totalQuestions.value - correctCount.value

  // 提交关卡完成 (仅闯关模式)
  if (levelId.value && !isPractice.value) {
    try {
      const res = await completeLevel(levelId.value, totalScore.value, usedTime.value, wrongCount)
      if (res) {
        earnedStars.value = res.stars || earnedStars.value
        rewards.value = {
          gold: res.gold || 10 + earnedStars.value * 10,
          exp: res.exp || 10 + earnedStars.value * 5,
          stickers: res.stickerId ? 1 : (earnedStars.value >= 2 ? 1 : 0)
        }
        if (res.gold !== undefined || res.exp !== undefined) {
          try {
            const userInfo = await getUserInfo()
            if (userInfo) userStore.setUserInfo(userInfo)
          } catch (e) {
            console.log('更新用户信息失败:', e)
          }
        }
      }
    } catch (e) {
      console.error('关卡完成失败:', e)
      uni.showToast({ title: '保存成绩失败: ' + (e.message || '请检查网络'), icon: 'none' })
      rewards.value = {
        gold: 10 + earnedStars.value * 10,
        exp: 10 + earnedStars.value * 5,
        stickers: earnedStars.value >= 2 ? 1 : 0
      }
    }
  } else {
    // 专项练习结算
    const practiceGold = Math.floor(correctCount.value * 2)
    const practiceExp = Math.floor(correctCount.value * 2)
    rewards.value = {
      gold: practiceGold,
      exp: practiceExp,
      stickers: 0
    }
    try {
      const userInfo = await getUserInfo()
      if (userInfo) userStore.setUserInfo(userInfo)
    } catch (e) {}
  }

  if (!isPractice.value) {
    await submitChallengeIfNeeded()
  }

  screen.value = 'result'

  // 触发奖励动画
  showTreasureChest.value = earnedStars.value >= 3 || isPractice.value
  setTimeout(() => {
    showRewardAnimation.value = true
    if (showTreasureChest.value) {
      generateCoinParticles(12)
    }
  }, 600)
}

async function submitChallengeIfNeeded() {
  if (!challengeId.value) return
  try {
    const res = await submitChallengeResult({
      challengeId: Number(challengeId.value),
      opponentId: opponentId.value ? Number(opponentId.value) : null,
      userScore: totalScore.value
    })
    if (!res) return
    challengeResult.value = {
      isWin: !!res.isWin,
      opponentScore: res.opponentScore || 0,
      rankDelta: res.rankDelta || 0,
      rewardGold: res.rewardGold || 0
    }
    rewards.value = {
      ...rewards.value,
      gold: rewards.value.gold + challengeResult.value.rewardGold
    }
    try {
      const userInfo = await getUserInfo()
      if (userInfo) userStore.setUserInfo(userInfo)
    } catch (e) {
      console.log('挑战奖励后更新用户信息失败:', e)
    }
  } catch (e) {
    console.log('挑战结算失败:', e)
    uni.showToast({ title: 'PK结算稍后同步', icon: 'none' })
  }
}

function exitQuiz() {
  clearInterval(timer)
  stopQuestionSpeech()
  stopFeedbackAudio()
  uni.navigateBack()
}

function goNextLevel() {
  if (isPractice.value) {
    // 重新开始练习
    screen.value = 'start'
    currentIndex.value = 0
    correctCount.value = 0
    totalScore.value = 0
    earnedStars.value = 0
    usedTime.value = 0
    usedTimeInQuiz.value = 0
    showRewardAnimation.value = false
    showTreasureChest.value = false
    if (questions.value[0]) {
      preloadQuestionAudio(questions.value[0])
    }
  } else {
    uni.navigateBack()
  }
}

function goBack() {
  uni.navigateBack()
}

onUnmounted(() => {
  clearInterval(timer)
  stopQuestionSpeech()
  stopFeedbackAudio()
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
  padding: 28px;
}

/* ===== 开始屏 ===== */
.start-screen {
  gap: 16px;
}

.start-emoji { font-size: 96px; display: block; }
.start-title { text-align: center; }
.start-subtitle { text-align: center; }
.start-tip {
  min-height: 36px;
  padding: 8px 14px;
  border-radius: 18px;
  background: #FFF8E6;
  color: $warning;
  font-size: 13px;
  font-weight: 700;
}

.info-row {
  display: flex;
  gap: 16px;
  margin: 8px 0 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 52px;
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
  margin-bottom: 18px;
}

.close-btn {
  width: 48px;
  height: 48px;
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

.hint-btn {
  min-width: 60px;
  min-height: 40px;
  padding: 6px 12px;
  border-radius: 100px;
  background: #FFF0E8;
  color: #FF8C42;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s ease;

  &:active { transform: scale(0.95); }

  &.used {
    background: #F0F0F0;
    color: #BBB;
    pointer-events: none;
  }
}

.timer {
  background: #F0F7FF;
  color: $learn-blue;
  min-width: 56px;
  min-height: 40px;
  padding: 8px 14px;
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
  gap: 14px;
  width: 100%;
  max-width: 680px;
}

.question-speech {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 18px;
  border-radius: $radius-xl;
  background: #F0F7FF;
  transition: background 0.3s ease;

  &.speaking {
    background: #DBEAFE;
  }
}
.question-emoji { font-size: 74px; }

.sound-wave {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 28px;
}
.sound-bar {
  width: 4px;
  border-radius: 2px;
  background: $learn-blue;
  animation: soundPulse 0.6s ease-in-out infinite alternate;

  &:nth-child(1) { height: 8px; animation-delay: 0s; }
  &:nth-child(2) { height: 16px; animation-delay: 0.15s; }
  &:nth-child(3) { height: 22px; animation-delay: 0.3s; }
  &:nth-child(4) { height: 12px; animation-delay: 0.45s; }
}

@keyframes soundPulse {
  0% { transform: scaleY(0.4); opacity: 0.5; }
  100% { transform: scaleY(1); opacity: 1; }
}
.speech-hint { font-size: 13px; color: $learn-blue; font-weight: 800; }
.question-count {
  min-height: 28px;
  padding: 4px 12px;
  border-radius: 14px;
  background: #EEF6FF;
  color: $learn-blue;
  font-size: 13px;
  font-weight: 800;
}
.question-text { text-align: center; line-height: 1.35; }

.options-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  width: 100%;
}

.option-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 78px;
  padding: 18px 20px;
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

  &.eliminated {
    opacity: 0.3;
    pointer-events: none;
    background: #F5F5F5;
    border-color: #E0E0E0;
  }
}

.option-label {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #F5F5F5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: $text;
  text-align: center;
  line-height: 34px;
}

.option-text {
  flex: 1;
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: $text;
  line-height: 1.35;
}

.fill-panel {
  width: min(640px, 100%);
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: center;
}

.fill-input {
  width: 100%;
  height: 56px;
  border: 2px solid #E8F0FE;
  border-radius: $radius-md;
  padding: 0 16px;
  font-size: 18px;
  text-align: center;

  &:focus {
    border-color: $learn-blue;
    background: #F7FBFF;
  }
}

.order-panel,
.voice-panel {
  width: min(640px, 100%);
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-row {
  min-height: 58px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: $radius;
  background: #F7FBFF;
}

.order-index {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #DBEAFE;
  color: $learn-blue;
  font-weight: 800;
}

.order-text {
  flex: 1;
  font-size: 17px;
  font-weight: 800;
  color: $text;
}

.order-actions {
  display: flex;
  gap: 6px;
}

.mini-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: $white;
  color: $learn-blue;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  box-shadow: $shadow-sm;

  &.disabled {
    opacity: 0.3;
    pointer-events: none;
  }
}

.match-panel {
  width: min(760px, 100%);
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.match-column {
  min-height: 220px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.match-item {
  min-height: 52px;
  padding: 10px 12px;
  border: 2px solid #E8F0FE;
  border-radius: $radius;
  background: $white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  cursor: pointer;

  &.active {
    border-color: $learn-blue;
    background: #EEF6FF;
  }

  &.paired {
    border-color: $success;
    background: #F0FFF5;
  }
}

.match-label {
  font-size: 16px;
  font-weight: 800;
  color: $text;
}

.match-submit {
  grid-column: 1 / -1;
}

.match-pair {
  font-size: 13px;
  color: $success;
  font-weight: 800;
}

.voice-target {
  text-align: center;
  font-size: 28px;
  font-weight: 900;
  color: $text;
  line-height: 1.3;
}

.voice-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.voice-attempt {
  text-align: center;
  font-size: 15px;
  color: $text-secondary;
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

.challenge-result {
  width: 360px;
  padding: 14px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.challenge-result-main {
  display: flex;
  align-items: center;
  gap: 10px;
}

.challenge-result-icon { font-size: 30px; }

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

.feedback-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.pet-exp-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 14px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.3);
  color: #FFD700;
  font-size: 18px;
  font-weight: 800;
  text-shadow: 0 1px 4px rgba(0,0,0,0.2);
}

/* ===== 金币飞入粒子 ===== */
.coin-particles {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.coin-particle {
  position: absolute;
  font-size: 20px;
}

/* ===== 得分飞入 ===== */
.score-fly-up {
  position: absolute;
  bottom: 30%;
  font-size: 28px;
  font-weight: 800;
  animation: score-fly-up 1.2s ease-out forwards;

  &.positive {
    color: $gold;
    text-shadow: 0 2px 10px rgba(255, 215, 0, 0.5);
  }
}

@keyframes score-fly-up {
  0% { transform: translateY(0) scale(0.5); opacity: 0; }
  30% { transform: translateY(-30px) scale(1.2); opacity: 1; }
  100% { transform: translateY(-100px) scale(1); opacity: 0; }
}

/* ===== 宝箱动画 ===== */
.treasure-chest {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chest-emoji {
  font-size: 90px;
  display: block;
}

.chest-coins {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.chest-coin {
  position: absolute;
  font-size: 22px;
}

@media (max-width: 640px) {
  .screen { padding: 16px; }
  .start-emoji { font-size: 76px; }
  .info-row { gap: 8px; }
  .info-item { padding: 8px 10px; }
  .quiz-screen { padding-top: 10px; }
  .hint-btn { min-width: 48px; font-size: 12px; padding: 4px 8px; }
  .question-emoji { font-size: 58px; }
  .question-text { font-size: 22px; }
  .options-grid { grid-template-columns: 1fr; gap: 10px; }
  .option-btn { min-height: 64px; }
  .match-panel { grid-template-columns: 1fr; }
  .voice-actions { flex-direction: column; }
  .result-stats { width: 100%; }
  .challenge-result { width: 100%; }
  .result-actions { width: 100%; flex-direction: column; }
}
</style>
