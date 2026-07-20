<template>
  <view class="match-panel" ref="panelRef">
    <!-- 左侧列 -->
    <view class="match-column left-column card">
      <view
        v-for="left in options"
        :key="left.answerValue"
        class="match-item left"
        :class="{
          active: selectedLeft === left.answerValue,
          paired: !!pairs[left.answerValue]
        }"
        :id="'left-' + left.answerValue"
        @tap="onTapLeft(left)"
      >
        <text class="match-label">{{ left.pairLeft }}</text>
        <view class="match-pair-badge" v-if="pairs[left.answerValue]">
          <text>{{ getRightText(pairs[left.answerValue]) }}</text>
        </view>
      </view>
    </view>

    <!-- 连线 -->
    <MatchLine
      :pairs="pairs"
      :left-positions="leftPositions"
      :right-positions="rightPositions"
    />

    <!-- 右侧列 -->
    <view class="match-column right-column card">
      <view
        v-for="right in rightItems"
        :key="right.answerValue"
        class="match-item right"
        :class="{
          paired: matchedRightSet.has(right.answerValue),
          'highlight-preview': highlightRight === right.answerValue
        }"
        :id="'right-' + right.answerValue"
        @tap="onTapRight(right)"
      >
        <text class="match-label">{{ right.pairRight }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import MatchLine from './MatchLine.vue'
import { soundManager } from '@/utils/sound'

const props = defineProps({
  options: { type: Array, required: true },
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['update:pairs', 'pair-change'])

// 状态
const pairs = ref({})
const selectedLeft = ref('')
const highlightRight = ref('')
const rightItems = ref([])

// 位置缓存
const leftPositions = ref({})
const rightPositions = ref({})

const panelRef = ref(null)

// 配对颜色
const PAIR_COLORS = ['#35C773', '#4A90D9', '#FF9800', '#9C27B0', '#F44336', '#00BCD4']

// 已配对的右侧集合
const matchedRightSet = computed(() => {
  return new Set(Object.values(pairs.value))
})

// 初始化右侧随机顺序
function initRightItems() {
  const items = [...props.options]
  // Fisher-Yates 洗牌
  for (let i = items.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[items[i], items[j]] = [items[j], items[i]]
  }
  rightItems.value = items
}

// 重置
function reset() {
  pairs.value = {}
  selectedLeft.value = ''
  highlightRight.value = ''
  initRightItems()
  nextTick(() => measurePositions())
}

// 测量元素位置
function measurePositions() {
  nextTick(() => {
    const query = uni.createSelectorQuery()
    query.select('.match-panel').boundingClientRect(panelRect => {
      if (!panelRect) return

      const panelTop = panelRect.top
      const panelLeft = panelRect.left

      // 测量左侧项位置
      const q2 = uni.createSelectorQuery()
      q2.selectAll('.match-item.left').fields({ id: true, rect: true, size: true }).exec(res => {
        if (!res || !res[0]) return
        const positions = {}
        res[0].forEach(item => {
          const id = item.id || ''
          const key = id.startsWith('left-') ? id.substring(5) : ''
          if (key) {
            positions[key] = {
              x: item.left + item.width - panelLeft,
              y: item.top + item.height / 2 - panelTop
            }
          }
        })
        leftPositions.value = positions
      })

      // 测量右侧项位置
      const q3 = uni.createSelectorQuery()
      q3.selectAll('.match-item.right').fields({ id: true, rect: true, size: true }).exec(res => {
        if (!res || !res[0]) return
        const positions = {}
        res[0].forEach((item, idx) => {
          const id = item.id || ''
          const key = id.startsWith('right-') ? id.substring(6) : ''
          if (key) {
            positions[key] = {
              x: item.left - panelLeft,
              y: item.top + item.height / 2 - panelTop
            }
          }
        })
        rightPositions.value = positions
      })
    }).exec()
  })
}

// 点击左侧项
function onTapLeft(item) {
  if (props.disabled) return

  // 如果点击已配对的项，解除配对
  if (pairs.value[item.answerValue]) {
    const newPairs = { ...pairs.value }
    delete newPairs[item.answerValue]
    pairs.value = newPairs
    selectedLeft.value = ''
    emit('update:pairs', newPairs)
    emit('pair-change', newPairs)

    try { uni.vibrateShort({ type: 'light' }) } catch (err) {}
    soundManager.play('tap')
    return
  }

  selectedLeft.value = item.answerValue

  try { uni.vibrateShort({ type: 'light' }) } catch (err) {}
  soundManager.play('tap')
}

// 点击右侧项
function onTapRight(item) {
  if (props.disabled || !selectedLeft.value) return

  // 检查右侧项是否已被其他左侧配对
  const existingLeft = Object.keys(pairs.value).find(k => pairs.value[k] === item.answerValue)
  if (existingLeft) {
    // 解除旧配对
    const newPairs = { ...pairs.value }
    delete newPairs[existingLeft]
    pairs.value = newPairs
  }

  // 建立新配对
  const newPairs = {
    ...pairs.value,
    [selectedLeft.value]: item.answerValue
  }
  pairs.value = newPairs

  selectedLeft.value = ''
  highlightRight.value = ''

  emit('update:pairs', newPairs)
  emit('pair-change', newPairs)

  try { uni.vibrateShort({ type: 'medium' }) } catch (err) {}
  soundManager.play('success')
}

// 获取右侧文本
function getRightText(answerValue) {
  const right = props.options.find(item => item.answerValue === answerValue)
  return right ? right.pairRight : ''
}

// 监听 options 变化
watch(() => props.options, () => {
  initRightItems()
  nextTick(() => measurePositions())
}, { immediate: true })

// 暴露方法
defineExpose({ reset, getPairs: () => pairs.value })
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.match-panel {
  width: min(760px, 100%);
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  position: relative;
  overflow: visible;
}

.match-column {
  min-height: 220px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.match-item {
  min-height: 52px;
  padding: 10px 14px;
  border: 2px solid #E8F0FE;
  border-radius: $radius;
  background: $white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  cursor: pointer;
  transition: all 0.25s $ease-spring;

  &:active {
    transform: scale(0.97);
  }

  &.left {
    &.active {
      border-color: $learn-blue;
      background: #EEF6FF;
      box-shadow: 0 0 0 3px rgba(74, 144, 217, 0.15);
      transform: scale(1.02);
    }

    &.paired {
      border-color: $success;
      background: #F0FFF5;
    }
  }

  &.right {
    &.paired {
      border-color: $success;
      background: #F0FFF5;
      opacity: 0.7;
    }

    &.highlight-preview {
      border-color: $learn-blue;
      background: #EEF6FF;
      transform: scale(1.03);
    }
  }
}

.match-label {
  font-size: 16px;
  font-weight: 800;
  color: $text;
  flex: 1;
}

.match-pair-badge {
  font-size: 13px;
  color: $success;
  font-weight: 800;
  padding: 2px 8px;
  background: rgba(53, 199, 115, 0.1);
  border-radius: 12px;
}
</style>
