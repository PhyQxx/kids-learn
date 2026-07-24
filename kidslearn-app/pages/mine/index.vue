<template>
  <AppLayout theme="kids" title="个人中心" :show-back="true" active-nav="home">
  <view class="mine-page">
    <view class="user-card">
      <image class="user-avatar" :src="summary.avatar" mode="aspectFill" />
      <view class="user-info">
        <text class="user-name">{{ summary.nickname }}</text>
        <text class="user-level">{{ summary.levelText }}</text>
        <text class="user-subtitle">{{ loading ? '正在同步学习档案...' : summary.petText }}</text>
      </view>
    </view>
    <view class="stats-row">
      <view v-for="stat in summary.stats" :key="stat.label" class="stat-item">
        <text class="stat-num">{{ stat.value }}</text>
        <text class="stat-label">{{ stat.label }}</text>
      </view>
    </view>
    <view class="summary-card">
      <view class="summary-item">
        <text class="summary-kind">成就</text>
        <text class="summary-text">{{ summary.achievementText }}</text>
      </view>
      <view class="summary-item">
        <text class="summary-kind">伙伴</text>
        <text class="summary-text">{{ summary.petText }}</text>
      </view>
    </view>
    <view class="menu-list">
      <view class="menu-item" v-for="item in menus" :key="item.label" @tap="handleMenu(item)">
        <view class="menu-route-mark"><text>{{ item.label.slice(0, 1) }}</text></view>
        <text class="menu-label">{{ item.label }}</text>
        <text class="menu-action">进入</text>
      </view>
    </view>
  </view>
  </AppLayout>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getUserInfo } from '@/api/user'
import { getPetStatus } from '@/api/pet'
import { getMyProgress } from '@/api/achievement'
import { useUserStore } from '@/store/user'
import { usePetStore } from '@/store/pet'
import { buildMineProfileSummary } from '@/utils/mineProfile.mjs'
import AppLayout from '@/components/AppLayout.vue'

const userStore = useUserStore()
const petStore = usePetStore()
const loading = ref(true)
const achievementProgress = ref(null)

const menus = [
  { label: '闯关记录', url: '/pages/mine/records' },
  { label: '错题本', url: '/pages/mine/wrong' },
  { label: '好友', url: '/pages/mine/friend' },
  { label: 'VIP会员', url: '/pages/mine/vip' },
  { label: '设置', url: '/pages/mine/settings' },
]

const summary = computed(() => buildMineProfileSummary({
  userInfo: userStore.userInfo,
  petInfo: petStore.petInfo,
  achievementProgress: achievementProgress.value,
}))

async function loadMineData() {
  loading.value = true
  const [userResult, petResult, achievementResult] = await Promise.allSettled([
    getUserInfo(),
    getPetStatus(),
    getMyProgress(),
  ])

  if (userResult.status === 'fulfilled' && userResult.value) {
    userStore.setUserInfo(userResult.value)
  }
  if (petResult.status === 'fulfilled' && petResult.value) {
    petStore.setPetInfo(petResult.value)
  }
  if (achievementResult.status === 'fulfilled' && achievementResult.value) {
    achievementProgress.value = achievementResult.value
  }

  loading.value = false
}

function handleMenu(item) {
  if (item.url) {
    uni.navigateTo({ url: item.url })
    return
  }
  uni.showToast({ title: '功能建设中', icon: 'none' })
}

onShow(loadMineData)
</script>

<style lang="scss" scoped>
.mine-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  min-height: 220px;
  background-image: linear-gradient(90deg, rgba(24,33,47,.88), rgba(24,33,47,.18)), url('/static/redesign/pet-habitat.png');
  background-size: cover;
  background-position: center;
  padding: 40rpx 32rpx;
  border-radius: 24rpx;
}

.user-info {
  min-width: 0;
}

.user-avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  border: 4rpx solid #fff;
}

.user-name {
  font-size: 34rpx;
  font-weight: 700;
  color: #fff;
  display: block;
}

.user-level {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
}

.user-subtitle {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.78);
  display: block;
}

.stats-row {
  display: flex;
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx;
  margin: 0;
  border: 1px solid rgba(63,111,229,.09);
  box-shadow: 0 9px 24px rgba(69,91,124,.07);
}

.stat-item {
  flex: 1;
  text-align: center;
}

.stat-num {
  font-size: 34rpx;
  font-weight: 700;
  color: #FF6B6B;
  display: block;
}

.stat-label {
  font-size: 24rpx;
  color: #999;
}

.summary-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 24rpx 32rpx;
  margin-bottom: 0;
  border: 1px solid rgba(63,111,229,.09);
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 10rpx 0;
}

.summary-icon {
  font-size: 32rpx;
}

.summary-kind,
.menu-route-mark {
  min-width: 58rpx;
  height: 58rpx;
  margin-right: 18rpx;
  border-radius: 18rpx;
  background: #EAF1FF;
  color: #315EBA;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20rpx;
  font-weight: 850;
}

.summary-text {
  font-size: 26rpx;
  color: #555;
}

.menu-list {
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 32rpx;
  border-bottom: 1rpx solid #f5f5f5;
}

.menu-icon {
  font-size: 36rpx;
  margin-right: 20rpx;
}

.menu-label {
  flex: 1;
  font-size: 28rpx;
}

.menu-action {
  min-width: 72rpx;
  padding: 8rpx 16rpx;
  border-radius: 999px;
  background: #EEF3FF;
  color: #315EBA;
  font-size: 20rpx;
  font-weight: 800;
  text-align: center;
}
</style>
