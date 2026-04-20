<template>
  <view class="login-page">
    <view class="login-header">
      <text class="logo-emoji">🌟</text>
      <text class="app-title">趣学星球</text>
      <text class="app-desc">让孩子在趣味中学习成长</text>
    </view>
    <view class="age-cards" v-if="!showLogin">
      <view class="age-card" v-for="group in ageGroups" :key="group.name" @click="selectAge(group)">
        <text class="age-icon">{{ group.icon }}</text>
        <view class="age-info">
          <text class="age-name">{{ group.name }}</text>
          <text class="age-desc">{{ group.desc }}</text>
        </view>
        <text class="age-arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      showLogin: false,
      ageGroups: [
        { icon: '🧒', name: '幼幼组 (3-6岁)', desc: '幼儿园阶段 · 认字拼音数学启蒙', group: 1 },
        { icon: '👦', name: '低年龄组 (6-9岁)', desc: '小学1-3年级 · 语文数学英语', group: 2 },
        { icon: '🧑', name: '高年龄组 (9-12岁)', desc: '小学4-6年级 · 进阶学习挑战', group: 3 },
      ],
    }
  },
  methods: {
    selectAge(group) {
      // TODO: 保存选择并跳转登录
      uni.switchTab({ url: '/pages/index/index' })
    },
  },
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #FF6B6B, #FF8E8E);
  padding: 0 32rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.login-header {
  text-align: center;
  margin-bottom: 80rpx;
}

.logo-emoji {
  font-size: 120rpx;
  display: block;
}

.app-title {
  font-size: 48rpx;
  font-weight: 800;
  color: #fff;
  display: block;
  margin-top: 16rpx;
}

.app-desc {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 8rpx;
}

.age-cards {
  width: 100%;
}

.age-card {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.1);
}

.age-card:active {
  opacity: 0.9;
  transform: scale(0.98);
}

.age-icon {
  font-size: 56rpx;
  margin-right: 24rpx;
}

.age-name {
  font-size: 32rpx;
  font-weight: 700;
  display: block;
}

.age-desc {
  font-size: 24rpx;
  color: #999;
  margin-top: 4rpx;
}

.age-arrow {
  margin-left: auto;
  font-size: 36rpx;
  color: #ccc;
}
</style>
