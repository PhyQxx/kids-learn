<template>
  <AppLayout theme="kids" :show-topbar="false">
    <view class="quiz-container" :class="{ 'has-bottom-nav': isPractice && screen === 'quiz' }">
      <!-- 开始屏 -->
      <view v-if="screen === 'start'" class="screen start-screen">
        <image class="start-world-art" src="/static/redesign/mission-island.png" mode="aspectFill" />
        <text class="start-title text-title text-bold">{{ levelName }}</text>
        <text class="start-subtitle text-light">准备好了吗？</text>
        <text v-if="!totalQuestions" class="start-tip">题目正在准备中，请稍等一下</text>
        <view class="info-row">
          <view class="info-item card">
            <text class="info-kind">题量</text>
            <text class="info-text text-sm">{{ totalQuestions }} 题</text>
          </view>
          <view class="info-item card" v-if="!isPractice">
            <text class="info-kind">目标</text>
            <text class="info-text text-sm">目标 3 星</text>
          </view>
          <view class="info-item card" v-if="timeLimit > 0">
            <text class="info-kind">限时</text>
            <text class="info-text text-sm">{{ timeLimit }} 秒</text>
          </view>
          <view class="info-item card" v-else-if="isPractice">
            <text class="info-kind">模式</text>
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
            <text>退出</text>
          </view>
          <view class="quiz-progress">
            <tn-line-progress :percent="(currentIndex + 1) / totalQuestions * 100" active-color="#4A90D9" :height="10" :show-percent="false" />
          </view>
          <view class="hint-btn" :class="{ used: hintUsed }" @tap="useHint">
            <text>{{ hintUsed ? '提示已用' : '查看提示' }}</text>
          </view>
          <view class="timer" :class="{ warning: timeLimit > 0 && countdown <= 10 }">
            <text v-if="timeLimit > 0">{{ countdown }}s</text>
            <text v-else>{{ usedTimeInQuiz }}s</text>
          </view>
        </view>

        <!-- 题目区域 -->
        <view class="question-area">
          <view class="question-speech" :class="{ speaking: isSpeaking }" @tap="questionToSpeech()">
            <text class="question-audio-label">听题</text>
            <view class="sound-wave" :class="{ silent: !isSpeaking }">
              <template v-if="isSpeaking">
                <view class="sound-bar" v-for="i in 4" :key="i"></view>
              </template>
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
          <view v-else-if="currentQuestion.interactionType === 'order'" class="order-panel-wrapper">
            <SortableList
              :items="orderItems"
              :disabled="!!selectedAnswer"
              @update:items="orderItems = $event"
            />
            <tn-button class="order-submit-btn" type="primary" shape="round" size="lg" :disabled="!!selectedAnswer" @click="submitOrderAnswer">提交排序</tn-button>
          </view>

          <!-- 连线题 -->
          <view v-else-if="currentQuestion.interactionType === 'match'" class="match-panel-wrapper">
            <MatchPanel
              ref="matchPanelRef"
              :options="currentQuestion.options"
              :disabled="!!selectedAnswer"
              @pair-change="onMatchPairChange"
            />
            <tn-button class="match-submit-btn" type="primary" shape="round" size="lg" :disabled="!canSubmitMatch || !!selectedAnswer" @click="submitMatchAnswer">提交连线</tn-button>
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

          <!-- 练习模式：解析卡片 -->
          <view v-if="isPractice && showAnalysis" class="analysis-card" :class="userAnswers[currentIndex]?.isCorrect ? 'analysis-correct' : 'analysis-wrong'">
            <view class="analysis-header">
              <text class="analysis-icon">{{ userAnswers[currentIndex]?.isCorrect ? '正确' : '复盘' }}</text>
              <text class="analysis-title text-bold">{{ userAnswers[currentIndex]?.isCorrect ? '回答正确！' : '回答错误' }}</text>
            </view>
            <view v-if="currentAnalysisText" class="analysis-body">
              <text class="analysis-label">题目解析</text>
              <text class="analysis-text">{{ currentAnalysisText }}</text>
            </view>
          </view>
        </view>

        <!-- 练习模式：底部导航栏 -->
        <view v-if="isPractice" class="quiz-bottom-nav">
          <view class="nav-btn" :class="{ disabled: currentIndex <= 0 }" @tap="prevQuestion">
            <text class="nav-arrow">‹</text>
            <text class="nav-text">上一题</text>
          </view>
          <view class="nav-center" @tap="showQuestionCardPopup = true">
            <text class="nav-counter">{{ currentIndex + 1 }} / {{ totalQuestions }}</text>
          </view>
          <view class="nav-btn" @tap="nextQuestion">
            <text class="nav-text">{{ currentIndex >= totalQuestions - 1 ? '完成' : '下一题' }}</text>
            <text class="nav-arrow">›</text>
          </view>
        </view>

        <!-- 练习模式：答题卡弹窗（驾考宝典风格题号网格） -->
        <view v-if="isPractice && showQuestionCardPopup" class="question-card-mask" @tap="showQuestionCardPopup = false">
          <view class="question-card-popup" @tap.stop>
            <view class="card-popup-header">
              <text class="card-popup-title">答题卡</text>
              <text class="card-popup-close" @tap="showQuestionCardPopup = false">✕</text>
            </view>
            <view class="card-popup-stats">
              <view class="qs-item"><view class="qs-dot answered-correct"></view><text>答对 {{ practiceCorrectCount }}</text></view>
              <view class="qs-item"><view class="qs-dot answered-wrong"></view><text>答错 {{ practiceWrongCount }}</text></view>
              <view class="qs-item"><view class="qs-dot unanswered"></view><text>未做 {{ totalQuestions - practiceAnsweredCount }}</text></view>
            </view>
            <scroll-view scroll-y class="card-grid-scroll">
              <view class="card-grid">
                <view
                  v-for="(q, idx) in questions"
                  :key="q.id || idx"
                  class="grid-cell"
                  :class="getGridCellClass(idx)"
                  @tap="goToQuestion(idx)"
                >
                  <text>{{ idx + 1 }}</text>
                </view>
              </view>
            </scroll-view>
            <view class="card-popup-footer">
              <tn-button type="primary" size="lg" shape="round" block @click="finishQuizFromCard">
                交卷（{{ practiceAnsweredCount }}/{{ totalQuestions }}）
              </tn-button>
            </view>
          </view>
        </view>
      </view>

      <!-- 结果屏 -->
      <view v-if="screen === 'result'" class="screen result-screen">
        <text class="result-emoji">{{ resultEmoji }}</text>

        <!-- 星级 (闯关时显示) -->
        <view class="result-stars" v-if="!isPractice">
          <text
            v-for="i in 3"
            :key="i"
            class="result-star"
            :class="{ filled: i <= earnedStars, animate: showRewardAnimation }"
            :style="{ animationDelay: (0.2 * i) + 's' }"
          >★</text>
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
            <text class="reward-emoji">🌟</text>
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
            <text class="challenge-result-icon">{{ challengeResult.settled ? (challengeResult.isWin ? '🏆' : '🤝') : '⏳' }}</text>
            <view>
              <text class="text-md text-bold">{{ challengeResult.settled ? (challengeResult.isWin ? '挑战获胜' : '挑战完成') : '成绩已锁定' }}</text>
              <text class="text-xs text-light">
                {{ challengeResult.settled
                  ? `对手 ${challengeResult.opponentScore} 分 · 段位 ${challengeResult.rankDelta >= 0 ? '+' : ''}${challengeResult.rankDelta}`
                  : '等待对手完成挑战' }}
              </text>
            </view>
          </view>
          <text v-if="challengeResult.settled" class="text-sm text-bold text-primary">+{{ challengeResult.rewardGold }} 🪙</text>
        </view>

        <view class="result-actions">
          <tn-button type="primary" size="lg" shape="round" @click="goNextLevel" style="background: linear-gradient(135deg, #4A90D9, #6BA3E0);">{{ isPractice ? '再来一次' : '下一关' }}</tn-button>
          <tn-button size="lg" shape="round" @click="goBack">{{ isPractice ? '结束练习' : '返回' }}</tn-button>
        </view>
      </view>

      <!-- 答对反馈 -->
      <view v-if="showCorrect" class="feedback-overlay correct-overlay">
        <view class="feedback-content animate-pop-in">
          <text class="feedback-text text-title text-bold text-white">回答正确</text>
          <text v-if="petExpGained" class="pet-exp-badge animate-pop-in">伙伴经验 +{{ petExpGained }}</text>
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
        <view class="wrong-feedback-card animate-shake">
          <text class="feedback-text text-title text-bold text-white">再想一想</text>
          <text v-if="wrongAiLoading" class="wrong-ai-text">AI老师正在讲一讲...</text>
          <text v-else-if="wrongAiExplanation" class="wrong-ai-text">{{ wrongAiExplanation }}</text>
        </view>
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { onBackPress } from '@dcloudio/uni-app'
import AppLayout from '@/components/AppLayout.vue'
import RewardOverlay from '@/components/common/RewardOverlay.vue'
import AnimatedNumber from '@/components/common/AnimatedNumber.vue'
import SortableList from '@/components/quiz/SortableList.vue'
import MatchPanel from '@/components/quiz/MatchPanel.vue'
import { soundManager } from '@/utils/sound'
import { useLearnStore } from '@/store/learn'
import { useUserStore } from '@/store/user'
import { usePetStore } from '@/store/pet'
import { getQuestions, submitAnswer, completeLevel, getHint, startPractice, resumePractice, submitPracticeAnswer, completePracticeSession, getExplainWrong } from '@/api/learn'
import { getChallengeQuestions, submitChallengeAnswer, finishChallenge } from '@/api/challenge'
import { getUserInfo } from '@/api/user'
import { getFeedbackAudioConfig } from '@/api/app'
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

