<template>
  <view class="home-page">
    <!-- 自定义导航栏 -->
    <view class="status-bar" :style="{ paddingTop: statusBarHeight + 'px' }"></view>
    <view class="nav-bar">
      <view class="nav-left">
        <image class="avatar" src="/static/default-avatar.png" mode="aspectFill" />
        <view class="greeting">
          <text class="greeting-text">Hi, 小明！</text>
          <text class="greeting-sub">今天也要加油哦 🌟</text>
        </view>
      </view>
      <view class="nav-right">
        <text class="coin">🪙 1,280</text>
      </view>
    </view>

    <!-- 宠物状态 -->
    <view class="card pet-status">
      <view class="flex-row" style="justify-content:space-between">
        <view class="pet-info">
          <text class="pet-name">星小猫</text>
          <text class="pet-mood">心情：开心 😊</text>
          <view class="hunger-bar">
            <view class="hunger-fill" style="width:80%"></view>
          </view>
          <text class="hunger-text">饱食度 80%</text>
        </view>
        <view class="pet-image">🐱</view>
      </view>
    </view>

    <!-- 今日任务 -->
    <view class="section">
      <text class="section-title">📚 今日任务</text>
      <view class="task-list">
        <view class="card task-card" v-for="task in tasks" :key="task.subject">
          <view class="flex-row" style="justify-content:space-between">
            <view class="task-info">
              <text class="task-icon">{{ task.icon }}</text>
              <view>
                <text class="task-name">{{ task.name }}</text>
                <text class="task-progress">{{ task.done }}/{{ task.total }}</text>
              </view>
            </view>
            <view class="task-btn" :class="{ 'task-done': task.done >= task.total }">
              {{ task.done >= task.total ? '已完成' : '去学习' }}
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 继续学习 -->
    <view class="section">
      <text class="section-title">⭐ 继续学习</text>
      <view class="card">
        <view class="flex-row" style="justify-content:space-between">
          <view>
            <text class="continue-name">第3关 认识数字</text>
            <text class="continue-sub">进度 60%</text>
          </view>
          <view class="btn-primary" style="padding:0 32rpx;height:64rpx;line-height:64rpx;font-size:28rpx">
            继续
          </view>
        </view>
        <view class="progress-bar">
          <view class="progress-fill" style="width:60%"></view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      statusBarHeight: 44,
      tasks: [
        { icon: '📝', name: '语文', subject: 'chinese', done: 2, total: 3 },
        { icon: '🔢', name: '数学', subject: 'math', done: 1, total: 3 },
        { icon: '🔤', name: '英语', subject: 'english', done: 0, total: 3 },
        { icon: '🧩', name: '逻辑', subject: 'logic', done: 0, total: 2 },
      ],
    }
  },
  onLoad() {
    const sysInfo = uni.getSystemInfoSync()
    this.statusBarHeight = sysInfo.statusBarHeight || 44
  },
}
</script>

<style lang="scss" scoped>
@import '../../common.scss';

.home-page {
  min-height: 100vh;
  background: $bg;
  padding-bottom: 180rpx;
}

.status-bar {
  background: #fff;
}

.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 32rpx;
  background: #fff;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, $primary, $primary-light);
}

.greeting-text {
  font-size: 32rpx;
  font-weight: 700;
  color: $text;
}

.greeting-sub {
  font-size: 24rpx;
  color: $text-light;
}

.coin {
  font-size: 28rpx;
  font-weight: 700;
  color: $accent;
}

.pet-status {
  margin: 24rpx 32rpx;
}

.pet-image {
  font-size: 96rpx;
}

.pet-name {
  font-size: 32rpx;
  font-weight: 700;
  display: block;
}

.pet-mood {
  font-size: 24rpx;
  color: $text-light;
  margin-top: 4rpx;
}

.hunger-bar {
  width: 200rpx;
  height: 12rpx;
  background: #eee;
  border-radius: 6rpx;
  margin-top: 12rpx;
}

.hunger-fill {
  height: 100%;
  background: linear-gradient(90deg, $primary, $primary-light);
  border-radius: 6rpx;
}

.hunger-text {
  font-size: 20rpx;
  color: $text-light;
  margin-top: 4rpx;
}

.section {
  margin: 24rpx 32rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 700;
  margin-bottom: 16rpx;
}

.task-info {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.task-icon {
  font-size: 44rpx;
}

.task-name {
  font-size: 28rpx;
  font-weight: 600;
  display: block;
}

.task-progress {
  font-size: 24rpx;
  color: $text-light;
}

.task-btn {
  background: linear-gradient(135deg, $primary, $primary-light);
  color: #fff;
  padding: 12rpx 32rpx;
  border-radius: 32rpx;
  font-size: 24rpx;
  font-weight: 600;
}

.task-done {
  background: #eee;
  color: $text-light;
}

.continue-name {
  font-size: 28rpx;
  font-weight: 600;
  display: block;
}

.continue-sub {
  font-size: 24rpx;
  color: $text-light;
}

.progress-bar {
  height: 12rpx;
  background: #eee;
  border-radius: 6rpx;
  margin-top: 16rpx;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, $teal, #26A69A);
  border-radius: 6rpx;
}
</style>
