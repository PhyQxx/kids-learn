import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(uni.getStorageSync('token') || '')
  const userInfo = ref(JSON.parse(uni.getStorageSync('userInfo') || 'null'))
  const isParentMode = ref(false)
  const sidebarCollapsed = ref(false)
  const ageGroup = ref(uni.getStorageSync('ageGroup') || 'lively') // 'macaron' | 'lively' | 'fresh'

  const isLoggedIn = computed(() => !!token.value)
  const nickname = computed(() => userInfo.value?.nickname || '小朋友')
  const level = computed(() => userInfo.value?.level || 1)
  const gold = computed(() => userInfo.value?.gold || 0)
  const themeClass = computed(() => `theme-${ageGroup.value}`)

  function setToken(val) {
    token.value = val
    uni.setStorageSync('token', val)
  }

  function setUserInfo(info) {
    userInfo.value = info
    uni.setStorageSync('userInfo', JSON.stringify(info))
  }

  function setParentMode(val) {
    isParentMode.value = val
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

  function logout() {
    token.value = ''
    userInfo.value = null
    isParentMode.value = false
    ageGroup.value = 'lively'
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')
    uni.removeStorageSync('ageGroup')
    uni.reLaunch({ url: '/pages/login/index' })
  }

  return {
    token, userInfo, isParentMode, sidebarCollapsed, ageGroup,
    isLoggedIn, nickname, level, gold, themeClass,
    setToken, setUserInfo, setParentMode, toggleSidebar, setAgeGroup, logout
  }
})
