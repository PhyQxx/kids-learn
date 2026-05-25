<template>
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
        <text class="summary-icon">🏅</text>
        <text class="summary-text">{{ summary.achievementText }}</text>
      </view>
      <view class="summary-item">
        <text class="summary-icon">🐾</text>
        <text class="summary-text">{{ summary.petText }}</text>
      </view>
    </view>
    <view class="menu-list">
      <view class="menu-item" v-for="item in menus" :key="item.label" @tap="handleMenu(item)">
        <text class="menu-icon">{{ item.icon }}</text>
        <text class="menu-label">{{ item.label }}</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>
  </view>
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

const userStore = useUserStore()
const petStore = usePetStore()
const loading = ref(true)
const achievementProgress = ref(null)

const menus = [
  { icon: '📋', label: '学习记录', url: '/pages/mine/records' },
  { icon: '📖', label: '错题本', url: '/pages/mine/wrong' },
  { icon: '🛡️', label: '家长中心', url: '/pages/parent/index' },
  { icon: '👥', label: '好友列表', url: '' },
  { icon: '👑', label: 'VIP会员', url: '/pages/mine/vip' },
  { icon: '⚙️', label: '设置', url: '/pages/mine/settings' },
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
  min-height: 100vh;
  padding: 32rpx;
  padding-bottom: 180rpx;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  background: linear-gradient(135deg, #FF6B6B, #FF8E8E);
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
  margin: 24rpx 0 16rpx;
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
  margin-bottom: 24rpx;
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

.menu-arrow {
  color: #ccc;
  font-size: 32rpx;
}
</style>
