<template>
  <view class="mine-page">
    <view class="user-card">
      <image class="user-avatar" src="/static/default-avatar.png" mode="aspectFill" />
      <view class="user-info">
        <text class="user-name">小明</text>
        <text class="user-level">Lv.5 好奇宝宝</text>
      </view>
    </view>
    <view class="stats-row">
      <view class="stat-item"><text class="stat-num">1280</text><text class="stat-label">金币</text></view>
      <view class="stat-item"><text class="stat-num">56</text><text class="stat-label">星星</text></view>
      <view class="stat-item"><text class="stat-num">12</text><text class="stat-label">贴纸</text></view>
    </view>
    <view class="menu-list">
      <view class="menu-item" v-for="item in menus" :key="item.label" @click="handleMenu(item)">
        <text class="menu-icon">{{ item.icon }}</text>
        <text class="menu-label">{{ item.label }}</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      menus: [
        { icon: '📋', label: '学习记录', url: '' },
        { icon: '📖', label: '错题本', url: '' },
        { icon: '👥', label: '好友列表', url: '' },
        { icon: '👑', label: 'VIP会员', url: '' },
        { icon: '👨‍👩‍👧', label: '家长中心', url: '/pages/parent/index' },
        { icon: '⚙️', label: '设置', url: '' },
      ],
    }
  },
  methods: {
    handleMenu(item) {
      if (item.url) {
        uni.navigateTo({ url: item.url })
      }
    },
  },
}
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

.stats-row {
  display: flex;
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx;
  margin: 24rpx 0;
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
