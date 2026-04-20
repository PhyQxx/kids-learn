<template>
  <view class="achieve-page">
    <view class="header">
      <text class="title">成就中心</text>
      <text class="subtitle">收集徽章，成为学习小达人！</text>
    </view>
    <view class="achieve-tabs">
      <view class="tab" :class="{ active: activeTab === 'all' }" @click="activeTab = 'all'">全部</view>
      <view class="tab" :class="{ active: activeTab === 'learn' }" @click="activeTab = 'learn'">学习</view>
      <view class="tab" :class="{ active: activeTab === 'collect' }" @click="activeTab = 'collect'">收集</view>
      <view class="tab" :class="{ active: activeTab === 'social' }" @click="activeTab = 'social'">社交</view>
    </view>
    <view class="achieve-grid">
      <view class="achieve-card" v-for="item in achievements" :key="item.id">
        <text class="achieve-icon">{{ item.icon }}</text>
        <text class="achieve-name">{{ item.name }}</text>
        <text class="achieve-desc">{{ item.desc }}</text>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      activeTab: 'all',
      achievements: [
        { id: 1, icon: '🌟', name: '初次闯关', desc: '完成第一个关卡' },
        { id: 2, icon: '🔥', name: '连续7天', desc: '连续登录7天' },
        { id: 3, icon: '📝', name: '语文小达人', desc: '完成10个语文关卡' },
        { id: 4, icon: '🔢', name: '数学小达人', desc: '完成10个数学关卡' },
        { id: 5, icon: '⭐', name: '三星满分', desc: '获得10次三星评价' },
        { id: 6, icon: '🏆', name: '排行榜冠军', desc: '排行榜第一名' },
      ],
    }
  },
}
</script>

<style lang="scss" scoped>
.achieve-page {
  min-height: 100vh;
  padding: 32rpx;
  padding-bottom: 180rpx;
}

.header {
  margin-bottom: 24rpx;
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

.achieve-tabs {
  display: flex;
  gap: 16rpx;
  margin-bottom: 32rpx;
}

.tab {
  padding: 12rpx 32rpx;
  border-radius: 32rpx;
  font-size: 26rpx;
  background: #fff;
  color: #666;
}

.tab.active {
  background: #FF6B6B;
  color: #fff;
}

.achieve-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 24rpx;
}

.achieve-card {
  width: calc(50% - 12rpx);
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx;
  text-align: center;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);
}

.achieve-icon {
  font-size: 64rpx;
  display: block;
}

.achieve-name {
  font-size: 28rpx;
  font-weight: 700;
  display: block;
  margin-top: 12rpx;
}

.achieve-desc {
  font-size: 22rpx;
  color: #999;
  margin-top: 4rpx;
}
</style>
