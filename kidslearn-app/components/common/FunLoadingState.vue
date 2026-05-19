<template>
  <view class="fun-loading-state" :class="{ 'full-screen': fullScreen }">
    <view class="animation-container">
      <!-- 动画 Mascot -->
      <view class="mascot-wrapper animate-float">
        <text class="mascot-emoji">{{ mascot }}</text>
        <view class="mascot-shadow"></view>
      </view>
      
      <!-- Spinner -->
      <view class="spinner-wrapper">
        <view class="custom-spinner"></view>
        <view class="spinner-orbit"></view>
      </view>
      
      <!-- Loading Text -->
      <view class="text-wrapper animate-fade-in">
        <text class="loading-text">{{ title }}</text>
        <view class="dots">
          <view class="dot"></view>
          <view class="dot"></view>
          <view class="dot"></view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: {
    type: String,
    default: '加载中'
  },
  mascot: {
    type: String,
    default: '🌍'
  },
  fullScreen: {
    type: Boolean,
    default: false
  }
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.fun-loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  width: 100%;
  
  &.full-screen {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(255, 255, 255, 0.95);
    z-index: 9999;
    padding: 0;
  }
}

.animation-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
}

/* Mascot */
.mascot-wrapper {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.mascot-emoji {
  font-size: 64px;
  line-height: 1;
  z-index: 2;
}

.mascot-shadow {
  width: 40px;
  height: 8px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 50%;
  margin-top: 12px;
  animation: shadow-scale 3s ease-in-out infinite;
}

@keyframes shadow-scale {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.4); opacity: 0.5; }
}

/* Spinner */
.spinner-wrapper {
  position: relative;
  width: 60px;
  height: 60px;
}

.custom-spinner {
  position: absolute;
  width: 100%;
  height: 100%;
  border: 4px solid #F1F6FC;
  border-top-color: $primary;
  border-radius: 50%;
  animation: spin 1.5s linear infinite;
}

.spinner-orbit {
  position: absolute;
  top: -8px;
  left: -8px;
  right: -8px;
  bottom: -8px;
  border: 1px dashed rgba($primary, 0.2);
  border-radius: 50%;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Text */
.text-wrapper {
  display: flex;
  align-items: center;
  gap: 4px;
}

.loading-text {
  font-size: 16px;
  font-weight: 800;
  color: #2C3E50;
  background: linear-gradient(135deg, $text, $primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.dots {
  display: flex;
  gap: 4px;
}

.dot {
  width: 6px;
  height: 6px;
  background-color: $primary;
  border-radius: 50%;
  animation: dot-jump 1.4s infinite ease-in-out both;
  
  &:nth-child(1) { animation-delay: -0.32s; }
  &:nth-child(2) { animation-delay: -0.16s; }
}

@keyframes dot-jump {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}
</style>
