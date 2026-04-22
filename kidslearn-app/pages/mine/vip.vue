<template>
  <view class="vip-page">
    <!-- 暗色侧边栏 -->
    <view class="sidebar" :class="{ collapsed: collapsed }">
      <view class="sidebar-header">
        <view class="hamburger" @tap="toggleSidebar">
          <text class="hamburger-icon">{{ collapsed ? '☰' : '✕' }}</text>
        </view>
        <view v-show="!collapsed" class="brand">
          <text class="brand-emoji">🌍</text>
          <view class="brand-text">
            <text class="brand-name">趣学星球</text>
            <text class="brand-sub">VIP</text>
          </view>
        </view>
      </view>

      <view class="sidebar-nav">
        <view class="nav-item" @tap="goHome">
          <text class="nav-icon">🏠</text>
          <text v-show="!collapsed" class="nav-label">返回首页</text>
        </view>
        <view class="nav-item active">
          <text class="nav-icon">👑</text>
          <text v-show="!collapsed" class="nav-label">VIP会员</text>
        </view>
      </view>

      <view class="sidebar-footer">
        <view class="sidebar-divider"></view>
        <view class="user-area">
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

    <!-- 主内容 -->
    <view class="vip-main">
      <scroll-view class="content-scroll" scroll-y>
        <view class="vip-content">
          <!-- Hero Banner -->
          <view class="hero-banner">
            <text class="hero-crown animate-float">👑</text>
            <text class="text-2xl text-bold text-gold">VIP会员</text>
            <text class="text-md text-white" style="opacity: 0.7;">解锁全部课程和专属特权</text>
          </view>

          <!-- 套餐选择 -->
          <view class="plan-grid">
            <view class="plan-card card">
              <text class="plan-name">月卡</text>
              <view class="plan-price">
                <text class="text-3xl text-bold text-white">¥25</text>
                <text class="text-xs text-white" style="opacity: 0.6;">/月</text>
              </view>
              <text class="text-xs text-white" style="opacity: 0.5;">原价 ¥30/月</text>
              <tn-button shape="round" size="lg" block @click="buyPlan('monthly')">开通</tn-button>
            </view>
            <view class="plan-card card featured">
              <view class="featured-tag"><text class="text-xs text-bold">推荐</text></view>
              <text class="plan-name">年卡</text>
              <view class="plan-price">
                <text class="text-3xl text-bold text-gold">¥168</text>
                <text class="text-xs text-gold" style="opacity: 0.6;">/年</text>
              </view>
              <text class="text-xs text-white" style="opacity: 0.5;">¥14/月 · 省53%</text>
              <tn-button type="warning" shape="round" size="lg" block @click="buyPlan('annual')">开通</tn-button>
            </view>
            <view class="plan-card card">
              <text class="plan-name">永久</text>
              <view class="plan-price">
                <text class="text-3xl text-bold text-white">¥298</text>
                <text class="text-xs text-white" style="opacity: 0.6;">一次</text>
              </view>
              <text class="text-xs text-white" style="opacity: 0.5;">永久享受所有特权</text>
              <tn-button shape="round" size="lg" block @click="buyPlan('permanent')">开通</tn-button>
            </view>
          </view>

          <!-- 特权对比 -->
          <view class="compare-card card">
            <text class="text-md text-bold" style="margin-bottom: 12px;">📊 特权对比</text>
            <view class="compare-header">
              <text class="compare-col">特权</text>
              <text class="compare-col">免费</text>
              <text class="compare-col gold">VIP</text>
            </view>
            <view v-for="f in features" :key="f.name" class="compare-row">
              <text class="compare-col text-sm">{{ f.name }}</text>
              <text class="compare-col">{{ f.free ? '✅' : '❌' }}</text>
              <text class="compare-col gold">✅</text>
            </view>
          </view>

          <!-- FAQ -->
          <view class="faq-card card">
            <text class="text-md text-bold" style="margin-bottom: 12px;">❓ 常见问题</text>
            <view v-for="faq in faqs" :key="faq.q" class="faq-item">
              <text class="text-sm text-bold">{{ faq.q }}</text>
              <text class="text-xs text-light">{{ faq.a }}</text>
            </view>
          </view>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const collapsed = computed(() => userStore.sidebarCollapsed)

const features = [
  { name: '基础课程', free: true },
  { name: '全部课程', free: false },
  { name: '宠物换装', free: true },
  { name: '高级装扮', free: false },
  { name: '排行榜', free: true },
  { name: '挑战赛', free: false },
  { name: '学习报告', free: true },
  { name: 'AI辅导', free: false }
]

