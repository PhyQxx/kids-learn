<template>
  <view class="app-layout" :class="themeClass">
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
          :class="{ active: currentNavKey === item.key }"
          @tap="navigateTo(item)"
        >
          <text class="nav-icon">{{ item.icon }}</text>
          <text v-show="!collapsed" class="nav-label">{{ item.label }}</text>
        </view>
      </view>

      <!-- 用户区域 -->
      <view class="sidebar-footer">
        <view class="sidebar-divider"></view>
        <view class="user-area" @tap="navigateTo({ path: '/pages/mine/index' })">
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
      <view class="topbar" v-if="showTopbar">
        <view class="topbar-left">
          <view v-if="showBack" class="back-btn" @tap="goBack">
            <text>←</text>
          </view>
          <text v-if="title" class="topbar-title">{{ title }}</text>
          <slot name="topbar-left"></slot>
          <slot name="topbar-left-extra"></slot>
        </view>
        <view class="topbar-center">
          <slot name="topbar-center"></slot>
        </view>
        <view class="topbar-right">
          <slot name="topbar-right"></slot>
        </view>
      </view>

      <!-- 内容区域 -->
      <scroll-view class="content-scroll custom-scroll" scroll-y>
        <view class="content-wrapper">
          <slot></slot>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/store/user'

const props = defineProps({
  title: { type: String, default: '' },
  showBack: { type: Boolean, default: false },
  showTopbar: { type: Boolean, default: true },
  theme: { type: String, default: 'kids' }, // kids | learn | parent | dark
  activeNav: { type: String, default: '' }
})

const userStore = useUserStore()
const collapsed = computed(() => userStore.sidebarCollapsed)

const themeClass = computed(() => `theme-${props.theme}`)

const navKeyByPath = {
  '/pages/main/index': 'home',
  '/pages/learn/index': 'learn',
  '/pages/learn/courses': 'learn',
  '/pages/learn/levels': 'learn',
  '/pages/learn/videos': 'learn',
  '/pages/learn/quiz': 'learn',
  '/pages/pet/index': 'pet',
  '/pages/ranking/index': 'ranking',
  '/pages/achievement/index': 'achievement',
  '/pages/parent/index': 'parent',
  '/pages/parent/monitor': 'parent',
  '/pages/parent/report': 'parent',
  '/pages/parent/time-control': 'parent',
  '/pages/parent/family': 'parent',
  '/pages/mine/vip': 'vip'
}

// 当前导航 key
const currentNavKey = computed(() => {
  if (props.activeNav) {
    return props.activeNav.startsWith('/') ? navKeyByPath[props.activeNav] || props.activeNav : props.activeNav
  }
  const pages = getCurrentPages()
  if (pages.length > 0) {
    const path = '/' + pages[pages.length - 1].route
    return navKeyByPath[path] || path
  }
  return 'home'
})

// 导航项
const navItems = computed(() => {
  if (userStore.isParentMode) {
    return [
      { key: 'parent', icon: '👨‍👩‍👧', label: '家长中心', path: '/pages/parent/index' },
      { key: 'vip', icon: '👑', label: 'VIP会员', path: '/pages/mine/vip' }
    ]
  }
  return [
    { key: 'home', icon: '🏠', label: '首页', tab: 'home' },
    { key: 'learn', icon: '📚', label: '学习中心', tab: 'learn' },
    { key: 'pet', icon: '🐱', label: '我的宠物', tab: 'pet' },
    { key: 'ranking', icon: '🏆', label: '排行榜', tab: 'ranking' },
    { key: 'achievement', icon: '🏅', label: '成就', tab: 'achievement' }
  ]
})

function toggleSidebar() {
  userStore.toggleSidebar()
}

function navigateTo(item) {
  const url = item.tab ? `/pages/main/index?tab=${item.tab}` : item.path
  if (!url) return

  if (item.tab) {
    uni.reLaunch({ url })
    return
  }

  uni.navigateTo({ url, fail: () => {
    uni.switchTab({ url, fail: () => {
      uni.reLaunch({ url })
    }})
  }})
}

