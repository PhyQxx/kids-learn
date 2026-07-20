<template>
  <view class="sortable-list">
    <view class="sortable-items" ref="listRef">
      <view
        v-for="(item, i) in items"
        :key="item.answerValue"
        class="sortable-item"
        :class="{
          dragging: dragIndex === i,
          shifting: shiftIndices.has(i),
          'first-item': i === 0
        }"
        :style="getItemStyle(i)"
        @touchstart="onTouchStart($event, i)"
        @touchmove="onTouchMove"
        @touchend="onTouchEnd"
        @touchcancel="onTouchEnd"
      >
        <!-- 拖拽把手 -->
        <view class="drag-handle">
          <text class="drag-dots">⠿</text>
        </view>

        <!-- 序号 -->
        <view class="order-index" :class="{ active: dragIndex === i }">
          <text>{{ i + 1 }}</text>
        </view>

        <!-- 内容 -->
        <text class="order-text">{{ item.text }}</text>

        <!-- 箭头按钮（备选交互） -->
        <view class="order-actions" v-if="!disabled">
          <view
            class="arrow-btn"
            :class="{ disabled: i === 0 || disabled }"
            @tap.stop="onArrowMove(i, -1)"
          >
            <text>↑</text>
          </view>
          <view
            class="arrow-btn"
            :class="{ disabled: i === items.length - 1 || disabled }"
            @tap.stop="onArrowMove(i, 1)"
          >
            <text>↓</text>
          </view>
        </view>
      </view>

      <!-- 插入指示器 -->
      <view
        v-if="insertIndex >= 0 && dragIndex >= 0"
        class="insert-indicator"
        :style="indicatorStyle"
      >
        <view class="indicator-line"></view>
        <view class="indicator-dot left"></view>
        <view class="indicator-dot right"></view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { soundManager } from '@/utils/sound'

