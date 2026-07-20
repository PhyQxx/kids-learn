<template>
  <view class="match-lines">
    <!-- 已配对的连线 -->
    <view
      v-for="(line, idx) in pairLines"
      :key="idx"
      class="match-line"
      :style="line.style"
    >
      <view class="line-inner" :style="{ background: line.color }"></view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  pairs: { type: Object, default: () => ({}) },
  leftPositions: { type: Object, default: () => ({}) },
  rightPositions: { type: Object, default: () => ({}) },
  pairColors: { type: Array, default: () => ['#35C773', '#4A90D9', '#FF9800', '#9C27B0', '#F44336', '#00BCD4'] }
})

// 计算所有连线的位置和角度
const pairLines = computed(() => {
  const lines = []
  const leftKeys = Object.keys(props.pairs)

  leftKeys.forEach((leftKey, colorIdx) => {
    const rightKey = props.pairs[leftKey]
    const startPos = props.leftPositions[leftKey]
    const endPos = props.rightPositions[rightKey]
    if (!startPos || !endPos) return

    const dx = endPos.x - startPos.x
    const dy = endPos.y - startPos.y
    const length = Math.sqrt(dx * dx + dy * dy)
    const angle = Math.atan2(dy, dx) * 180 / Math.PI

    const color = props.pairColors[colorIdx % props.pairColors.length]

    lines.push({
      style: {
        left: `${startPos.x}px`,
        top: `${startPos.y}px`,
        width: `${length}px`,
        transform: `rotate(${angle}deg)`,
        transformOrigin: '0 50%'
      },
      color
    })
  })

  return lines
})
</script>

<style lang="scss" scoped>
.match-lines {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 10;
}

.match-line {
  position: absolute;
  height: 4px;
}

.line-inner {
  width: 100%;
  height: 100%;
  border-radius: 2px;
}
</style>
