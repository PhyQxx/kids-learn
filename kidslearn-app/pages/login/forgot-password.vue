<template>
  <view class="reset-page">
    <view class="reset-card">
      <text class="reset-title">找回密码</text>
      <text class="reset-desc">验证绑定手机号后设置新密码</text>
      <input class="reset-input" v-model="phone" type="number" maxlength="11" placeholder="绑定手机号" />
      <view class="code-row">
        <input class="reset-input code-input" v-model="code" type="number" maxlength="6" placeholder="6位验证码" />
        <button class="code-button" :disabled="countdown > 0" @tap="sendCode">
          {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
        </button>
      </view>
      <input class="reset-input" v-model="newPassword" type="password" password placeholder="新密码（6-50位）" />
      <input class="reset-input" v-model="confirmPassword" type="password" password placeholder="再次输入新密码" />
      <text v-if="errorText" class="reset-error">{{ errorText }}</text>
      <button class="reset-submit" :disabled="submitting" @tap="submitReset">重置密码</button>
      <text class="back-link" @tap="goBack">返回登录</text>
    </view>
  </view>
</template>

<script setup>
import { onBeforeUnmount, ref } from 'vue'
import { resetForgottenPassword, sendForgotPasswordCode } from '@/api/auth'

const phone = ref('')
const code = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const errorText = ref('')
const submitting = ref(false)
const countdown = ref(0)
let timer

onBeforeUnmount(() => { if (timer) clearInterval(timer) })

async function sendCode() {
  if (!/^1[3-9]\d{9}$/.test(phone.value)) {
    errorText.value = '请输入正确的手机号'
    return
  }
  try {
    await sendForgotPasswordCode(phone.value)
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0) { clearInterval(timer); timer = null }
    }, 1000)
    uni.showToast({ title: '验证码已发送', icon: 'success' })
  } catch (error) {
    errorText.value = error?.msg || error?.message || '发送失败'
  }
}

async function submitReset() {
  if (!/^1[3-9]\d{9}$/.test(phone.value)) { errorText.value = '请输入正确的手机号'; return }
  if (!/^\d{6}$/.test(code.value)) { errorText.value = '请输入6位验证码'; return }
  if (newPassword.value.length < 6 || newPassword.value.length > 50) { errorText.value = '新密码长度必须为6-50位'; return }
  if (newPassword.value !== confirmPassword.value) { errorText.value = '两次输入的密码不一致'; return }
  submitting.value = true
  errorText.value = ''
  try {
    await resetForgottenPassword(phone.value, code.value, newPassword.value)
    uni.showToast({ title: '密码已重置', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 800)
  } catch (error) {
    errorText.value = error?.msg || error?.message || '重置失败'
  } finally {
    submitting.value = false
  }
}

function goBack() { uni.navigateBack() }
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.reset-page { min-height: 100vh; padding: 28px; display: flex; align-items: center; justify-content: center; box-sizing: border-box; background: linear-gradient(135deg, #a8edea, #fed6e3); }
.reset-card { width: min(420px, 100%); padding: 32px; border-radius: 24px; background: rgba(255,255,255,.94); box-shadow: 0 18px 48px rgba(50,75,95,.14); display: flex; flex-direction: column; gap: 14px; box-sizing: border-box; }
.reset-title { font-size: 26px; font-weight: 850; color: $text; }
.reset-desc { margin-bottom: 8px; color: $text-light; font-size: 13px; }
.reset-input { height: 46px; padding: 0 14px; border-radius: 14px; background: #F5F7FA; color: $text; box-sizing: border-box; }
.code-row { display: flex; gap: 10px; }
.code-input { flex: 1; }
.code-button { width: 126px; height: 46px; line-height: 46px; margin: 0; padding: 0; border-radius: 14px; background: #EEF3FF; color: #315EBA; font-size: 13px; }
.reset-submit { height: 48px; line-height: 48px; margin: 6px 0 0; border-radius: 14px; background: $primary; color: #fff; font-weight: 800; }
.reset-error { color: #D93025; font-size: 13px; }
.back-link { padding-top: 4px; text-align: center; color: #315EBA; font-size: 13px; }
</style>
