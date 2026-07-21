<template>
  <view v-if="hasError" class="error-boundary">
    <view class="error-content">
      <image class="error-art" src="/static/redesign/weakpoint-console.png" mode="aspectFill" />
      <text class="error-title">哎呀，出错了</text>
      <text class="error-message">{{ errorMessage }}</text>
      <view class="error-actions">
        <button class="retry-btn" @tap="handleRetry">重新加载</button>
        <button class="home-btn" @tap="handleGoHome">回到首页</button>
      </view>
    </view>
  </view>
  <slot v-else />
</template>

<script setup>
import { ref, onErrorCaptured } from 'vue'

const hasError = ref(false)
const errorMessage = ref('')

onErrorCaptured((err) => {
  hasError.value = true
  errorMessage.value = err.message || '未知错误'
  console.error('ErrorBoundary caught:', err)
  return false
})

function handleRetry() {
  hasError.value = false
  errorMessage.value = ''
}

function handleGoHome() {
  uni.reLaunch({ url: '/pages/main/index' })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.error-boundary {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.error-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 16px;
}

.error-emoji {
  font-size: 64px;
  margin-bottom: 8px;
}

.error-art { width: 150px; height: 110px; border-radius: 22px; }

.error-title {
  font-size: 20px;
  font-weight: 700;
  color: #333;
}

.error-message {
  font-size: 14px;
  color: #999;
  max-width: 280px;
}

.error-actions {
  display: flex;
  gap: 16px;
  margin-top: 16px;
}

.retry-btn,
.home-btn {
  padding: 12px 24px;
  border-radius: 24px;
  font-size: 14px;
  border: none;
}

.retry-btn {
  background: $primary;
  color: #fff;
}

.home-btn {
  background: #f5f5f5;
  color: #666;
}

.retry-btn:active,
.home-btn:active {
  transform: scale(0.95);
}
</style>
