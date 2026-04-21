import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(uni.getStorageSync('token') || '')
  const userInfo = ref(JSON.parse(uni.getStorageSync('userInfo') || 'null'))
  const isParentMode = ref(false)
  const sidebarCollapsed = ref(false)

  const isLoggedIn = computed(() => !!token.value)
  const nickname = computed(() => userInfo.value?.nickname || '小朋友')
  const level = computed(() => userInfo.value?.level || 1)
  const gold = computed(() => userInfo.value?.gold || 0)

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

  function logout() {
    token.value = ''
    userInfo.value = null
    isParentMode.value = false
    uni.removeStorageSync('token')
    uni.removeStorageSync('userInfo')
    uni.reLaunch({ url: '/pages/login/index' })
  }

  return {
    token, userInfo, isParentMode, sidebarCollapsed,
    isLoggedIn, nickname, level, gold,
    setToken, setUserInfo, setParentMode, toggleSidebar, logout
  }
})
