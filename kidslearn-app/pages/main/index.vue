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
import { onShow } from '@dcloudio/uni-app'
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

// 标记首次加载
function switchTab(key) {
  if (key === 'vip') {
    uni.navigateTo({ url: '/pages/mine/vip' })
    return
  }
  if (!loadedTabs.has(key)) loadedTabs.add(key)
  activeTab.value = key
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
  uni.navigateTo({ url: `/pages/learn/courses?subjectId=${subject.id}` })
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
    uni.showToast({ title: '年级配置成功', icon: 'success' })
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
  background: $bg;
}

/* ===== 侧边栏 ===== */
.sidebar {
  width: $sidebar-width;
  min-width: $sidebar-width;
  height: 100vh;
  background: $white;
  border-right: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  transition: all $transition-normal;
  overflow: hidden;
  z-index: 100;

  &.collapsed {
    width: $sidebar-collapsed;
    min-width: $sidebar-collapsed;
  }
}

.sidebar-header {
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.hamburger {
  width: 44px; height: 44px;
  display: flex; align-items: center; justify-content: center;
  border-radius: $radius; background: #F5F5F5;
  cursor: pointer; flex-shrink: 0;
  &:active { transform: scale(0.92); }
}

.hamburger-icon { font-size: 18px; color: $text; }

.brand { display: flex; align-items: center; gap: 8px; overflow: hidden; }
.brand-emoji { font-size: 32px; flex-shrink: 0; }
.brand-text { display: flex; flex-direction: column; }
.brand-name { font-size: 18px; font-weight: bold; color: $text; }
.brand-sub { font-size: 11px; color: $text-light; }

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
  padding: 12px 16px; border-radius: $radius;
  cursor: pointer; transition: all $transition-fast;
  white-space: nowrap;
  &:active { transform: scale(0.97); }
  .nav-icon { font-size: 22px; flex-shrink: 0; width: 28px; text-align: center; }
  .nav-label { font-size: 15px; color: $text; font-weight: 500; }
  &.active { background: #FFF0F0; .nav-label { color: $primary; font-weight: 600; } }
  &:not(.active):hover { background: #F8F8F8; }
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
  background: $white; border-bottom: 1px solid rgba(0,0,0,0.04);
}

.topbar-left { display: flex; align-items: center; gap: 12px; }
.topbar-title { font-size: 18px; font-weight: 600; color: $text; }
.topbar-center { flex: 1; display: flex; align-items: center; justify-content: center; }
.topbar-right { display: flex; align-items: center; gap: 10px; }

.greeting-text { font-size: 14px; color: $text; margin-right: 4px; }

.action-btn {
  width: 44px; height: 44px; border-radius: $radius;
  background: #F5F5F5; display: flex; align-items: center; justify-content: center;
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
  padding: 24px 28px;
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
  .sidebar { width: 48px; min-width: 48px; }
  .sidebar-header { padding: 8px; }
  .hamburger { width: 32px; height: 32px; }
  .sidebar-nav { padding: 4px; }
  .nav-item { padding: 10px 8px; .nav-icon { font-size: 20px; } }
  .user-area { padding: 4px; }
  .user-avatar { width: 36px; height: 36px; }
  .content-wrapper { padding: 12px; }
  .topbar { height: 48px; min-height: 48px; }
  .topbar-title { font-size: 15px; }
}
</style>