const props = defineProps({
  items: { type: Array, required: true },
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['update:items', 'reorder'])

// 拖拽状态
const dragIndex = ref(-1)
const dragStartY = ref(0)
const dragCurrentY = ref(0)
const dragItemHeight = ref(64)
const insertIndex = ref(-1)
const shiftIndices = ref(new Set())
const listRef = ref(null)

// 长按定时器
let longPressTimer = null
let isLongPressing = false
let touchStartX = 0
let touchStartY = 0

// 计算拖拽项的偏移
function getItemStyle(index) {
  if (dragIndex.value === index && isLongPressing) {
    const offset = dragCurrentY.value - dragStartY.value
    return {
      transform: `translateY(${offset}px) scale(1.05)`,
      zIndex: 100,
      opacity: 0.9,
      transition: 'none'
    }
  }
  if (shiftIndices.value.has(index)) {
    const direction = index > dragIndex.value ? -1 : 1
    return {
      transform: `translateY(${direction * dragItemHeight.value}px)`,
      transition: 'transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275)'
    }
  }
  return {
    transform: 'translateY(0)',
    transition: 'transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275)'
  }
}

// 计算指示器位置
const indicatorStyle = computed(() => {
  if (insertIndex.value < 0) return { display: 'none' }
  const y = insertIndex.value * dragItemHeight.value
  return {
    top: `${y - 2}px`,
    display: 'flex'
  }
})

// 触摸开始 - 准备长按
function onTouchStart(e, index) {
  if (props.disabled) return
  const touch = e.touches[0]
  touchStartX = touch.clientX
  touchStartY = touch.clientY
  dragStartY.value = touch.clientY
  dragCurrentY.value = touch.clientY

  // 清除之前的定时器
  if (longPressTimer) {
    clearTimeout(longPressTimer)
  }

  // 设置长按定时器（300ms）
  longPressTimer = setTimeout(() => {
    isLongPressing = true
    dragIndex.value = index
    insertIndex.value = index

    // 获取项高度
    nextTick(() => {
      const query = uni.createSelectorQuery()
      query.select('.sortable-item').boundingClientRect(rect => {
        if (rect) {
          dragItemHeight.value = rect.height + 12 // gap
        }
      }).exec()
    })

    // 触觉反馈
    try {
      uni.vibrateShort({ type: 'light' })
    } catch (err) {}
  }, 300)
}

// 触摸移动
function onTouchMove(e) {
  if (!isLongPressing) {
    const touch = e.touches[0]
    const dx = Math.abs(touch.clientX - touchStartX)
    const dy = Math.abs(touch.clientY - touchStartY)
    // 如果移动超过 10px，取消长按
    if (dx > 10 || dy > 10) {
      if (longPressTimer) {
        clearTimeout(longPressTimer)
        longPressTimer = null
      }
    }
    return
  }

  e.preventDefault()
  const touch = e.touches[0]
  dragCurrentY.value = touch.clientY

  // 计算插入位置
  const offset = dragCurrentY.value - dragStartY.value
  const shift = Math.round(offset / dragItemHeight.value)
  const newInsertIndex = Math.max(0, Math.min(props.items.length - 1, dragIndex.value + shift))

  if (newInsertIndex !== insertIndex.value) {
    insertIndex.value = newInsertIndex
    updateShiftIndices(newInsertIndex)
  }
}

// 触摸结束
function onTouchEnd() {
  if (longPressTimer) {
    clearTimeout(longPressTimer)
    longPressTimer = null
  }

  if (!isLongPressing) return

  // 执行排序
  if (insertIndex.value >= 0 && insertIndex.value !== dragIndex.value) {
    const newItems = [...props.items]
    const [moved] = newItems.splice(dragIndex.value, 1)
    newItems.splice(insertIndex.value, 0, moved)
    emit('update:items', newItems)
    emit('reorder', newItems)

    // 触觉 + 音效反馈
    try {
      uni.vibrateShort({ type: 'medium' })
    } catch (err) {}
    soundManager.play('tap')
  }

  // 重置状态
  isLongPressing = false
  dragIndex.value = -1
  dragCurrentY.value = 0
  dragStartY.value = 0
  insertIndex.value = -1
  shiftIndices.value = new Set()
}

// 更新需要位移的项
function updateShiftIndices(targetIndex) {
  const indices = new Set()
  const current = dragIndex.value
  if (targetIndex > current) {
    for (let i = current + 1; i <= targetIndex; i++) {
      indices.add(i)
    }
  } else if (targetIndex < current) {
    for (let i = targetIndex; i < current; i++) {
      indices.add(i)
    }
  }
  shiftIndices.value = indices
}

// 箭头按钮点击（备选交互）
function onArrowMove(index, direction) {
  if (props.disabled) return
  const nextIndex = index + direction
  if (nextIndex < 0 || nextIndex >= props.items.length) return

  const newItems = [...props.items]
  const temp = newItems[index]
  newItems[index] = newItems[nextIndex]
  newItems[nextIndex] = temp
  emit('update:items', newItems)
  emit('reorder', newItems)

  // 触觉 + 音效反馈
  try {
    uni.vibrateShort({ type: 'light' })
  } catch (err) {}
  soundManager.play('tap')
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.sortable-list {
  width: 100%;
}

.sortable-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: relative;
}

.sortable-item {
  min-height: 58px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: $radius;
  background: #F7FBFF;
  border: 2px solid transparent;
  will-change: transform;
  touch-action: none;
  user-select: none;

  &.dragging {
    background: $white;
    border-color: $learn-blue;
    box-shadow: 0 8px 24px rgba(74, 144, 217, 0.2);
    border-radius: $radius-md;
  }

  &.shifting {
    // 位移动画由 inline style 控制
  }

  &:active:not(.dragging) {
    transform: scale(0.98);
    transition: transform 0.15s ease;
  }
}

.drag-handle {
  width: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  cursor: grab;
}

.drag-dots {
  font-size: 18px;
  color: $text-light;
  opacity: 0.5;
  letter-spacing: 2px;
}

.order-index {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #DBEAFE;
  color: $learn-blue;
  font-weight: 800;
  font-size: 14px;
  flex-shrink: 0;
  transition: all 0.3s $ease-spring;

  &.active {
    background: $learn-blue;
    color: $white;
    transform: scale(1.1);
  }
}

.order-text {
  flex: 1;
  font-size: 17px;
  font-weight: 800;
  color: $text;
  line-height: 1.35;
}

.order-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.arrow-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: $white;
  color: $learn-blue;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  font-size: 16px;
  box-shadow: $shadow-sm;
  transition: all 0.15s ease;

  &:active {
    transform: scale(0.9);
    box-shadow: none;
  }

  &.disabled {
    opacity: 0.3;
    pointer-events: none;
  }
}

.insert-indicator {
  position: absolute;
  left: 12px;
  right: 12px;
  height: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
  pointer-events: none;
}

.indicator-line {
  flex: 1;
  height: 4px;
  background: $learn-blue;
  border-radius: 2px;
  animation: indicatorPulse 0.8s ease infinite;
}

.indicator-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: $learn-blue;
  position: absolute;

  &.left { left: -5px; }
  &.right { right: -5px; }
}

@keyframes indicatorPulse {
  0%, 100% { opacity: 1; transform: scaleX(1); }
  50% { opacity: 0.7; transform: scaleX(0.98); }
}
</style>
