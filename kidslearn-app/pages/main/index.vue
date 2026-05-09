<template>
  <view class="main-page" :class="themeClass">
    <!-- 侧边栏 -->
    <view class="sidebar" :class="{ collapsed: collapsed }">
      <!-- Logo区域 -->
      <view class="sidebar-header">
        <view class="hamburger" @tap="toggleSidebar">
          <text class="hamburger-icon">{{ collapsed ? '☰' : '✕' }}</text>
        </view>
        <view v-show="!collapsed" class="brand">
          <text class="brand-emoji">🌍</text>
          <view class="brand-text">
            <text class="brand-name">趣学星球</text>
            <text class="brand-sub">KidsLearn</text>
          </view>
        </view>
      </view>

      <!-- 导航菜单 -->
      <view class="sidebar-nav">
        <view
          v-for="item in navItems"
          :key="item.key"
          class="nav-item"
          :class="{ active: activeTab === item.key }"
          @tap="switchTab(item.key)"
        >
          <text class="nav-icon">{{ item.icon }}</text>
          <text v-show="!collapsed" class="nav-label">{{ item.label }}</text>
        </view>
      </view>

      <!-- 用户区域 -->
      <view class="sidebar-footer">
        <view class="sidebar-divider"></view>
        <view class="user-area" @tap="goSettings">
          <view class="user-avatar">
            <text class="avatar-emoji">👦</text>
          </view>
          <view v-show="!collapsed" class="user-info">
            <text class="user-name">{{ userStore.nickname }}</text>
            <text class="user-level">Lv.{{ userStore.level }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 主内容区 -->
    <view class="main-area">
      <!-- 顶部栏 -->
      <view class="topbar">
        <view class="topbar-left">
          <text class="topbar-title">{{ currentTitle }}</text>
          <text v-if="activeTab !== 'parent'" class="topbar-subtitle">选一个喜欢的任务开始吧</text>
        </view>
        <view class="topbar-center"></view>
        <view class="topbar-right">
          <text class="greeting-text">你好，{{ userStore.nickname }} 👋</text>
          <view class="action-btn" @tap="goNotifications"><text>🔔</text></view>
          <view v-if="activeTab !== 'parent'" class="parent-mode-btn" @tap="enterParentMode">
            <text class="text-white text-xs">👨‍👩‍👧 家长模式</text>
          </view>
          <view v-if="activeTab === 'parent'" class="exit-parent-btn" @tap="exitParentMode">
            <text class="text-white text-xs">退出家长模式</text>
          </view>
        </view>
      </view>

      <!-- 内容区域 - 组件切换 -->
      <scroll-view class="content-scroll custom-scroll" scroll-y :enhanced="true" :bounces="false" :show-scrollbar="false">
        <view class="content-wrapper">
          <HomeContent v-if="loadedTabs.has('home')" v-show="activeTab === 'home'" @go-subject="goSubject" @go-learn="switchTab('learn')" />
          <LearnContent v-if="loadedTabs.has('learn')" v-show="activeTab === 'learn'" @go-courses="goCourses" />
          <PetContent v-if="loadedTabs.has('pet')" v-show="activeTab === 'pet'" />
          <RankingContent v-if="loadedTabs.has('ranking')" v-show="activeTab === 'ranking'" />
          <AchievementContent v-if="loadedTabs.has('achievement')" v-show="activeTab === 'achievement'" />
          <ParentContent v-if="loadedTabs.has('parent')" v-show="activeTab === 'parent'" />
        </view>
      </scroll-view>
    </view>
  </view>

  <!-- 年级配置弹框（首次登录或未配置年级时弹出） -->
  <GradeSelectPopup
    :visible.sync="showGradePopup"
    :current-grade="userStore.userInfo?.gradeLevelId"
    @confirm="handleGradeConfirm"
  />
</template>

<script setup>
import { ref, computed, provide, onMounted, reactive } from 'vue'
import { onShow, onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/user'
import { useLearnStore } from '@/store/learn'
import { getUserInfo, updateChildProfile } from '@/api/user'

import HomeContent from '@/components/home/HomeContent.vue'
import LearnContent from '@/components/learn/LearnContent.vue'
import PetContent from '@/components/pet/PetContent.vue'
import RankingContent from '@/components/ranking/RankingContent.vue'
import AchievementContent from '@/components/achievement/AchievementContent.vue'
import ParentContent from '@/components/parent/ParentContent.vue'
import GradeSelectPopup from '@/components/GradeSelectPopup.vue'

const userStore = useUserStore()
const learnStore = useLearnStore()

const activeTab = ref('home')
const collapsed = computed(() => userStore.sidebarCollapsed)
const loadedTabs = reactive(new Set(['home']))
const showGradePopup = ref(false)

// 向子组件暴露年级弹窗状态，避免签到弹窗同时弹出
provide('gradePopupVisible', showGradePopup)

// 标记首次加载
function switchTab(key) {
  if (key === 'vip') {
    uni.navigateTo({ url: '/pages/mine/vip' })
    return
  }
  // 如果点击的是当前 tab，检查是否在子页面
  if (activeTab.value === key) {
    // 如果当前在子页面（URL 不是 main/index），返回到主页
    const pages = getCurrentPages()
    const currentPage = pages[pages.length - 1]
    const route = currentPage?.route || ''
    if (!route.includes('main/index')) {
      uni.reLaunch({ url: '/pages/main/index?tab=' + key })
    }
    return
  }
  // 切换到不同 tab，reLaunch 带上目标 tab
  uni.reLaunch({ url: '/pages/main/index?tab=' + key })
}

// 通过 provide 向子组件暴露 switchTab 方法
provide('switchTab', switchTab)

const themeClass = computed(() => {
  if (activeTab.value === 'learn') return 'theme-learn'
  if (activeTab.value === 'parent') return 'theme-parent'
  return 'theme-kids'
})

const currentTitle = computed(() => {
  const map = {
    home: '首页',
    learn: '学习中心',
    pet: '我的宠物',
    ranking: '排行榜',
    achievement: '成就中心',
    parent: '家长中心'
  }
  return map[activeTab.value] || ''
})

const navItems = computed(() => {
  if (userStore.isParentMode) {
    return [
      { key: 'parent', icon: '👨‍👩‍👧', label: '家长中心' },
      { key: 'vip', icon: '👑', label: 'VIP会员' }
    ]
  }
  return [
    { key: 'home', icon: '🏠', label: '首页' },
    { key: 'learn', icon: '📚', label: '学习中心' },
    { key: 'pet', icon: '🐱', label: '我的宠物' },
    { key: 'ranking', icon: '🏆', label: '排行榜' },
    { key: 'achievement', icon: '🏅', label: '成就' }
  ]
})

function toggleSidebar() {
  userStore.toggleSidebar()
}

function enterParentMode() {
  uni.navigateTo({ url: '/pages/parent/gate' })
}

function exitParentMode() {
  userStore.setParentMode(false)
  activeTab.value = 'home'
}

function goSettings() {
  uni.navigateTo({ url: '/pages/mine/settings' })
}

function goNotifications() {
  uni.showToast({ title: '暂无新消息', icon: 'none' })
}

function goSubject(subject) {
  learnStore.setSubject(subject)
  // 移动端首页今日学习进入专项练习，不再是闯关
  uni.navigateTo({ url: `/pages/learn/quiz?practiceModeId=${subject.id}&timeLimit=0` })
}

function goCourses(subject) {
  learnStore.setSubject(subject)
  uni.navigateTo({ url: `/pages/learn/courses?subjectId=${subject.id}` })
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
  if (userStore.isParentMode) {
    loadedTabs.add('parent')
    activeTab.value = 'parent'
  }
  // 检测年级配置
  checkGradeSetup()
})

onLoad((query) => {
  if (query.tab && ['home', 'learn', 'pet', 'ranking', 'achievement', 'parent'].includes(query.tab)) {
    if (!loadedTabs.has(query.tab)) loadedTabs.add(query.tab)
    activeTab.value = query.tab
  }
})

onShow(() => {
  if (userStore.isParentMode && activeTab.value !== 'parent') {
    activeTab.value = 'parent'
  }
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.main-page {
  display: flex;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(circle at 18% 10%, rgba(255, 217, 90, 0.20), transparent 24%),
    linear-gradient(180deg, #F8FBFF 0%, #FFF8F2 100%);
}

/* ===== 侧边栏 ===== */
.sidebar {
  width: $sidebar-width;
  min-width: $sidebar-width;
  height: 100vh;
  background: rgba(255, 255, 255, 0.92);
  border-right: 1px solid rgba(73, 98, 128, 0.08);
  box-shadow: 8px 0 28px rgba(73, 98, 128, 0.06);
  display: flex;
  flex-direction: column;
  transition: all $transition-normal;
  overflow: hidden;
  z-index: 100;

  &.collapsed {
    width: $sidebar-collapsed;
    min-width: $sidebar-collapsed;

    .sidebar-header {
      justify-content: center;
      padding: 12px 6px;
    }

    .hamburger {
      width: 46px;
      height: 46px;
    }

    .sidebar-nav {
      align-items: center;
      padding: 8px 6px;
    }

    .nav-item {
      width: 48px;
      min-height: 48px;
      justify-content: center;
      gap: 0;
      padding: 0;
      margin: 0 auto;
      border-radius: 16px;
      box-sizing: border-box;

      .nav-icon {
        width: auto;
        font-size: 24px;
        line-height: 1;
      }
    }

    .sidebar-footer {
      padding: 8px 6px;
    }

    .user-area {
      width: 48px;
      min-height: 48px;
      justify-content: center;
      padding: 0;
      margin: 0 auto;
      box-sizing: border-box;
    }

    .user-avatar {
      width: 42px;
      height: 42px;
    }
  }
}

.sidebar-header {
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.hamburger {
  width: 48px; height: 48px;
  display: flex; align-items: center; justify-content: center;
  border-radius: $radius; background: #F1F6FC;
  cursor: pointer; flex-shrink: 0;
  &:active { transform: scale(0.92); }
}

.hamburger-icon { font-size: 18px; color: $text; }

.brand { display: flex; align-items: center; gap: 8px; overflow: hidden; }
.brand-emoji { font-size: 32px; flex-shrink: 0; }
.brand-text { display: flex; flex-direction: column; }
.brand-name { white-space: nowrap; font-size: 19px; font-weight: bold; color: $text; }
.brand-sub { font-size: 12px; color: $text-light; }

/* 导航 */
.sidebar-nav {
  flex: 1;
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex; align-items: center; gap: 12px;
  min-height: 52px;
  padding: 12px 16px; border-radius: $radius-md;
  cursor: pointer; transition: all $transition-fast;
  white-space: nowrap;
  &:active { transform: scale(0.97); }
  .nav-icon { font-size: 24px; flex-shrink: 0; width: 30px; text-align: center; }
  .nav-label { font-size: 16px; color: $text; font-weight: 700; }
  &.active {
    background: #FFF0E8;
    box-shadow: 0 8px 18px rgba(255, 122, 89, 0.16);
    .nav-label { color: $primary; font-weight: 800; }
  }
  &:not(.active):hover { background: #F1F6FC; }
}

/* 学习主题 */
.theme-learn .nav-item.active { background: #E8F0FE; .nav-label { color: $learn-blue; } }

/* 家长主题 */
.theme-parent {
  .sidebar { background: $white; }
  .nav-item.active { background: #E8F8F8; .nav-label { color: $teal; } }
  .topbar { background: linear-gradient(135deg, $teal, $teal-dark); }
  .topbar-title { color: $white; }
}

/* 用户区域 */
.sidebar-footer { padding: 12px; }
.sidebar-divider { height: 1px; background: rgba(0,0,0,0.06); margin-bottom: 12px; }

.user-area {
  display: flex; align-items: center; gap: 10px;
  min-height: 56px;
  padding: 8px; border-radius: $radius; cursor: pointer;
  &:active { background: #F5F5F5; }
}

.user-avatar {
  width: 44px; height: 44px; border-radius: 50%;
  background: linear-gradient(135deg, $primary, $primary-light);
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}

.avatar-emoji { font-size: 24px; }
.user-info { display: flex; flex-direction: column; }
.user-name { font-size: 14px; font-weight: 600; color: $text; }
.user-level { font-size: 11px; color: $text-light; }

/* ===== 主内容区 ===== */
.main-area {
  flex: 1; display: flex; flex-direction: column; overflow: hidden; background: $bg;
}

.topbar {
  height: $topbar-height; min-height: $topbar-height;
  padding: 0 28px; display: flex; align-items: center; justify-content: space-between;
  background: rgba(255, 255, 255, 0.82);
  border-bottom: 1px solid rgba(73, 98, 128, 0.06);
}

.topbar-left { display: flex; flex-direction: column; justify-content: center; gap: 2px; }
.topbar-title { font-size: 20px; font-weight: 800; color: $text; }
.topbar-subtitle { font-size: 12px; color: $text-light; }
.topbar-center { flex: 1; display: flex; align-items: center; justify-content: center; }
.topbar-right { display: flex; align-items: center; gap: 10px; }

.greeting-text { font-size: 15px; color: $text-secondary; margin-right: 4px; font-weight: 600; }

.action-btn {
  width: 48px; height: 48px; border-radius: $radius;
  background: #F1F6FC; display: flex; align-items: center; justify-content: center;
  font-size: 18px; cursor: pointer;
  &:active { transform: scale(0.92); }
}

.parent-mode-btn {
  background: linear-gradient(135deg, $teal, $teal-dark);
  border-radius: 100px; padding: 6px 16px;
  box-shadow: 0 4px 12px rgba(78, 205, 196, 0.3);
  cursor: pointer;
  &:active { transform: scale(0.95); }
}

.exit-parent-btn {
  background: $error;
  border-radius: 100px; padding: 6px 16px;
  cursor: pointer;
  &:active { transform: scale(0.95); }
}

.content-scroll {
  flex: 1;
  height: 0;
}

.content-wrapper {
  padding: 24px 28px 32px;
  min-height: 100%;
}

/* ===== 响应式适配 ===== */
@media (max-width: 800px) {
  .sidebar {
    width: $sidebar-collapsed;
    min-width: $sidebar-collapsed;
    &.collapsed { width: 48px; min-width: 48px; }
  }
  .brand { display: none !important; }
  .nav-label { display: none !important; }
  .user-info { display: none !important; }
  .content-wrapper { padding: 16px; }
  .topbar { padding: 0 16px; }
  .greeting-text { display: none; }
}

@media (max-width: 640px) {
  .main-page { flex-direction: column-reverse; }
  .sidebar {
    width: 100%;
    min-width: 100%;
    height: 68px;
    min-height: 68px;
    border-right: 0;
    border-top: 1px solid rgba(73, 98, 128, 0.08);
    box-shadow: 0 -8px 28px rgba(73, 98, 128, 0.08);

    &.collapsed {
      width: 100%;
      min-width: 100%;
    }
  }
  .sidebar-header, .sidebar-footer { display: none; }
  .sidebar-nav {
    flex-direction: row;
    align-items: center;
    justify-content: space-around;
    gap: 4px;
    padding: 8px;
  }
  .nav-item {
    flex: 1;
    min-height: 52px;
    flex-direction: column;
    justify-content: center;
    gap: 2px;
    padding: 6px 4px;
    border-radius: 16px;
    .nav-icon { font-size: 20px; width: auto; }
    .nav-label {
      display: block !important;
      font-size: 11px;
      line-height: 1.1;
    }
  }
  .content-wrapper { padding: 14px 14px 18px; }
  .topbar { height: 56px; min-height: 56px; padding: 0 14px; }
  .topbar-title { font-size: 17px; }
  .topbar-subtitle { display: none; }
  .parent-mode-btn, .exit-parent-btn { display: none; }
}
</style>