function goBack() {
  uni.navigateBack({ fail: () => {
    uni.reLaunch({ url: '/pages/main/index' })
  }})
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.app-layout {
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
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $radius;
  background: #F1F6FC;
  cursor: pointer;
  flex-shrink: 0;

  &:active { transform: scale(0.9); }
}

.hamburger-icon {
  font-size: 18px;
  color: $text;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
}

.brand-emoji {
  font-size: 32px;
  flex-shrink: 0;
}

.brand-text {
  display: flex;
  flex-direction: column;
}

.brand-name {
  white-space: nowrap;
  font-size: 18px;
  font-weight: bold;
  color: $text;
}

.brand-sub {
  font-size: 11px;
  color: $text-light;
}

/* ===== 导航菜单 ===== */
.sidebar-nav {
  flex: 1;
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 52px;
  padding: 12px 16px;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all $transition-fast;
  white-space: nowrap;

  &:active { transform: scale(0.97); }

  .nav-icon {
    font-size: 24px;
    flex-shrink: 0;
    width: 28px;
    text-align: center;
  }

  .nav-label {
    font-size: 16px;
    color: $text;
    font-weight: 700;
  }

  &.active {
    background: #FFF0E8;
    box-shadow: 0 8px 18px rgba(255, 122, 89, 0.16);
    .nav-label { color: $primary; font-weight: 800; }
  }

  &:not(.active):hover {
    background: #F1F6FC;
  }
}

/* 学习主题蓝色激活 */
.theme-learn .nav-item.active {
  background: #E8F0FE;
  .nav-label { color: $learn-blue; }
}

/* 家长主题青色激活 */
.theme-parent .nav-item.active {
  background: #E8F8F8;
  .nav-label { color: $teal; }
}

/* VIP暗色主题 */
.theme-dark {
  .sidebar {
    background: $dark2;
    border-right-color: rgba(255, 255, 255, 0.06);
  }
  .brand-name { color: $white; }
  .brand-sub { color: rgba(255, 255, 255, 0.5); }
  .hamburger { background: rgba(255, 255, 255, 0.1); }
  .hamburger-icon { color: $white; }
  .nav-item {
    .nav-label { color: rgba(255, 255, 255, 0.6); }
    &:not(.active):hover { background: rgba(255, 255, 255, 0.05); }
  }
  .nav-item.active {
    background: rgba(255, 215, 0, 0.1);
    .nav-label { color: $gold; }
  }
  .sidebar-divider { background: rgba(255, 255, 255, 0.06); }
  .user-name { color: rgba(255, 255, 255, 0.8); }
  .user-level { color: rgba(255, 255, 255, 0.4); }
}

/* ===== 用户区域 ===== */
.sidebar-footer {
  padding: 12px;
}

.sidebar-divider {
  height: 1px;
  background: rgba(0, 0, 0, 0.06);
  margin-bottom: 12px;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 56px;
  padding: 8px;
  border-radius: $radius;
  cursor: pointer;

  &:active { background: #F5F5F5; }
}

.user-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, $primary, $primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar-emoji {
  font-size: 24px;
}

.theme-parent .user-avatar {
  background: linear-gradient(135deg, $teal, $teal-dark);
}

.theme-dark .user-avatar {
  background: linear-gradient(135deg, $gold, #FFA500);
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: $text;
}

.user-level {
  font-size: 11px;
  color: $text-light;
}

/* ===== 主内容区 ===== */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: transparent;
}

.theme-dark .main-area {
  background: $dark;
}

.topbar {
  height: $topbar-height;
  min-height: $topbar-height;
  padding: 0 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.82);
  border-bottom: 1px solid rgba(73, 98, 128, 0.06);
}

.theme-parent .topbar {
  background: linear-gradient(135deg, $teal, $teal-dark);
  .topbar-title { color: $white; }
}

.theme-dark .topbar {
  background: $dark2;
  border-bottom-color: rgba(255, 255, 255, 0.06);
  .topbar-title { color: $white; }
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.topbar-title {
  font-size: 20px;
  font-weight: 800;
  color: $text;
}

.back-btn {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $radius;
  background: #F1F6FC;
  font-size: 18px;
  cursor: pointer;

  &:active { transform: scale(0.9); }
}

.topbar-center {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.content-scroll {
  flex: 1;
  height: 0; // 让flex正确计算高度
}

.content-wrapper {
  padding: 24px 28px 32px;
  min-height: 100%;
}

@media (max-width: 640px) {
  .app-layout { flex-direction: column-reverse; }
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
  .topbar {
    height: 56px;
    min-height: 56px;
    padding: 0 14px;
  }
  .topbar-title { font-size: 17px; }
  .back-btn { width: 42px; height: 42px; }
  .content-wrapper { padding: 14px 14px 18px; }
}
</style>
