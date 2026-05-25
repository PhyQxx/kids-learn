<template>
  <view v-if="visible" class="gate-mask" @tap.stop>
    <view class="gate-dialog" @tap.stop>
      <view class="gate-icon">🛡️</view>
      <text class="gate-title">验证家长密码</text>
      <text class="gate-desc">进入家长模式前需要确认当前账号密码</text>
      <input
        class="gate-input"
        v-model="password"
        type="password"
        password
        placeholder="请输入密码"
        placeholder-class="gate-placeholder"
        :focus="visible"
        :disabled="loading"
        @confirm="confirm"
      />
      <text v-if="errorText" class="gate-error">{{ errorText }}</text>
      <view class="gate-actions">
        <button class="gate-btn cancel" :disabled="loading" @tap="close">取消</button>
        <button class="gate-btn confirm" :disabled="loading" @tap="confirm">
          {{ loading ? '验证中' : '进入' }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { verifyPassword } from '@/api/auth'

const props = defineProps({
  targetUrl: { type: String, default: '/pages/parent/index' }
})

const visible = ref(false)
const password = ref('')
const errorText = ref('')
const loading = ref(false)

function open() {
  password.value = ''
  errorText.value = ''
  loading.value = false
  visible.value = true
}

function close() {
  if (loading.value) return
  visible.value = false
  password.value = ''
  errorText.value = ''
}

async function confirm() {
  if (loading.value) return
  const value = password.value.trim()
  if (!value) {
    errorText.value = '请输入密码'
    return
  }
  loading.value = true
  errorText.value = ''
  try {
    await verifyPassword(value)
    loading.value = false
    close()
    uni.navigateTo({ url: props.targetUrl })
  } catch (error) {
    errorText.value = error?.msg || error?.message || '密码验证失败'
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.gate-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(25, 34, 46, 0.42);
  box-sizing: border-box;
}

.gate-dialog {
  width: min(360px, 100%);
  padding: 24px;
  border-radius: $radius-lg;
  background: #fff;
  border: 1px solid rgba(73, 98, 128, 0.08);
  box-shadow: 0 18px 48px rgba(73, 98, 128, 0.22);
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 12px;
  box-sizing: border-box;
}

.gate-icon {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: #FFF0E8;
  color: $primary;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  align-self: center;
}

.gate-title {
  color: $text;
  font-size: 20px;
  font-weight: 800;
  text-align: center;
}

.gate-desc {
  color: $text-light;
  font-size: 13px;
  line-height: 1.5;
  text-align: center;
}

.gate-input {
  height: 44px;
  padding: 0 14px;
  border-radius: $radius;
  background: #F6F8FB;
  color: $text;
  font-size: 15px;
  box-sizing: border-box;
}

.gate-placeholder {
  color: $text-light;
}

.gate-error {
  color: #D93025;
  font-size: 13px;
}

.gate-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 4px;
}

.gate-btn {
  height: 42px;
  line-height: 42px;
  border-radius: $radius;
  font-size: 15px;
  font-weight: 800;
}

.gate-btn.cancel {
  background: #F1F6FC;
  color: $text-secondary;
}

.gate-btn.confirm {
  background: $primary;
  color: #fff;
}
</style>
