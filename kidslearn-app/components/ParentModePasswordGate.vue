<template>
  <view v-if="visible" class="gate-mask" @tap.stop>
    <view class="gate-dialog" @tap.stop>
      <image class="gate-art" src="/static/redesign/rank-island.png" mode="aspectFill" />
      <text class="gate-title">{{ configured ? '验证家长PIN' : '设置家长PIN' }}</text>
      <text class="gate-desc">{{ configured ? '请输入该账号的6位家长PIN' : '首次使用需用账号密码设置独立家长PIN' }}</text>
      <input
        v-if="!configured"
        class="gate-input"
        v-model="password"
        type="password"
        password
        placeholder="请输入账号密码"
        placeholder-class="gate-placeholder"
        :disabled="loading"
      />
      <input
        class="gate-input"
        v-model="pin"
        type="number"
        password
        maxlength="6"
        placeholder="请输入6位家长PIN"
        placeholder-class="gate-placeholder"
        :focus="visible"
        :disabled="loading"
        @confirm="confirm"
      />
      <input
        v-if="!configured"
        class="gate-input"
        v-model="confirmPin"
        type="number"
        password
        maxlength="6"
        placeholder="再次输入家长PIN"
        placeholder-class="gate-placeholder"
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
import { getParentPinStatus, setupParentPin, verifyParentPin } from '@/api/auth'

const props = defineProps({
  targetUrl: { type: String, default: '/pages/parent/index' }
})

const visible = ref(false)
const password = ref('')
const pin = ref('')
const confirmPin = ref('')
const configured = ref(true)
const errorText = ref('')
const loading = ref(false)

async function open() {
  password.value = ''
  pin.value = ''
  confirmPin.value = ''
  errorText.value = ''
  loading.value = true
  visible.value = true
  try {
    const status = await getParentPinStatus()
    configured.value = status?.configured !== false
  } catch (error) {
    errorText.value = error?.msg || error?.message || '暂时无法验证家长PIN状态'
  } finally {
    loading.value = false
  }
}

function close() {
  if (loading.value) return
  visible.value = false
  password.value = ''
  pin.value = ''
  confirmPin.value = ''
  errorText.value = ''
}

async function confirm() {
  if (loading.value) return
  const value = pin.value.trim()
  if (!/^\d{6}$/.test(value)) {
    errorText.value = '请输入6位数字家长PIN'
    return
  }
  if (!configured.value && !password.value.trim()) {
    errorText.value = '请输入账号密码'
    return
  }
  if (!configured.value && confirmPin.value.trim() !== value) {
    errorText.value = '两次输入的家长PIN不一致'
    return
  }
  loading.value = true
  errorText.value = ''
  try {
    if (configured.value) {
      await verifyParentPin(value)
    } else {
      await setupParentPin(password.value.trim(), value)
    }
    loading.value = false
    close()
    uni.navigateTo({ url: props.targetUrl })
  } catch (error) {
    errorText.value = error?.msg || error?.message || '家长PIN验证失败'
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

.gate-art { width: 86px; height: 86px; align-self: center; border-radius: 22px; }

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
