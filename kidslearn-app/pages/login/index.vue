<template>
  <view class="login-container">
    <view class="login-box">
      <view class="logo-area">
        <image class="login-world-art" src="/static/redesign/mission-island.png" mode="aspectFill" />
        <view class="login-brand-copy">
          <text class="logo-text">趣学星球</text>
          <text class="logo-subtitle">开启今天的学习航线</text>
        </view>
      </view>

      <view class="form-area">
        <view class="input-group">
          <text class="input-label">账号</text>
          <input class="input-field" v-model="loginForm.username" placeholder="请输入账号" placeholder-class="ph-color" />
        </view>
        <view class="input-group">
          <text class="input-label">密码</text>
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

<script setup>
import { reactive, ref } from 'vue'
import { login } from '@/api/auth'
import { useUserStore } from '@/store/user'
import { useRealtimeStore } from '@/store/realtime'
import { getPostAuthRedirectUrl } from '@/utils/onboardingFlow.mjs'
import GlobalLoadingOverlay from '@/components/common/GlobalLoadingOverlay.vue'

const loginForm = reactive({ username: '', password: '' })
const loginError = ref('')

async function handleLogin() {
  const { username, password } = loginForm
  if (!username) { loginError.value = '请输入账号'; return }
  if (!password) { loginError.value = '请输入密码'; return }
  loginError.value = ''

  try {
    const res = await login({ username, password }, {
      showLoading: '正在进入星球...',
      loadingMascot: ''
    })

    const userStore = useUserStore()
    userStore.setToken(res.accessToken, res.refreshToken)
    if (res.userInfo) {
      userStore.setUserInfo(res.userInfo)
    }
    useRealtimeStore().connect()
    uni.reLaunch({ url: getPostAuthRedirectUrl(res.userInfo) })
  } catch (e) {
    loginError.value = e.msg || '登录失败，请检查账号密码'
  }
}

function goRegister() {
  uni.navigateTo({ url: '/pages/login/register' })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  padding: 20px;
  padding-bottom: $safe-area-bottom;
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
  position: relative;
  min-height: 178px;
  overflow: hidden;
  border-radius: 22px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 28px;
}

.login-world-art { position: absolute; inset: 0; width: 100%; height: 100%; }
.logo-area::after { content: ''; position: absolute; inset: 0; background: linear-gradient(180deg, rgba(255,255,255,.04), rgba(255,255,255,.94)); }
.login-brand-copy { position: relative; z-index: 2; display: flex; flex-direction: column; align-items: center; gap: 3px; margin-bottom: 16px; }
.logo-subtitle { color: #5D6A7A; font-size: 12px; }

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

.input-label { width: 40px; margin-right: 10px; color: #315EBA; font-size: 12px; font-weight: 850; }

.input-icon {
  font-size: 20px;
  margin-right: 12px;
}

.input-field {
  flex: 1;
  font-size: $mobile-font-base; /* 防止iOS自动缩放 */
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
  background: linear-gradient(135deg, $primary, $primary-light);
  color: white;
  font-size: 18px;
  font-weight: bold;
  border: none;
  border-radius: 16px;
  padding: 14px 0;
  line-height: 1.5;
  min-height: $tap-target-lg;
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

/* 响应式适配 */
@include respond-sm {
  .login-box {
    padding: 30px 20px;
    border-radius: 20px;
  }

  .logo-emoji {
    font-size: 48px;
  }

  .logo-text {
    font-size: 24px;
  }

  .input-group {
    padding: 14px 16px;
  }

  .input-field {
    font-size: $mobile-font-base;
  }

  .primary-btn {
    min-height: 56px;
    font-size: 16px;
  }
}
</style>
