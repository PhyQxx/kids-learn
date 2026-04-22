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
          :key="item.path"
          class="nav-item"
          :class="{ active: currentPage === item.path }"
          @tap="navigateTo(item.path)"
        >
          <text class="nav-icon">{{ item.icon }}</text>
          <text v-show="!collapsed" class="nav-label">{{ item.label }}</text>
        </view>
      </view>

      <!-- 用户区域 -->
      <view class="sidebar-footer">
        <view class="sidebar-divider"></view>
        <view class="user-area" @tap="navigateTo('/pages/mine/index')">
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

// 当前页面路径
const currentPage = computed(() => {
  if (props.activeNav) return props.activeNav
  const pages = getCurrentPages()
  if (pages.length > 0) return '/' + pages[pages.length - 1].route
  return ''
})

// 导航项
const navItems = computed(() => {
  if (userStore.isParentMode) {
    return [
      { icon: '👨‍👩‍👧', label: '家长中心', path: '/pages/parent/index' },
      { icon: '👑', label: 'VIP会员', path: '/pages/mine/vip' }
    ]
  }
  return [
    { icon: '🏠', label: '首页', path: '/pages/main/index' },
    { icon: '📚', label: '学习中心', path: '/pages/main/index' },
    { icon: '🐱', label: '我的宠物', path: '/pages/main/index' },
    { icon: '🏆', label: '排行榜', path: '/pages/main/index' },
    { icon: '🏅', label: '成就', path: '/pages/main/index' }
  ]
})

function toggleSidebar() {
  userStore.toggleSidebar()
}

function navigateTo(path) {
  uni.navigateTo({ url: path, fail: () => {
    uni.switchTab({ url: path, fail: () => {
      uni.reLaunch({ url: path })
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
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $radius;
  background: #F5F5F5;
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
  padding: 12px 16px;
  border-radius: $radius;
  cursor: pointer;
  transition: all $transition-fast;
  white-space: nowrap;

  &:active { transform: scale(0.97); }

  .nav-icon {
    font-size: 22px;
    flex-shrink: 0;
    width: 28px;
    text-align: center;
  }

  .nav-label {
    font-size: 15px;
    color: $text;
    font-weight: 500;
  }

  &.active {
    background: #FFF0F0;
    .nav-label { color: $primary; font-weight: 600; }
  }

  &:not(.active):hover {
    background: #F8F8F8;
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
  background: $bg;
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
  background: $white;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
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
  font-size: 18px;
  font-weight: 600;
  color: $text;
}

.back-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $radius;
  background: #F5F5F5;
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
  padding: 24px 28px;
  min-height: 100%;
}
</style>
