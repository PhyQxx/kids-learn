<template>
  <view class="grade-popup-mask" v-if="visible" @tap="onMaskTap">
    <view class="grade-popup" @tap="onPopupTap">
      <!-- 标题 -->
      <view class="popup-header">
        <text class="popup-title">选择年级</text>
        <view class="close-btn" @tap="onCloseTap">
          <text>✕</text>
        </view>
      </view>

      <!-- 副标题 -->
      <text class="popup-subtitle">选择一个适合孩子当前年级的选项</text>

      <!-- 年级网格 -->
      <view class="grade-grid">
        <view
          v-for="grade in gradeOptions"
          :key="grade.value"
          class="grade-item"
          :class="{ selected: selectedGrade === grade.value }"
          @tap="selectGrade(grade.value)"
        >
          <text class="grade-emoji">{{ grade.icon }}</text>
          <text class="grade-label">{{ grade.label }}</text>
          <view v-if="selectedGrade === grade.value" class="check-badge">
            <text>✓</text>
          </view>
        </view>
      </view>

      <!-- 确认按钮 -->
      <view class="confirm-btn" @tap="handleConfirm">
        <text class="text-white text-md text-bold">确认</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  currentGrade: { type: Number, default: null }
})

const emit = defineEmits(['update:visible', 'confirm'])

const gradeOptions = [
  { value: 1, label: '小班', icon: '🧒' },
  { value: 2, label: '中班', icon: '👦' },
  { value: 3, label: '大班', icon: '🧒' },
  { value: 4, label: '一年级', icon: '📖' },
  { value: 5, label: '二年级', icon: '📗' },
  { value: 6, label: '三年级', icon: '📕' },
  { value: 7, label: '四年级', icon: '📘' },
  { value: 8, label: '五年级', icon: '📙' },
  { value: 9, label: '六年级', icon: '📚' }
]

const selectedGrade = ref(null)

watch(() => props.currentGrade, (val) => {
  selectedGrade.value = val
}, { immediate: true })

watch(() => props.visible, (val) => {
  if (val) {
    selectedGrade.value = props.currentGrade
  }
})

function selectGrade(value) {
  selectedGrade.value = value
}

// 点击遮罩层关闭
function onMaskTap() {
  emit('update:visible', false)
}

// 点击弹框内容，不关闭
function onPopupTap() {
  // 阻止冒泡，什么都不做
}

// 点击关闭按钮
function onCloseTap() {
  emit('update:visible', false)
  emit('close')
}

function handleConfirm() {
  if (!selectedGrade.value) {
    uni.showToast({ title: '请先选择年级', icon: 'none' })
    return
  }
  emit('confirm', selectedGrade.value)
  emit('update:visible', false)
}
</script>

<style lang="scss" scoped>
.grade-popup-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 9999;
}

.grade-popup {
  width: 100%;
  max-width: 600rpx;
  background: #fff;
  border-radius: 32rpx 32rpx 0 0;
  padding: 40rpx 32rpx;
  padding-bottom: calc(40rpx + env(safe-area-inset-bottom));
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}

.popup-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.popup-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #333;
}

.close-btn {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #F5F5F5;
  border-radius: 50%;
  font-size: 28rpx;
  color: #999;
}

.popup-subtitle {
  font-size: 26rpx;
  color: #999;
  margin-bottom: 32rpx;
}

.grade-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20rpx;
  margin-bottom: 40rpx;
}

.grade-item {
  position: relative;
  background: #F8F8F8;
  border-radius: 20rpx;
  padding: 28rpx 16rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  transition: all 0.2s ease;

  &.selected {
    background: linear-gradient(135deg, #FFF0F0, #FFE8E8);
    border: 2rpx solid #FF6B6B;
  }

  &:active {
    transform: scale(0.95);
  }
}

.grade-emoji {
  font-size: 48rpx;
}

.grade-label {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
}

.check-badge {
  position: absolute;
  top: -8rpx;
  right: -8rpx;
  width: 36rpx;
  height: 36rpx;
  background: #FF6B6B;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20rpx;
  color: #fff;
  font-weight: bold;
}

.confirm-btn {
  width: 100%;
  height: 88rpx;
  background: linear-gradient(135deg, #FF6B6B, #FF8E8E);
  border-radius: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(255, 107, 107, 0.3);

  &:active {
    opacity: 0.9;
    transform: scale(0.98);
  }
}
</style>