const faqs = [
  { q: 'VIP可以退款吗？', a: '购买后7天内可以申请退款。' },
  { q: '多设备可以使用吗？', a: 'VIP支持同一家庭下的多台设备。' },
  { q: '到期后数据会丢失吗？', a: '学习数据永久保留，VIP内容将不可访问。' }
]

function toggleSidebar() { userStore.toggleSidebar() }
function goHome() { uni.reLaunch({ url: '/pages/main/index' }) }

function buyPlan(plan) {
  uni.showToast({ title: `即将开通${plan === 'monthly' ? '月卡' : plan === 'annual' ? '年卡' : '永久'}会员`, icon: 'none' })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.vip-page {
  display: flex;
  width: 100vw;
  height: 100vh;
  background: $dark;
  overflow: hidden;
}

.sidebar {
  width: $sidebar-width;
  min-width: $sidebar-width;
  height: 100vh;
  background: $dark2;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
  transition: all $transition-normal;
  overflow: hidden;

  &.collapsed { width: $sidebar-collapsed; min-width: $sidebar-collapsed; }
}

.sidebar-header { padding: 16px; display: flex; align-items: center; gap: 12px; }
.hamburger {
  width: 36px; height: 36px; display: flex; align-items: center; justify-content: center;
  border-radius: $radius; background: rgba(255, 255, 255, 0.1); cursor: pointer; flex-shrink: 0;
  &:active { transform: scale(0.9); }
}
.hamburger-icon { font-size: 18px; color: $white; }
.brand { display: flex; align-items: center; gap: 8px; }
.brand-emoji { font-size: 32px; }
.brand-text { display: flex; flex-direction: column; }
.brand-name { font-size: 18px; font-weight: bold; color: $white; }
.brand-sub { font-size: 11px; color: rgba(255, 255, 255, 0.4); }

.sidebar-nav { flex: 1; padding: 8px 12px; display: flex; flex-direction: column; gap: 4px; }
.nav-item {
  display: flex; align-items: center; gap: 12px; padding: 12px 16px; border-radius: $radius;
  cursor: pointer; white-space: nowrap;
  .nav-icon { font-size: 22px; width: 28px; text-align: center; }
  .nav-label { font-size: 15px; color: rgba(255, 255, 255, 0.6); }
  &.active { background: rgba(255, 215, 0, 0.1); .nav-label { color: $gold; } }
}

.sidebar-footer { padding: 12px; }
.sidebar-divider { height: 1px; background: rgba(255, 255, 255, 0.06); margin-bottom: 12px; }
.user-area { display: flex; align-items: center; gap: 10px; padding: 8px; border-radius: $radius; }
.user-avatar {
  width: 44px; height: 44px; border-radius: 50%;
  background: linear-gradient(135deg, $gold, #FFA500);
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.avatar-emoji { font-size: 24px; }
.user-info { display: flex; flex-direction: column; }
.user-name { font-size: 14px; font-weight: 600; color: rgba(255, 255, 255, 0.8); }
.user-level { font-size: 11px; color: rgba(255, 255, 255, 0.4); }

.vip-main { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.content-scroll { flex: 1; height: 0; }
.vip-content { padding: 24px 28px; display: flex; flex-direction: column; gap: 20px; }

.hero-banner {
  text-align: center;
  padding: 32px;
  background: linear-gradient(135deg, #1A1A2E, #16213E);
  border-radius: $radius-md;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.hero-crown { font-size: 64px; }
.text-gold { color: $gold; }

.plan-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.plan-card {
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  position: relative;

  &.featured {
    border-color: $gold;
    background: rgba(255, 215, 0, 0.08);
  }
}

.featured-tag {
  position: absolute;
  top: -1px; right: 16px;
  background: $gold;
  padding: 2px 12px;
  border-radius: 0 0 8px 8px;
}

.plan-name { font-size: 16px; color: rgba(255, 255, 255, 0.7); }
.plan-price { display: flex; align-items: baseline; gap: 4px; }

.compare-card {
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.compare-header, .compare-row {
  display: flex;
  padding: 8px 0;
  & + .compare-row { border-top: 1px solid rgba(255, 255, 255, 0.04); }
}

.compare-col {
  flex: 1;
  text-align: center;
  &.gold { color: $gold; font-weight: 600; }
}

.faq-card {
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.faq-item {
  padding: 10px 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  & + .faq-item { border-top: 1px solid rgba(255, 255, 255, 0.04); }
}
</style>
