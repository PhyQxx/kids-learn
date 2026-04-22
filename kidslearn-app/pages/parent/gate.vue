<template>
  <view class="gate-page">
    <!-- 装饰背景 -->
    <view class="bg-decor">
      <view class="decor-circle c1"></view>
      <view class="decor-circle c2"></view>
      <view class="decor-circle c3"></view>
    </view>

    <!-- 验证卡片 -->
    <view class="gate-card animate-slide-up">
      <text class="gate-icon">🛡️</text>
      <text class="gate-title text-title text-bold">家长验证</text>
      <text class="gate-desc text-light text-sm">请完成验证以进入家长模式</text>

      <!-- 验证方式切换 -->
      <tn-tabs v-model="verifyTabIndex" active-color="#4ECDC4">
        <tn-tabs-item title="密码验证" />
        <tn-tabs-item title="短信验证" />
      </tn-tabs>

      <!-- 密码验证 -->
      <view v-if="verifyTabIndex === 0" class="verify-form">
        <input class="input" v-model="phone" placeholder="手机号" placeholder-class="input-placeholder" />
        <input class="input" v-model="password" type="password" placeholder="密码" placeholder-class="input-placeholder" />
        <tn-button type="primary" size="lg" shape="round" block @click="verify" style="background: linear-gradient(135deg, #4ECDC4, #26A69A);">验证</tn-button>
      </view>

      <!-- 短信验证 -->
      <view v-if="verifyTabIndex === 1" class="verify-form">
        <input class="input" v-model="phone" placeholder="手机号" placeholder-class="input-placeholder" />
        <view class="sms-row">
          <input class="input" v-model="code" placeholder="验证码" placeholder-class="input-placeholder" style="flex: 1;" />
          <tn-button size="sm" shape="round" @click="sendCode">{{ codeSent ? `${countdown}s` : '发送验证码' }}</tn-button>
        </view>
        <tn-button type="primary" size="lg" shape="round" block @click="verify" style="background: linear-gradient(135deg, #4ECDC4, #26A69A);">验证</tn-button>
      </view>

      <text v-if="errorMsg" class="error-text text-xs text-error">{{ errorMsg }}</text>

      <view class="gate-links">
        <text class="text-primary text-sm" @tap="forgotPassword">忘记密码</text>
        <text class="text-light text-sm">|</text>
        <text class="text-light text-sm" @tap="cancel">取消</text>
      </view>
    </view>

    <!-- 验证成功 -->
    <view v-if="showSuccess" class="success-overlay">
      <view class="success-card animate-pop-in">
        <text class="success-icon">✅</text>
        <text class="text-xl text-bold">验证成功</text>
        <text class="text-sm text-light">正在进入家长模式...</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { useUserStore } from '@/store/user'
import { login } from '@/api/auth'

const userStore = useUserStore()

const verifyTabIndex = ref(0)
const phone = ref('')
const password = ref('')
const code = ref('')
const codeSent = ref(false)
const countdown = ref(60)
const errorMsg = ref('')
const showSuccess = ref(false)

async function verify() {
  if (!phone.value) { errorMsg.value = '请输入手机号'; return }
  if (verifyTabIndex.value === 0 && !password.value) { errorMsg.value = '请输入密码'; return }
  if (verifyTabIndex.value === 1 && !code.value) { errorMsg.value = '请输入验证码'; return }

  errorMsg.value = ''
  uni.showLoading({ title: '验证中...' })

  try {
    const res = await login({ username: phone.value, password: password.value, loginType: 2 })
    // 家长登录成功
    userStore.setToken(res.accessToken)
    if (res.userInfo) {
      userStore.setUserInfo(res.userInfo)
    }
    uni.hideLoading()
    showSuccess.value = true
    setTimeout(() => {
      userStore.setParentMode(true)
      uni.reLaunch({ url: '/pages/main/index' })
    }, 1500)
  } catch (e) {
    uni.hideLoading()
    errorMsg.value = e?.msg || '验证失败，请检查账号密码'
  }
}

function sendCode() {
  if (codeSent.value) return
  codeSent.value = true
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
      codeSent.value = false
      countdown.value = 60
    }
  }, 1000)
}

function forgotPassword() { uni.showToast({ title: '请联系管理员重置密码', icon: 'none' }) }
function cancel() { uni.navigateBack() }
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.gate-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #FFF5F5, #FFF9F9, #F0FAF9);
  position: relative;
  overflow: hidden;
}

.bg-decor {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  pointer-events: none;
}

.decor-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.15;
  &.c1 { width: 300px; height: 300px; background: $primary; top: -80px; right: -60px; }
  &.c2 { width: 200px; height: 200px; background: $teal; bottom: -40px; left: 10%; }
  &.c3 { width: 150px; height: 150px; background: $primary-light; top: 40%; left: -40px; }
}

.gate-card {
  width: 440px;
  background: $white;
  border-radius: $radius-xl;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
  padding: 36px;
  text-align: center;
  z-index: 1;
}

.gate-icon { font-size: 64px; display: block; margin-bottom: 12px; }
.gate-title { display: block; margin-bottom: 4px; }
.gate-desc { display: block; margin-bottom: 20px; }

.verify-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sms-row {
  display: flex;
  gap: 8px;
}

.error-text {
  display: block;
  margin-top: 8px;
}

.gate-links {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}

.success-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.success-card {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.success-icon { font-size: 64px; }
</style>
