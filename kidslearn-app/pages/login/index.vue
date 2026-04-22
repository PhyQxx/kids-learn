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

    <!-- 登录表单 -->
    <view class="login-form" v-if="showLogin">
      <view class="form-group">
        <text class="form-label">账号</text>
        <input class="form-input" v-model="loginForm.username" placeholder="请输入账号" placeholder-class="input-placeholder" />
      </view>
      <view class="form-group">
        <text class="form-label">密码</text>
        <input class="form-input" v-model="loginForm.password" placeholder="请输入密码" placeholder-class="input-placeholder" type="password" />
      </view>
      <view v-if="loginError" class="error-text">
        <text class="text-xs">{{ loginError }}</text>
      </view>
      <view class="btn-login" @click="handleLogin">
        <text class="text-white text-md text-bold">登 录</text>
      </view>
      <view class="form-footer">
        <text class="text-sm" style="color: rgba(255,255,255,0.7);" @click="goRegister">没有账号？去注册</text>
      </view>
    </view>
  </view>
</template>

<script>
import { login } from '@/api/auth'
import { useUserStore } from '@/store/user'

export default {
  data() {
    return {
      showLogin: false,
      selectedAgeGroup: null,
      ageGroups: [
        { icon: '🧒', name: '幼幼组 (3-6岁)', desc: '幼儿园阶段 · 认字拼音数学启蒙', group: 1 },
        { icon: '👦', name: '低年龄组 (6-9岁)', desc: '小学1-3年级 · 语文数学英语', group: 2 },
        { icon: '🧑', name: '高年龄组 (9-12岁)', desc: '小学4-6年级 · 进阶学习挑战', group: 3 },
      ],
      loginForm: { username: '', password: '' },
      loginError: '',
    }
  },
  methods: {
    selectAge(group) {
      this.selectedAgeGroup = group.group
      this.showLogin = true
    },
    async handleLogin() {
      const { username, password } = this.loginForm
      if (!username) { this.loginError = '请输入账号'; return }
      if (!password) { this.loginError = '请输入密码'; return }
      this.loginError = ''
      uni.showLoading({ title: '登录中...' })
      try {
        const res = await login({ username, password, loginType: 1 })
        const userStore = useUserStore()
        userStore.setToken(res.accessToken)
        if (res.userInfo) {
          userStore.setUserInfo(res.userInfo)
        }
        uni.hideLoading()
        uni.reLaunch({ url: '/pages/main/index' })
      } catch (e) {
        uni.hideLoading()
        this.loginError = e?.msg || '登录失败，请检查账号密码'
      }
    },
    goRegister() {
      uni.navigateTo({ url: `/pages/login/register?learnAgeGroup=${this.selectedAgeGroup || 2}` })
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

/* 登录表单 */
.login-form {
  width: 100%;
  background: #fff;
  border-radius: 24rpx;
  padding: 48rpx 40rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.1);
}

.form-group {
  margin-bottom: 28rpx;
}

.form-label {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  display: block;
  margin-bottom: 12rpx;
}

.form-input {
  width: 100%;
  height: 88rpx;
  background: #F5F5F5;
  border-radius: 16rpx;
  padding: 0 28rpx;
  font-size: 28rpx;
}

.btn-login {
  width: 100%;
  height: 88rpx;
  background: linear-gradient(135deg, #FF6B6B, #FF8E8E);
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 16rpx;
  box-shadow: 0 8rpx 20rpx rgba(255, 107, 107, 0.3);
}

.btn-login:active {
  opacity: 0.9;
  transform: scale(0.98);
}

.error-text {
  margin: 12rpx 0;
  color: #FF4757;
}

.form-footer {
  text-align: center;
  margin-top: 24rpx;
}
</style>
