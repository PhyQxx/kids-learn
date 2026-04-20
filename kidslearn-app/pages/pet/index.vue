<template>
  <view class="pet-page">
    <view class="pet-display">
      <view class="pet-emoji">🐱</view>
      <text class="pet-name">星小猫</text>
      <text class="pet-level">Lv.5</text>
    </view>
    <view class="pet-stats">
      <view class="stat-item">
        <text class="stat-label">饱食度</text>
        <view class="stat-bar"><view class="stat-fill hunger" style="width:80%"></view></view>
        <text class="stat-value">80%</text>
      </view>
      <view class="stat-item">
        <text class="stat-label">心情</text>
        <text class="stat-mood">😊 开心</text>
      </view>
    </view>
    <view class="pet-actions">
      <view class="action-btn">🍖 喂食</view>
      <view class="action-btn">👗 换装</view>
      <view class="action-btn">🎮 玩耍</view>
      <view class="action-btn">📸 合影</view>
    </view>
  </view>
</template>

<script>
export default {}
</script>

<style lang="scss" scoped>
.pet-page {
  min-height: 100vh;
  padding: 32rpx;
  padding-bottom: 180rpx;
}

.pet-display {
  text-align: center;
  padding: 48rpx 0;
}

.pet-emoji {
  font-size: 160rpx;
}

.pet-name {
  font-size: 36rpx;
  font-weight: 700;
  display: block;
  margin-top: 16rpx;
}

.pet-level {
  font-size: 26rpx;
  color: #FF6B6B;
  margin-top: 8rpx;
}

.pet-stats {
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 32rpx;
}

.stat-item {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}

.stat-label {
  width: 120rpx;
  font-size: 28rpx;
  color: #666;
}

.stat-bar {
  flex: 1;
  height: 16rpx;
  background: #eee;
  border-radius: 8rpx;
  margin: 0 16rpx;
}

.stat-fill {
  height: 100%;
  border-radius: 8rpx;
}

.hunger {
  background: linear-gradient(90deg, #FF6B6B, #FF8E8E);
}

.stat-value {
  font-size: 26rpx;
  color: #999;
}

.stat-mood {
  font-size: 28rpx;
}

.pet-actions {
  display: flex;
  gap: 24rpx;
  flex-wrap: wrap;
}

.action-btn {
  flex: 1;
  min-width: calc(50% - 12rpx);
  height: 96rpx;
  line-height: 96rpx;
  text-align: center;
  background: #fff;
  border-radius: 24rpx;
  font-size: 30rpx;
  font-weight: 600;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);
}

.action-btn:active {
  opacity: 0.8;
}
</style>
