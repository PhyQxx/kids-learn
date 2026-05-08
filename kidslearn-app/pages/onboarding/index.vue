<template>
  <view class="onboarding-page">
    <!-- 步骤指示器 -->
    <view class="step-indicator">
      <view v-for="s in 3" :key="s" class="step-dot" :class="{ active: step >= s, current: step === s }" />
    </view>

    <!-- Step 1: 选择宠物 -->
    <view v-if="step === 1" class="step-content animate-fade-in">
      <text class="step-title animate-bounce-in">选择你的学习伙伴 🌟</text>
      <text class="step-desc">选一个喜欢的宠物，它会陪你一起学习！</text>
      <scroll-view scroll-y class="pet-grid">
        <view class="pet-row">
          <view v-for="pet in pets" :key="pet.id" class="pet-card" :class="{ selected: selectedPetId === pet.id }"
            @tap="selectedPetId = pet.id">
            <text class="pet-emoji">{{ pet.imageUrl }}</text>
            <text class="pet-name">{{ pet.petName }}</text>
          </view>
        </view>
      </scroll-view>
      <view class="btn-next" :class="{ disabled: !selectedPetId }" @tap="confirmPet">
        <text class="text-white text-md">{{ selectedPetId ? '选好啦！' : '请选择一个宠物' }}</text>
      </view>
    </view>

    <!-- Step 2: 测评答题 -->
    <view v-if="step === 2" class="step-content animate-fade-in">
      <text class="step-title">快速小测试 📝</text>
      <text class="step-desc">回答 {{ assessmentTotal }} 道题，帮你找到合适的学习起点</text>

      <view v-if="!quizStarted" class="quiz-intro">
        <text class="quiz-intro-text">测试约需 2 分钟，答错也没关系哦～</text>
        <view class="btn-next" @tap="startQuiz">
          <text class="text-white text-md">开始测试</text>
        </view>
      </view>

      <view v-else class="quiz-area">
        <view class="quiz-progress">
          <view class="quiz-progress-bar">
            <view class="quiz-progress-fill" :style="{ width: quizProgress + '%' }" />
          </view>
          <text class="text-sm text-light">{{ currentIndex + 1 }} / {{ assessmentTotal }}</text>
        </view>

        <view v-if="currentQuestion" class="question-card">
          <text class="question-text">{{ currentQuestion.questionText }}</text>
          <view class="option-list">
            <view v-for="opt in currentQuestion.options" :key="opt.optionLabel"
              class="option-item" :class="{ chosen: chosenLabel === opt.optionLabel }"
              @tap="chooseAnswer(opt)">
              <text class="option-label">{{ opt.optionLabel }}</text>
              <text class="option-text">{{ opt.optionText }}</text>
            </view>
          </view>
          <view v-if="chosenAnswer" class="btn-next" @tap="submitQuizAnswer">
            <text class="text-white text-md">{{ isLastQuestion ? '完成测试' : '下一题' }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- Step 3: 功能导览 -->
    <view v-if="step === 3" class="step-content animate-fade-in">
      <text class="step-title animate-bounce-in">准备就绪！🎉</text>
      <view class="tour-cards">
        <view class="tour-card animate-slide-left">
          <text class="tour-icon">📚</text>
          <text class="tour-title">学习中心</text>
          <text class="tour-desc">选择学科和课程，开始答题闯关</text>
        </view>
        <view class="tour-card animate-slide-left" style="animation-delay:0.1s">
          <text class="tour-icon">🐱</text>
          <text class="tour-title">我的宠物</text>
          <text class="tour-desc">喂食、玩耍、换装，宠物陪你成长</text>
        </view>
        <view class="tour-card animate-slide-left" style="animation-delay:0.2s">
          <text class="tour-icon">🏆</text>
          <text class="tour-title">成就排行</text>
          <text class="tour-desc">完成挑战，赢取金币和徽章</text>
        </view>
      </view>
      <view class="btn-next btn-go" @tap="finishOnboarding">
        <text class="text-white text-lg">开始学习之旅！</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAvailablePets, selectPet, updateOnboardingStep } from '@/api/pet'
import { getAssessmentQuestions, submitAnswer } from '@/api/learn'
import { useUserStore } from '@/store/user'
import { usePetStore } from '@/store/pet'
import { getUserInfo } from '@/api/user'
import {
  getOnboardingPayload,
  normalizePetOptions,
  normalizeQuestionOptions,
  wasAnswerCorrect
} from '@/utils/onboardingData.mjs'

const userStore = useUserStore()
const petStore = usePetStore()

const step = ref(1)

// Step 1: Pet selection
const pets = ref([])
const selectedPetId = ref(null)

// Step 2: Assessment quiz
const quizStarted = ref(false)
const questions = ref([])
const currentIndex = ref(0)
const chosenAnswer = ref('')
const chosenLabel = ref('')
const correctCount = ref(0)
const assessmentTotal = ref(10)

const currentQuestion = computed(() => questions.value[currentIndex.value] || null)
const isLastQuestion = computed(() => currentIndex.value >= questions.value.length - 1)
const quizProgress = computed(() => questions.value.length ? ((currentIndex.value + 1) / questions.value.length) * 100 : 0)

