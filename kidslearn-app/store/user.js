import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useLearnStore } from './learn'
import { usePetStore } from './pet'
import { useRealtimeStore } from './realtime'

export const useUserStore = defineStore('user', () => {
  const token = ref(uni.getStorageSync('token') || '')
  const refreshTokenStr = ref(uni.getStorageSync('refreshToken') || '')
  const userInfo = ref(JSON.parse(uni.getStorageSync('userInfo') || 'null'))
  const sidebarCollapsed = ref(false)
  const ageGroup = ref(uni.getStorageSync('ageGroup') || 'lively') // 'macaron' | 'lively' | 'fresh'
  // 家长模式验证后的 PIN（仅存内存，不持久化，登出清除）；供家长写操作接口透传后端校验
  const verifiedParentPin = ref('')

  const isLoggedIn = computed(() => !!token.value)
  const nickname = computed(() => userInfo.value?.nickname || '小朋友')
  const level = computed(() => userInfo.value?.level || 1)
  const gold = computed(() => userInfo.value?.gold || 0)
  const themeClass = computed(() => `theme-${ageGroup.value}`)
  const onboardingStep = computed(() => userInfo.value?.onboardingStep ?? 0)

  function setToken(val, rVal) {
    token.value = val
    uni.setStorageSync('token', val)
    if (rVal) {
      refreshTokenStr.value = rVal
      uni.setStorageSync('refreshToken', rVal)
    }
  }

  function setUserInfo(info) {
    userInfo.value = info
    uni.setStorageSync('userInfo', JSON.stringify(info))
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setAgeGroup(age) {
    if (['macaron', 'lively', 'fresh'].includes(age)) {
      ageGroup.value = age
      uni.setStorageSync('ageGroup', age)
    }
  }

  function setVerifiedParentPin(pin) {
    verifiedParentPin.value = pin || ''
  }

  function logout() {
    uni.closeSocket({ complete: () => {} })
    // Clear other stores
    const learnStore = useLearnStore()
    const petStore = usePetStore()
    const realtimeStore = useRealtimeStore()
    learnStore.clearLearningContext()
    learnStore.setDailyTasks([])
    petStore.setPetInfo(null)
    realtimeStore.close()
    // Clear user state
    token.value = ''
    refreshTokenStr.value = ''
    userInfo.value = null
    ageGroup.value = 'lively'
    verifiedParentPin.value = ''
    uni.removeStorageSync('token')
    uni.removeStorageSync('refreshToken')
    uni.removeStorageSync('userInfo')
    uni.removeStorageSync('ageGroup')
    uni.reLaunch({ url: '/pages/login/index' })
  }

  return {
    token, refreshToken: refreshTokenStr, userInfo, sidebarCollapsed, ageGroup, verifiedParentPin,
    isLoggedIn, nickname, level, gold, themeClass, onboardingStep,
    setToken, setUserInfo, toggleSidebar, setAgeGroup, setVerifiedParentPin, logout
  }
})
