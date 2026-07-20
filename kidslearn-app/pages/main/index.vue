<template>
  <AppLayout
    theme="kids"
    :show-topbar="true"
    :show-back="false"
    :active-nav="activeTab"
  >
    <!-- 顶部栏左侧 -->
    <template #topbar-left>
      <view class="topbar-left-custom">
        <text class="topbar-title">{{ currentTitle }}</text>
        <text class="topbar-subtitle">选一个喜欢的任务开始吧</text>
      </view>
    </template>

    <!-- 顶部栏右侧 -->
    <template #topbar-right>
      <text class="greeting-text">你好，{{ userStore.nickname }} 👋</text>
      <view class="action-btn" @tap="goNotifications"><text>🔔</text></view>
    </template>

    <!-- 主内容 -->
    <view class="main-content">
      <HomeContent v-if="loadedTabs.has('home')" v-show="activeTab === 'home'" @go-subject="goSubject" @go-learn="switchTab('learn')" />
      <LearnContent v-if="loadedTabs.has('learn')" v-show="activeTab === 'learn'" @go-courses="goCourses" />
      <PetContent v-if="loadedTabs.has('pet')" v-show="activeTab === 'pet'" />
      <RankingContent v-if="loadedTabs.has('ranking')" v-show="activeTab === 'ranking'" />
      <AchievementContent v-if="loadedTabs.has('achievement')" v-show="activeTab === 'achievement'" />
    </view>
  </AppLayout>

  <!-- 年级配置弹框（首次登录或未配置年级时弹出） -->
  <GradeSelectPopup
    :visible.sync="showGradePopup"
    :current-grade="userStore.userInfo?.gradeLevelId"
    @confirm="handleGradeConfirm"
  />

  <GlobalLoadingOverlay />
</template>

<script setup>
import { ref, computed, provide, onMounted, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/user'
import { useLearnStore } from '@/store/learn'
import { getUserInfo, updateChildProfile } from '@/api/user'
import { getParentReport, getTimeControl } from '@/api/parent'
import { ensureLearningAccess } from '@/utils/learningAccess.mjs'
import { soundManager } from '@/utils/sound'

import AppLayout from '@/components/AppLayout.vue'
import HomeContent from '@/components/home/HomeContent.vue'
import LearnContent from '@/components/learn/LearnContent.vue'
import PetContent from '@/components/pet/PetContent.vue'
import RankingContent from '@/components/ranking/RankingContent.vue'
import AchievementContent from '@/components/achievement/AchievementContent.vue'
import GradeSelectPopup from '@/components/GradeSelectPopup.vue'
import GlobalLoadingOverlay from '@/components/common/GlobalLoadingOverlay.vue'

const userStore = useUserStore()
const learnStore = useLearnStore()

const activeTab = ref('home')
const loadedTabs = reactive(new Set(['home']))
const showGradePopup = ref(false)

// 向子组件暴露年级弹窗状态，避免签到弹窗同时弹出
provide('gradePopupVisible', showGradePopup)

// 标记首次加载
function switchTab(key) {
  soundManager.play('tap')
  if (key === 'vip') {
    uni.navigateTo({ url: '/pages/mine/vip' })
    return
  }
  // 挑战赛作为新页面打开
  if (key === 'challenge') {
    activeTab.value = 'challenge'
    uni.navigateTo({ url: '/pages/challenge/index' })
    return
  }

  // 如果已经在 main/index 页面，直接切换 activeTab
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  const route = currentPage?.route || ''

  if (route.includes('main/index')) {
    if (activeTab.value === key) return
    activeTab.value = key
    if (!loadedTabs.has(key)) loadedTabs.add(key)
    return
  }

  // 切换到不同 tab，reLaunch 带上目标 tab
  uni.reLaunch({ url: '/pages/main/index?tab=' + key })
}

// 通过 provide 向子组件暴露 switchTab 方法
provide('switchTab', switchTab)

const currentTitle = computed(() => {
  const map = {
    home: '首页',
    learn: '学习中心',
    pet: '我的宠物',
    ranking: '排行榜',
    achievement: '成就中心'
  }
  return map[activeTab.value] || ''
})

function goNotifications() {
  uni.showToast({ title: '暂无新消息', icon: 'none' })
}

function showLearningBlocked(message) {
  uni.showModal({
    title: '休息一下',
    content: message,
    showCancel: false
  })
}

async function goSubject(subject) {
  const allowed = await ensureLearningAccess({
    fetchTimeControl: getTimeControl,
    fetchReport: getParentReport,
    showBlockedMessage: showLearningBlocked
  })
  if (!allowed) return

  learnStore.setSubject(subject)
  // 移动端首页今日学习进入专项练习，不再是闯关
  uni.navigateTo({ url: `/pages/learn/quiz?practiceModeId=${subject.id}&timeLimit=0` })
}

function goCourses(subject) {
  learnStore.setSubject(subject)
  uni.navigateTo({ url: `/pages/learn/levels?subjectId=${subject.id}&subjectName=${encodeURIComponent(subject.name)}` })
}

async function checkGradeSetup() {
  // 获取最新用户信息
  try {
    const info = await getUserInfo()
    if (info) {
      userStore.setUserInfo(info)
      // 如果没有配置年级，弹出设置弹框
      if (!info.gradeLevelId) {
        showGradePopup.value = true
      }
    }
  } catch (e) {
    console.log('检查年级配置失败', e)
  }
}

async function handleGradeConfirm(grade) {
  try {
    await updateChildProfile({ gradeLevel: grade })
    const info = await getUserInfo()
    if (info) userStore.setUserInfo(info)
    learnStore.clearLearningContext()
    uni.showToast({ title: '年级配置成功', icon: 'success' })
    uni.reLaunch({ url: '/pages/main/index?tab=learn' })
  } catch (e) {
    uni.showToast({ title: '配置失败', icon: 'none' })
  }
}

onMounted(() => {
  // 检测年级配置
  checkGradeSetup()
})

onLoad((query) => {
  if (query.tab && ['home', 'learn', 'pet', 'ranking', 'achievement'].includes(query.tab)) {
    if (!loadedTabs.has(query.tab)) loadedTabs.add(query.tab)
    activeTab.value = query.tab
  }
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.topbar-left-custom {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 2px;
}

.topbar-title {
  font-size: 20px;
  font-weight: 800;
  color: $text;
}

.topbar-subtitle {
  font-size: 12px;
  color: $text-light;
}

.greeting-text {
  font-size: 15px;
  color: $text-secondary;
  margin-right: 4px;
  font-weight: 600;
}

.action-btn {
  width: 48px;
  height: 48px;
  border-radius: $radius;
  background: #F1F6FC;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  cursor: pointer;

  &:active { transform: scale(0.92); }
}

.main-content {
  min-height: 100%;
}

/* 响应式适配 */
@include respond-md {
  .greeting-text { display: none; }
}

@include respond-sm {
  .topbar-subtitle { display: none; }
  .greeting-text { display: none; }
}
</style>
