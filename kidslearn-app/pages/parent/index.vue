<template>
  <view class="parent-page">
    <view class="header">
      <text class="title">家长中心</text>
    </view>
    <view class="menu-list">
      <view class="menu-card" v-for="item in menus" :key="item.label">
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
        { icon: '📊', label: '学习报告' },
        { icon: '⏰', label: '时间管理' },
        { icon: '🔒', label: '内容管控' },
        { icon: '📋', label: '使用记录' },
        { icon: '👨‍👩‍👧', label: '家庭组管理' },
        { icon: '🔔', label: '消息通知' },
      ],
    }
  },
}
</script>

<style lang="scss" scoped>
.parent-page {
  min-height: 100vh;
  padding: 32rpx;
}

.header {
  margin-bottom: 32rpx;
}

.title {
  font-size: 40rpx;
  font-weight: 800;
}

.menu-list {
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;
}

.menu-card {
  display: flex;
  align-items: center;
  padding: 36rpx 32rpx;
  border-bottom: 1rpx solid #f5f5f5;
}

.menu-icon {
  font-size: 40rpx;
  margin-right: 24rpx;
}

.menu-label {
  flex: 1;
  font-size: 30rpx;
  font-weight: 500;
}

.menu-arrow {
  color: #ccc;
  font-size: 36rpx;
}
</style>
