<template>
  <view class="learn-page">
    <view class="header">
      <text class="title">学习中心</text>
      <text class="subtitle">选择一个模式开始吧！</text>
    </view>
    <view class="mode-grid">
      <view class="mode-card" style="background: #E8F0FE;" @click="goMode('level')">
        <text class="mode-icon">⚔️</text>
        <text class="mode-name">闯关模式</text>
        <text class="mode-desc">挑战关卡，赢取星星</text>
      </view>
      <view class="mode-card" style="background: #E8FFF8;" @click="goMode('practice')">
        <text class="mode-icon">📝</text>
        <text class="mode-name">专项练习</text>
        <text class="mode-desc">巩固知识，无限刷题</text>
      </view>
      <view class="mode-card" style="background: #FFF0F0;" @click="goMode('review')">
        <text class="mode-icon">💡</text>
        <text class="mode-name">错题回顾</text>
        <text class="mode-desc">智能复习，消灭错题</text>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  methods: {
    goMode(mode) {
      if (mode === 'level') {
        // After refactor, user should select subject first
        uni.navigateTo({ url: '/pages/learn/subjects' });
      } else if (mode === 'practice') {
        // After refactor, user should select subject first
        uni.navigateTo({ url: '/pages/learn/subjects?practice=true' });
      } else if (mode === 'review') {
        uni.navigateTo({ url: '/pages/mine/wrong' });
      }
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.learn-page {
  min-height: 100vh;
  padding: 32rpx;
  padding-bottom: 180rpx;
}

.header {
  margin-bottom: 32rpx;
}

.title {
  font-size: 40rpx;
  font-weight: 800;
  display: block;
}

.subtitle {
  font-size: 26rpx;
  color: #999;
  margin-top: 8rpx;
}

.mode-grid {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.mode-card {
  padding: 40rpx 32rpx;
  border-radius: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);
}

.mode-icon {
  font-size: 64rpx;
  display: block;
}

.mode-name {
  font-size: 32rpx;
  font-weight: 700;
  margin-top: 16rpx;
  display: block;
}

.mode-desc {
  font-size: 24rpx;
  color: #666;
  margin-top: 4rpx;
}
</style>