// 反馈语音配置（从 API 加载，默认值与之前一致）
const FEEDBACK_AUDIO_BASE_DEFAULT = 'https://ftp.pnkx.top:8/ftp/kids-learn/question/audio/seed/feedback'
let feedbackBaseUrl = FEEDBACK_AUDIO_BASE_DEFAULT
let correctFeedbacks = ['correct-1', 'correct-2', 'correct-3', 'correct-4', 'correct-5']
let wrongFeedbacks = ['wrong-1', 'wrong-2', 'wrong-3']
let lastCorrectIdx = -1
let lastWrongIdx = -1
let feedbackAudio = null

async function loadFeedbackAudioConfig() {
  try {
    const res = await getFeedbackAudioConfig()
    if (res) {
      if (res.baseUrl) feedbackBaseUrl = res.baseUrl.replace(/\/+$/, '')
      if (res.correctList) correctFeedbacks = res.correctList.split(',').map(s => s.trim()).filter(Boolean)
      if (res.wrongList) wrongFeedbacks = res.wrongList.split(',').map(s => s.trim()).filter(Boolean)
    }
  } catch (e) {
    // 使用默认值
  }
}

function feedbackAudioUrl(name) {
  return `${feedbackBaseUrl}/${name}.wav`
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
  correctFeedbacks.forEach(preload)
  wrongFeedbacks.forEach(preload)
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
  const list = type === 'correct' ? correctFeedbacks : wrongFeedbacks
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
const matchPanelRef = ref(null)
const showCorrect = ref(false)
const showWrong = ref(false)
const wrongAiLoading = ref(false)
const wrongAiExplanation = ref('')
const countdown = ref(60)
const timeLimit = ref(60)
const usedTimeInQuiz = ref(0)
let timer = null

// 练习模式：存储每题答案 { [idx]: { answer, displayAnswer, isCorrect, correctAnswer, analysis } }
const userAnswers = ref({})
// 练习模式：当前题是否已显示解析
const showAnalysis = ref(false)
// 练习模式：当前题解析文本
const currentAnalysisText = ref('')
// 练习模式：答题卡弹窗
const showQuestionCardPopup = ref(false)

const levelId = ref(null)
const pageGradeLevelId = ref(null)
const challengeId = ref(null)
const challengeAnswerPromises = []
// 退出兜底结算进行中标志，防止 onBackPress 与 exitQuiz 重复触发
const isExiting = ref(false)
const isPractice = ref(false)
const practiceModeId = ref(null)
// 练习模式：后端会话ID（断点续做核心）
const practiceSessionId = ref(null)
// 练习模式：题目顺序快照中的每题ID（用于答题卡网格定位）
const practiceQuestionIds = ref([])
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
  // 加载反馈语音配置（异步，不阻塞页面渲染）
  loadFeedbackAudioConfig()

  const pages = getCurrentPages()
  const page = pages[pages.length - 1]
  const options = page.$page?.options || {}

  // 默认无倒计时，仅练习模式按 options.timeLimit 开启，避免闯关/关卡模式继承默认 60s 被强制结束
  timeLimit.value = 0

  if (options.practiceModeId) {
    isPractice.value = true
    practiceModeId.value = options.practiceModeId
    timeLimit.value = parseInt(options.timeLimit) || 0
    try {
      let res
      if (options.sessionId) {
        // 断点续做：恢复题目顺序 + 已答记录 + 当前进度
        res = await resumePractice(options.sessionId)
        practiceSessionId.value = res.sessionId || options.sessionId
        // 从后端返回的 userRecords 还原每题答案状态
        if (res.userRecords) {
          const restored = {}
          res.questions.forEach((q, idx) => {
            const r = res.userRecords[q.id]
            if (r) {
              restored[idx] = {
                answer: r.userAnswer || '',
                displayAnswer: r.userAnswer || '',
                isCorrect: r.isCorrect === 1 || r.isCorrect === true,
                correctAnswer: '',
                analysis: q.analysisText || ''
              }
            }
          })
          userAnswers.value = restored
        }
        currentIndex.value = res.currentIndex || 0
      } else {
        // 新建会话
        res = await startPractice(practiceModeId.value)
        practiceSessionId.value = res.sessionId || res.practiceSessionId
      }
      levelName.value = res.modeName || '专项练习'
      levelEmoji.value = '📝'
      if (res.questions && Array.isArray(res.questions) && res.questions.length > 0) {
        questions.value = res.questions.map(q => normalizeQuizQuestion(q))
        practiceQuestionIds.value = questions.value.map(q => q.id)
        // 同步已有统计（续做场景）
        if (typeof res.correctCount === 'number') correctCount.value = res.correctCount
        totalScore.value = Object.values(userAnswers.value).filter(a => a.isCorrect).length * 10
        if (questions.value[currentIndex.value]) {
          preloadQuestionAudio(questions.value[currentIndex.value])
          resetInteractionState()
          restoreQuestionState()
        }
      } else {
        uni.showToast({ title: '该练习下没有题目', icon: 'none' })
      }
    } catch (e) {
      uni.showToast({ title: e?.message || '加载练习失败', icon: 'none' })
    }
  } else {
    challengeId.value = options.challengeMatchId || null
    levelId.value = options.levelId || learnStore.currentLevel?.id
    pageGradeLevelId.value = options.gradeLevelId || userStore.userInfo?.gradeLevelId || null
    if (challengeId.value) {
      try {
        const res = await getChallengeQuestions(Number(challengeId.value))
        questions.value = Array.isArray(res) ? res.map(q => normalizeQuizQuestion(q)) : []
        levelName.value = '挑战赛'
        levelEmoji.value = '🏆'
        // 闯关总倒计时：每题平均 30 秒
        if (questions.value.length > 0) {
          timeLimit.value = questions.value.length * 30
        }
        if (questions.value[0]) {
          preloadQuestionAudio(questions.value[0])
          resetInteractionState()
        }
      } catch (e) {
        uni.showToast({ title: e?.message || '挑战题目加载失败', icon: 'none' })
      }
    } else if (levelId.value) {
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
        questions.value = []
        uni.showToast({ title: '题目加载失败，请稍后重试', icon: 'none' })
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

// 练习模式：答题卡统计（基于 userAnswers 动态计算，避免与 correctCount 不同步）
const practiceAnsweredCount = computed(() => Object.keys(userAnswers.value).length)
const practiceCorrectCount = computed(() => Object.values(userAnswers.value).filter(a => a.isCorrect).length)
const practiceWrongCount = computed(() => Object.values(userAnswers.value).filter(a => !a.isCorrect).length)

// 练习模式：答题卡单元格状态 class
function getGridCellClass(idx) {
  if (idx === currentIndex.value) return 'cell-current'
  const saved = userAnswers.value[idx]
  if (!saved) return 'cell-unanswered'
  return saved.isCorrect ? 'cell-correct' : 'cell-wrong'
}

// 练习模式：从答题卡交卷
function finishQuizFromCard() {
  showQuestionCardPopup.value = false
  finishQuiz()
}
const matchedRightValues = computed(() => new Set(Object.values(matchPairs.value)))
const canSubmitMatch = computed(() =>
  currentQuestion.value.options.length > 0
  && Object.keys(matchPairs.value).length === currentQuestion.value.options.length
)

// 连线题配对变化回调
function onMatchPairChange(newPairs) {
  matchPairs.value = newPairs
}

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
  if (isPractice.value) {
    // 练习模式：显示对错
    if (opt.correct) return 'correct'
    if ((opt.label === selectedAnswer.value || opt.answerValue === selectedAnswer.value) && !opt.correct) return 'wrong'
    return ''
  } else {
    // 闯关模式：仅标记已选，不显示对错
    if (opt.label === selectedAnswer.value || opt.answerValue === selectedAnswer.value) return 'selected'
    return ''
  }
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
      emoji: isStar ? '星' : '币',
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
  // 重置 MatchPanel 组件
  if (matchPanelRef.value && q.interactionType === 'match') {
    matchPanelRef.value.reset()
  }
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
  if (isPractice.value) {
    // 练习模式：已提交则锁定，未提交则提交
    if (userAnswers.value[currentIndex.value]) return
    submitCurrentAnswer(opt.answerValue || opt.label, opt.label)
  } else {
    // 闯关模式：选择后立即自动跳转（不等待接口）
    if (selectedAnswer.value) return
    const answer = opt.answerValue || opt.label
    selectedAnswer.value = opt.label
    // 异步提交，不等待结果
    const q = currentQuestion.value
    const answerTime = Math.round((Date.now() - (questionStartTime.value || startTime.value)) / 1000)
    if (q.id) {
      const request = challengeId.value
        ? submitChallengeAnswer(Number(challengeId.value), { snapshotId: Number(q.id), answer, durationMs: answerTime * 1000 })
        : submitAnswer({ questionId: q.id, answer, answerTime })
      if (challengeId.value) challengeAnswerPromises.push(request)
      request.then(res => {
        if (res?.correct) {
          correctCount.value++
          totalScore.value += (q.score || 10)
          if (res?.petExp) {
            petExpGained.value = res.petExp
            showPetExp.value = true
          }
        }
      }).catch(() => {})
    }
    // 立即跳下一题
    setTimeout(() => {
      showPetExp.value = false
      nextQuestionAuto()
    }, 300)
  }
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

  if (q.id) {
    // 练习模式走专门的会话接口（同步 user_question_record + 推进 session 进度）
    const submitPromise = challengeId.value
      ? submitChallengeAnswer(Number(challengeId.value), { snapshotId: Number(q.id), answer, durationMs: answerTime * 1000 })
      : isPractice.value && practiceSessionId.value
        ? submitPracticeAnswer(practiceSessionId.value, { questionId: q.id, answer, answerTime })
        : submitAnswer({ questionId: q.id, answer, answerTime })
    if (challengeId.value) challengeAnswerPromises.push(submitPromise)
    submitPromise.then(res => {
      const isCorrect = res?.correct || false
      const correctAnswer = res?.correctAnswer || ''
      const analysis = res?.explanationText || ''

      if (isPractice.value) {
        // 练习模式：存储答案，显示对错+解析，不自动跳转
        // 注意：同一题重新作答时先扣除旧统计再加新统计，避免重复计数
        const old = userAnswers.value[currentIndex.value]
        if (old) {
          if (old.isCorrect) {
            correctCount.value = Math.max(0, correctCount.value - 1)
            totalScore.value = Math.max(0, totalScore.value - (q.score || 10))
          }
        }
        userAnswers.value[currentIndex.value] = { answer, displayAnswer, isCorrect, correctAnswer, analysis }
        if (isCorrect) {
          correctCount.value++
          totalScore.value += (q.score || 10)
          showCorrect.value = true
          try { uni.vibrateShort() } catch (_) {}
          try { soundManager.play('success') } catch (_) {}
          try { playFeedbackAudio('correct') } catch (_) {}
          generateCoinParticles(8)
          if (res?.petExp) showPetExpGain(res.petExp)
          // 答对反馈短暂展示后自动关闭（解析卡片保留供查看）
          setTimeout(() => { showCorrect.value = false }, 1500)
        } else {
          showWrong.value = true
          wrongAiExplanation.value = ''
          wrongAiLoading.value = true
          try { uni.vibrateLong() } catch (_) {}
          try { soundManager.play('fail') } catch (_) {}
          try { playFeedbackAudio('wrong') } catch (_) {}
          loadWrongAiExplanation(q.id, analysis)
          if (correctAnswer) {
            const cOpt = q.options.find(o => (o.answerValue || o.label) === correctAnswer)
            if (cOpt) cOpt.correct = true
          }
          // 答错反馈展示一段时间后自动关闭（解析卡片保留供查看）
          setTimeout(() => { showWrong.value = false }, 2500)
        }
        // 显示解析
        currentAnalysisText.value = analysis
        showAnalysis.value = true
      } else {
        // 闯关模式（非选项题型：填空/排序/连线/语音）
        if (isCorrect) {
          correctCount.value++
          totalScore.value += (q.score || 10)
          showCorrect.value = true
          try { uni.vibrateShort() } catch (_) {}
          try { soundManager.play('success') } catch (_) {}
          try { playFeedbackAudio('correct') } catch (_) {}
          generateCoinParticles(8)
          if (res?.petExp) showPetExpGain(res.petExp)
        } else {
          showWrong.value = true
          wrongAiExplanation.value = ''
          wrongAiLoading.value = true
          try { uni.vibrateLong() } catch (_) {}
          try { soundManager.play('fail') } catch (_) {}
          try { playFeedbackAudio('wrong') } catch (_) {}
          loadWrongAiExplanation(q.id, analysis)
          if (correctAnswer) {
            const cOpt = q.options.find(o => (o.answerValue || o.label) === correctAnswer)
            if (cOpt) cOpt.correct = true
          }
        }
        setTimeout(() => {
          showCorrect.value = false
          showWrong.value = false
          wrongAiLoading.value = false
          nextQuestionAuto()
        }, isCorrect ? 2500 : 6500)
      }
    }).catch(() => {
      if (isPractice.value) {
        userAnswers.value[currentIndex.value] = { answer, displayAnswer, isCorrect: false, correctAnswer: '', analysis: '' }
        showWrong.value = true
        currentAnalysisText.value = ''
        showAnalysis.value = true
        try { playFeedbackAudio('wrong') } catch (_) {}
        setTimeout(() => { showWrong.value = false }, 2500)
      } else {
        showWrong.value = true
        try { playFeedbackAudio('wrong') } catch (_) {}
        setTimeout(() => { showWrong.value = false; nextQuestionAuto() }, 3000)
      }
    })
  } else {
    // 无 questionId fallback
    if (isPractice.value) {
      userAnswers.value[currentIndex.value] = { answer, displayAnswer, isCorrect: false, correctAnswer: '', analysis: '' }
      showWrong.value = true
      showAnalysis.value = true
    } else {
      showWrong.value = true
      setTimeout(() => { showWrong.value = false; nextQuestionAuto() }, 1000)
    }
  }
}

// 闯关模式：自动跳转下一题（不等接口结果）
function nextQuestionAuto() {
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

// 练习模式：下一题（手动）
function nextQuestion() {
  if (currentIndex.value >= questions.value.length - 1) {
    finishQuiz()
    return
  }
  currentIndex.value++
  restoreQuestionState()
}

// 练习模式：上一题（手动）
function prevQuestion() {
  if (currentIndex.value <= 0) return
  currentIndex.value--
  restoreQuestionState()
}

// 练习模式：从答题卡跳转到指定题
function goToQuestion(idx) {
  if (idx < 0 || idx >= totalQuestions.value) return
  currentIndex.value = idx
  showQuestionCardPopup.value = false
  restoreQuestionState()
}

// 练习模式：切换题目后恢复已答状态
function restoreQuestionState() {
  showAnalysis.value = false
  currentAnalysisText.value = ''
  showCorrect.value = false
  showWrong.value = false
  wrongAiExplanation.value = ''
  wrongAiLoading.value = false
  eliminatedOptions.value = new Set()
  const saved = userAnswers.value[currentIndex.value]
  if (saved) {
    selectedAnswer.value = saved.displayAnswer
    showAnalysis.value = true
    currentAnalysisText.value = saved.analysis || ''
    // 恢复正确答案标记
    const q = currentQuestion.value
    if (saved.correctAnswer && q.options) {
      const cOpt = q.options.find(o => (o.answerValue || o.label) === saved.correctAnswer)
      if (cOpt) cOpt.correct = true
    }
  } else {
    selectedAnswer.value = ''
    resetInteractionState()
  }
  questionStartTime.value = Date.now()
  preloadQuestionAudio(questions.value[currentIndex.value])
  setTimeout(() => questionToSpeech(), 250)
}

async function loadWrongAiExplanation(questionId, fallbackText = '') {
  if (!questionId) {
    wrongAiExplanation.value = fallbackText || ''
    wrongAiLoading.value = false
    return
  }
  try {
    const res = await getExplainWrong(questionId)
    wrongAiExplanation.value = res?.aiExplanation || res?.analysisText || fallbackText || ''
  } catch (e) {
    wrongAiExplanation.value = fallbackText || ''
  } finally {
    wrongAiLoading.value = false
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
  if (challengeAnswerPromises.length) {
    await Promise.allSettled(challengeAnswerPromises)
  }
  usedTime.value = Math.round((Date.now() - startTime.value) / 1000)

  // Calculate stars
  const acc = totalQuestions.value > 0 ? correctCount.value / totalQuestions.value : 0
  if (acc >= 0.9) earnedStars.value = 3
  else if (acc >= 0.7) earnedStars.value = 2
  else if (acc >= 0.5) earnedStars.value = 1
  else earnedStars.value = 0

  const wrongCount = totalQuestions.value - correctCount.value

  // 提交关卡完成 (仅闯关模式)
  if (levelId.value && !isPractice.value && !challengeId.value) {
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
    // 通知后端完成会话（置为 COMPLETED，下次进入会新建会话而非续做）
    if (practiceSessionId.value) {
      try {
        await completePracticeSession(practiceSessionId.value)
      } catch (e) {
        console.log('完成练习会话失败', e)
      }
    }
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
    const res = await finishChallenge(Number(challengeId.value))
    if (!res) return
    const settled = !!res.settled
    challengeResult.value = {
      settled,
      isWin: !!res.isWin,
      myScore: res.myScore,
      opponentScore: res.opponentScore,
      rankDelta: res.rankDelta || 0,
      rewardGold: res.rewardGold || 0
    }
    if (settled) {
      rewards.value = {
        ...rewards.value,
        gold: rewards.value.gold + challengeResult.value.rewardGold
      }
    }
    try {
      const userInfo = await getUserInfo()
      if (userInfo) userStore.setUserInfo(userInfo)
    } catch (e) {
      console.log('挑战奖励后更新用户信息失败:', e)
    }
    if (!settled) {
      uni.showToast({ title: '成绩已锁定，等待对手完成', icon: 'none' })
    }
  } catch (e) {
    console.log('挑战结算失败:', e)
    uni.showToast({ title: 'PK结算稍后同步', icon: 'none' })
  }
}

async function exitQuiz() {
  // 防止 onBackPress 与点击退出按钮重复触发
  if (isExiting.value) return
  // 未进入答题屏（仍在开始页）或闯关模式：无需兜底结算，直接退出
  const needSettle = screen.value === 'quiz' && (challengeId.value || practiceSessionId.value)
  if (!needSettle) {
    clearInterval(timer)
    stopQuestionSpeech()
    stopFeedbackAudio()
    uni.navigateBack()
    return
  }
  const tip = challengeId.value
    ? '退出后将按已答内容结算挑战成绩，未答题目按 0 分计。'
    : '退出后将保存本次练习进度。'
  uni.showModal({
    title: '确定退出？',
    content: tip,
    confirmText: '退出',
    cancelText: '继续答题',
    success: async (res) => {
      if (!res.confirm) return
      isExiting.value = true
      clearInterval(timer)
      stopQuestionSpeech()
      stopFeedbackAudio()
      try {
        // 兜底结算：挑战按已答内容 finish；练习完成会话保存进度
        if (challengeId.value) {
          await submitChallengeIfNeeded()
        } else if (practiceSessionId.value) {
          try {
            await completePracticeSession(practiceSessionId.value)
          } catch (e) {
            console.log('退出时完成练习会话失败', e)
          }
        }
      } catch (e) {
        console.log('退出结算失败', e)
      } finally {
        uni.navigateBack()
      }
    }
  })
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

// 拦截系统返回键（App 端硬件返回 / 手势返回），走 exitQuiz 的二次确认 + 兜底结算流程
onBackPress(() => {
  exitQuiz()
  return true
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.quiz-container {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;

  // 练习模式下强制撑满，确保底部导航固定
  &.has-bottom-nav {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
  }
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
  align-items: stretch;
  padding: 16px 0 0;
  overflow: hidden;
}

.quiz-topbar {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
  padding: 0 28px;
  flex-shrink: 0;
  box-sizing: border-box;
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
  margin: 0 auto;
  overflow-y: auto;
  padding: 12px 28px 20px;
  box-sizing: border-box;
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

  &.silent {
    gap: 0;
  }
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

  &.selected {
    background: #E8F0FE;
    border-color: $learn-blue;
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

.order-panel-wrapper {
  width: min(640px, 100%);
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-submit-btn {
  margin-top: 4px;
}

.match-panel-wrapper {
  width: min(760px, 100%);
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: relative;
}

.match-submit-btn {
  margin-top: 4px;
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
  gap: 8px;
  justify-content: center;
  padding: 16px 20px;
  overflow: hidden;
}

.result-emoji { font-size: 44px; line-height: 1; display: block; text-align: center; }

.result-stars {
  display: flex;
  gap: 6px;
  margin-top: -2px;
}

.result-star {
  font-size: 28px;
  color: #E0E0E0;
  transition: all 0.3s ease;

  &.filled { color: $gold; }
  &.animate { animation: popIn 0.5s ease backwards; }
}

.reward-row {
  display: flex;
  gap: 10px;
  margin: 2px 0;
  width: 100%;
  max-width: 320px;
}

.reward-card {
  flex: 1;
  padding: 8px 6px;
  text-align: center;
  min-width: 0;
}

.reward-emoji { font-size: 20px; display: block; margin-bottom: 2px; line-height: 1; }
.reward-value { display: block; }
.reward-label { display: block; margin-top: 2px; }

.result-stats {
  width: 100%;
  max-width: 320px;
  padding: 10px 14px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;

  & + .stat-row { border-top: 1px solid #F5F5F5; }
}

.challenge-result {
  width: 100%;
  max-width: 320px;
  padding: 10px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.challenge-result-main {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.challenge-result-icon { font-size: 24px; line-height: 1; }

.result-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 2px;
  width: 100%;
  max-width: 320px;
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

.wrong-feedback-card {
  width: min(560px, 88%);
  padding: 18px 22px;
  border-radius: $radius-md;
  background: rgba(231, 76, 60, 0.9);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  text-align: center;
  box-shadow: 0 12px 30px rgba(231, 76, 60, 0.24);
}

.wrong-ai-text {
  color: $white;
  font-size: 15px;
  line-height: 1.55;
  font-weight: 700;
}

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

/* ===== 练习模式：解析卡片 ===== */
.analysis-card {
  margin-top: 16px;
  padding: 16px;
  border-radius: $radius-md;
  background: #F8FFF8;
  border: 2px solid #D4EDDA;
  animation: slideUp 0.3s ease;

  &.analysis-wrong {
    background: #FFF8F8;
    border-color: #F5C6CB;
  }
}

.analysis-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.analysis-icon { font-size: 20px; }
.analysis-title { font-size: 16px; color: $text; }

.analysis-body {
  padding-top: 8px;
  border-top: 1px solid rgba(0,0,0,0.06);
}

.analysis-label {
  font-size: 13px;
  font-weight: 600;
  color: $text-light;
  margin-bottom: 4px;
  display: block;
}

.analysis-text {
  font-size: 14px;
  color: $text;
  line-height: 1.6;
}

/* ===== 练习模式：底部导航栏 ===== */
.quiz-bottom-nav {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 28px;
  padding-bottom: calc(12px + env(safe-area-inset-bottom, 0px));
  background: $white;
  border-top: 1px solid #F0F0F0;
  flex-shrink: 0;
  box-sizing: border-box;
  flex-shrink: 0;
}

.nav-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 10px 18px;
  border-radius: $radius;
  background: #F5F7FA;
  cursor: pointer;
  transition: all 0.2s ease;

  &:active { transform: scale(0.95); }

  &.disabled {
    opacity: 0.3;
    pointer-events: none;
  }
}

.nav-arrow {
  font-size: 20px;
  font-weight: 700;
  color: $learn-blue;
}

.nav-text {
  font-size: 14px;
  font-weight: 600;
  color: $learn-blue;
}

.nav-center {
  padding: 8px 16px;
  border-radius: $radius-sm;
  background: #F0F4FF;
  cursor: pointer;
}

.nav-counter {
  font-size: 14px;
  font-weight: 700;
  color: $learn-blue;
}

/* ===== 练习模式：答题卡弹窗（驾考宝典风格） ===== */
.question-card-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  animation: fade-in 0.2s ease;
}

@keyframes fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

.question-card-popup {
  width: 100%;
  max-width: 480px;
  max-height: 70vh;
  background: $white;
  border-radius: 24px 24px 0 0;
  display: flex;
  flex-direction: column;
  animation: slide-up-card 0.25s ease;
  padding-bottom: env(safe-area-inset-bottom, 0px);
}

@keyframes slide-up-card {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.card-popup-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px 12px;
  border-bottom: 1px solid #F0F0F0;
}

.card-popup-title {
  font-size: 18px;
  font-weight: 700;
  color: $text;
}

.card-popup-close {
  font-size: 20px;
  color: $text-secondary;
  padding: 4px 8px;
  cursor: pointer;
}

.card-popup-stats {
  display: flex;
  gap: 20px;
  padding: 14px 22px;
  flex-wrap: wrap;
}

.qs-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: $text-secondary;
}

.qs-dot {
  width: 14px;
  height: 14px;
  border-radius: 4px;
  flex-shrink: 0;
}

.qs-dot.answered-correct { background: $success; }
.qs-dot.answered-wrong { background: $error; }
.qs-dot.unanswered { background: #E8EAED; }

.card-grid-scroll {
  flex: 1;
  padding: 8px 22px 16px;
  overflow-y: auto;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.grid-cell {
  aspect-ratio: 1;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.15s ease;
  user-select: none;

  &:active { transform: scale(0.92); }
}

/* 未答：灰色边框 */
.grid-cell.cell-unanswered {
  background: #F5F7FA;
  color: $text-secondary;
  border: 1px solid #E8EAED;
}

/* 答对：绿色填充 */
.grid-cell.cell-correct {
  background: $success;
  color: $white;
}

/* 答错：红色填充 */
.grid-cell.cell-wrong {
  background: $error;
  color: $white;
}

/* 当前题：蓝色描边高亮 */
.grid-cell.cell-current {
  background: #F0F4FF;
  color: $learn-blue;
  border: 2px solid $learn-blue;
  box-shadow: 0 0 0 2px rgba(74, 144, 217, 0.2);
}

.card-popup-footer {
  padding: 14px 22px 18px;
  border-top: 1px solid #F0F0F0;
}

@include respond-sm {
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
  .result-actions { flex-direction: column; }
  .card-grid { grid-template-columns: repeat(5, 1fr); gap: 8px; }
  .grid-cell { font-size: 14px; }
}
</style>
