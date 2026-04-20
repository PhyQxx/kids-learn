<template>
  <view class="learn-page">
    <view class="header">
      <text class="title">学习中心</text>
      <text class="subtitle">选择你想学习的科目吧！</text>
    </view>
    <view class="subject-grid">
      <view class="subject-card" v-for="item in subjects" :key="item.code"
        :style="{ background: item.bgColor }" @click="goSubject(item)">
        <text class="subject-icon">{{ item.icon }}</text>
        <text class="subject-name">{{ item.name }}</text>
        <text class="subject-count">{{ item.courseCount }}门课程</text>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      subjects: [
        { code: 'CHINESE', name: '语文', icon: '📝', bgColor: '#FFF0F0', courseCount: 12 },
        { code: 'MATH', name: '数学', icon: '🔢', bgColor: '#E8FFF8', courseCount: 15 },
        { code: 'ENGLISH', name: '英语', icon: '🔤', bgColor: '#FFFBE6', courseCount: 10 },
        { code: 'LOGIC', name: '逻辑', icon: '🧩', bgColor: '#F0EAFF', courseCount: 8 },
        { code: 'SCIENCE', name: '科学', icon: '🔬', bgColor: '#E8F8E8', courseCount: 6 },
        { code: 'MUSIC', name: '音乐', icon: '🎵', bgColor: '#FFF3E0', courseCount: 5 },
      ],
    }
  },
  methods: {
    goSubject(item) {
      uni.navigateTo({ url: `/pages/learn/courses?subject=${item.code}` })
    },
  },
}
</script>

<style lang="scss" scoped>
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

.subject-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 24rpx;
}

.subject-card {
  width: calc(50% - 12rpx);
  padding: 40rpx 32rpx;
  border-radius: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);
}

.subject-icon {
  font-size: 64rpx;
  display: block;
}

.subject-name {
  font-size: 32rpx;
  font-weight: 700;
  margin-top: 16rpx;
  display: block;
}

.subject-count {
  font-size: 24rpx;
  color: #999;
  margin-top: 4rpx;
}
</style>
