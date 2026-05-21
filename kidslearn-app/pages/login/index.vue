<template>
  <view class="login-container">
    <view class="login-box">
      <view class="logo-area">
        <text class="logo-emoji">🚀</text>
        <text class="logo-text">KidsLearn</text>
      </view>

      <view class="form-area">
        <view class="input-group">
          <text class="input-icon">👤</text>
          <input class="input-field" v-model="loginForm.username" placeholder="请输入账号" placeholder-class="ph-color" />
        </view>
        <view class="input-group">
          <text class="input-icon">🔒</text>
          <input class="input-field" v-model="loginForm.password" type="password" placeholder="请输入密码" placeholder-class="ph-color" />
        </view>
        <view v-if="loginError" class="error-msg">{{ loginError }}</view>
      </view>

      <view class="btn-area">
        <button class="primary-btn" @tap="handleLogin">登 录</button>
      </view>
      <view class="link-area">
        <text class="link-text" @tap="goRegister">没有账号？去注册</text>
      </view>
    </view>
    <GlobalLoadingOverlay />
  </view>
</template>

<script>
import { login } from '@/api/auth'
import { useUserStore } from '@/store/user'
import { useRealtimeStore } from '@/store/realtime'
import { getPostAuthRedirectUrl } from '@/utils/onboardingFlow.mjs'
import { showLoading, hideLoading } from '@/utils/loading'
import GlobalLoadingOverlay from '@/components/common/GlobalLoadingOverlay.vue'

export default {
  data() {
    return {
      loginForm: { username: '', password: '' },
      loginError: '',
    }
  },
  methods: {
    async handleLogin() {
      const { username, password } = this.loginForm
      if (!username) { this.loginError = '请输入账号'; return }
      if (!password) { this.loginError = '请输入密码'; return }
      this.loginError = ''
      
      try {
        // 使用拦截器自动处理加载状态
        const res = await login({ username, password, loginType: 1 }, { 
          showLoading: '正在进入星球...', 
          loadingMascot: '🚀' 
        })
        
        const userStore = useUserStore()
        userStore.setToken(res.accessToken, res.refreshToken)
        if (res.userInfo) {
          userStore.setUserInfo(res.userInfo)
        }
        useRealtimeStore().connect()
        uni.reLaunch({ url: getPostAuthRedirectUrl(res.userInfo) })
      } catch (e) {
        this.loginError = e.msg || '登录失败，请检查账号密码'
      }
    },
    goRegister() {
      uni.navigateTo({ url: '/pages/login/register' })
    }
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  padding: 20px;
}
.login-box {
  width: 100%;
  max-width: 400px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 24px;
  padding: 40px 30px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
}
.logo-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 40px;
}
.logo-emoji {
  font-size: 64px;
  margin-bottom: 10px;
}
.logo-text {
  font-size: 28px;
  font-weight: 800;
  color: #333;
  letter-spacing: 2px;
}
.form-area {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 30px;
}
.input-group {
  display: flex;
  align-items: center;
  background: #f5f7fa;
  border-radius: 16px;
  padding: 12px 20px;
}
.input-icon {
  font-size: 20px;
  margin-right: 12px;
}
.input-field {
  flex: 1;
  font-size: 16px;
  height: 24px;
  min-height: 24px;
  border: none;
  background: transparent;
  outline: none;
}
.ph-color {
  color: #a0aab5;
}
.error-msg {
  color: #ff4d4f;
  font-size: 14px;
  text-align: center;
  margin-top: -10px;
}
.btn-area {
  margin-bottom: 20px;
}
.primary-btn {
  width: 100%;
  background: linear-gradient(135deg, #FF6B6B, #FF8E8B);
  color: white;
  font-size: 18px;
  font-weight: bold;
  border: none;
  border-radius: 16px;
  padding: 14px 0;
  line-height: 1.5;
}
.primary-btn::after {
  border: none;
}
.primary-btn:active {
  transform: scale(0.98);
  opacity: 0.9;
}
.link-area {
  text-align: center;
}
.link-text {
  color: #666;
  font-size: 14px;
}
</style>