onMounted(async () => {
  const res = await getAvailablePets()
  pets.value = normalizePetOptions(res)
})

async function confirmPet() {
  if (!selectedPetId.value) return
  uni.showLoading({ title: '领养中...' })
  try {
    const res = await selectPet(selectedPetId.value)
    const petInfo = getOnboardingPayload(res)
    if (petInfo) {
      petStore.setPetInfo(petInfo)
      await updateOnboardingStep(1)
      step.value = 2
    }
  } finally {
    uni.hideLoading()
  }
}

async function startQuiz() {
  uni.showLoading({ title: '准备题目...' })
  try {
    const res = await getAssessmentQuestions()
    questions.value = normalizeQuestionOptions(res)
    assessmentTotal.value = questions.value.length
    quizStarted.value = true
  } finally {
    uni.hideLoading()
  }
}

function chooseAnswer(opt) {
  chosenLabel.value = opt.optionLabel
  chosenAnswer.value = opt.answerValue || opt.optionLabel
}

async function submitQuizAnswer() {
  if (!chosenAnswer.value) return
  const q = currentQuestion.value
  try {
    const res = await submitAnswer({
      questionId: q.id,
      answer: chosenAnswer.value,
      timeTaken: 0
    })
    if (wasAnswerCorrect(res)) {
      correctCount.value++
    }
  } catch (_) { /* ignore errors in assessment */ }

  chosenAnswer.value = ''
  chosenLabel.value = ''
  if (isLastQuestion.value) {
    await updateOnboardingStep(2)
    step.value = 3
  } else {
    currentIndex.value++
  }
}

async function finishOnboarding() {
  await updateOnboardingStep(3)
  try {
    const infoRes = await getUserInfo()
    const info = getOnboardingPayload(infoRes)
    if (info) {
      userStore.setUserInfo(info)
    }
  } catch (_) {}
  uni.reLaunch({ url: '/pages/main/index' })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.onboarding-page {
  width: 100vw;
  height: 100vh;
  background: linear-gradient(135deg, #FFF5F5, #F0F7FF, #F5FFF0);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 40px;
  overflow: hidden;
}

.step-indicator {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  margin-top: 8px;
}
.step-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #DDD;
  transition: all 0.3s;
  &.active { background: $primary; }
  &.current { transform: scale(1.3); box-shadow: 0 0 8px rgba($primary, 0.4); }
}

.step-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 800px;
}

.step-title {
  font-size: 28px;
  font-weight: 700;
  color: $text;
  margin-bottom: 8px;
}
.step-desc {
  font-size: 14px;
  color: $text-secondary;
  margin-bottom: 16px;
}

// Pet grid
.pet-grid {
  flex: 1;
  max-height: 55vh;
  width: 100%;
}
.pet-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
  padding: 8px;
}
.pet-card {
  width: 100px;
  height: 110px;
  background: $white;
  border-radius: $radius-lg;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 3px solid transparent;
  transition: all 0.2s;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  &.selected {
    border-color: $primary;
    background: rgba($primary, 0.05);
    transform: scale(1.05);
  }
}
.pet-emoji { font-size: 40px; }
.pet-name { font-size: 12px; color: $text-secondary; }

.btn-next {
  margin-top: 12px;
  padding: 12px 48px;
  background: linear-gradient(135deg, $primary, $primary-light);
  border-radius: 24px;
  &.disabled { opacity: 0.5; pointer-events: none; }
}
.btn-go {
  margin-top: 16px;
  padding: 16px 64px;
  font-size: 18px;
}

// Quiz
.quiz-intro {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
}
.quiz-intro-text { font-size: 16px; color: $text-secondary; }

.quiz-area {
  flex: 1;
  width: 100%;
  display: flex;
  flex-direction: column;
}
.quiz-progress {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.quiz-progress-bar {
  flex: 1;
  height: 8px;
  background: #EEE;
  border-radius: 4px;
  overflow: hidden;
}
.quiz-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, $primary, $primary-light);
  border-radius: 4px;
  transition: width 0.3s;
}

.question-card {
  background: $white;
  border-radius: $radius-xl;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.06);
  flex: 1;
  display: flex;
  flex-direction: column;
}
.question-text {
  font-size: 18px;
  font-weight: 600;
  color: $text;
  margin-bottom: 16px;
  line-height: 1.5;
}
.option-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
}
.option-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: $radius;
  background: #F8F8FA;
  border: 2px solid transparent;
  transition: all 0.2s;
  cursor: pointer;
  &.chosen {
    border-color: $primary;
    background: rgba($primary, 0.06);
  }
}
.option-label {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: $primary;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}
.option-text { font-size: 15px; color: $text; }

// Tour
.tour-cards {
  display: flex;
  gap: 20px;
  margin: 16px 0;
}
.tour-card {
  width: 200px;
  background: $white;
  border-radius: $radius-xl;
  padding: 24px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
}
.tour-icon { font-size: 36px; }
.tour-title { font-size: 16px; font-weight: 700; color: $text; }
.tour-desc { font-size: 12px; color: $text-secondary; text-align: center; line-height: 1.5; }
</style>
