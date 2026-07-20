<template>
  <canvas
    class="match-canvas"
    canvas-id="matchCanvas"
    id="matchCanvas"
    :style="canvasStyle"
    @error="onCanvasError"
  ></canvas>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'

const props = defineProps({
  pairs: { type: Object, default: () => ({}) },
  leftPositions: { type: Object, default: () => ({}) },
  rightPositions: { type: Object, default: () => ({}) },
  drawingLine: { type: Object, default: null },
  pairColors: { type: Array, default: () => ['#35C773', '#4A90D9', '#FF9800', '#9C27B0', '#F44336', '#00BCD4'] },
  width: { type: Number, default: 760 },
  height: { type: Number, default: 400 }
})

const emit = defineEmits(['canvasReady'])

const canvasStyle = ref({
  width: `${props.width}px`,
  height: `${props.height}px`
})

let ctx = null
let canvasNode = null
let dpr = 1

function onCanvasError(e) {
  console.warn('MatchCanvas error:', e)
}

onMounted(() => {
  initCanvas()
})

// 监听尺寸变化
watch(() => [props.width, props.height], ([newW, newH]) => {
  canvasStyle.value = {
    width: `${newW}px`,
    height: `${newH}px`
  }
  // 重新初始化 canvas
  nextTick(() => initCanvas())
})

function initCanvas() {
  // #ifdef H5
  const query = uni.createSelectorQuery()
  query.select('#matchCanvas').fields({ node: true, size: true }).exec((res) => {
    if (res && res[0] && res[0].node) {
      canvasNode = res[0].node
      ctx = canvasNode.getContext('2d')
      dpr = uni.getSystemInfoSync().pixelRatio || 2
      canvasNode.width = props.width * dpr
      canvasNode.height = props.height * dpr
      ctx.scale(dpr, dpr)
      emit('canvasReady')
      drawAll()
    }
  })
  // #endif

  // #ifndef H5
  ctx = uni.createCanvasContext('matchCanvas')
  emit('canvasReady')
  drawAll()
  // #endif
}

// 监听配对和绘制线变化，重绘
watch(() => [props.pairs, props.drawingLine, props.leftPositions, props.rightPositions], () => {
  drawAll()
}, { deep: true })

function drawAll() {
  if (!ctx) return

  // #ifdef H5
  if (canvasNode) {
    ctx.clearRect(0, 0, props.width, props.height)

    // DEBUG: 画测试线 - 从左上角到右下角
    ctx.strokeStyle = 'green'
    ctx.lineWidth = 4
    ctx.setLineDash([10, 5])
    ctx.beginPath()
    ctx.moveTo(0, 0)
    ctx.lineTo(props.width, props.height)
    ctx.stroke()
    ctx.setLineDash([])

    // DEBUG: 画固定坐标点
    ctx.fillStyle = 'red'
    ctx.beginPath(); ctx.arc(319, 110, 8, 0, Math.PI * 2); ctx.fill()
    ctx.fillStyle = 'blue'
    ctx.beginPath(); ctx.arc(361, 110, 8, 0, Math.PI * 2); ctx.fill()

    drawPairs()
    drawDrawingLine()
    return
  }
  // #endif

  // 小程序
  ctx.clearRect(0, 0, props.width, props.height)
  drawPairs()
  drawDrawingLine()
  ctx.draw && ctx.draw(true)
}

function drawPairs() {
  const pairs = props.pairs
  const leftKeys = Object.keys(pairs)
  leftKeys.forEach((leftKey, colorIdx) => {
    const rightKey = pairs[leftKey]
    const startPos = props.leftPositions[leftKey]
    const endPos = props.rightPositions[rightKey]
    if (!startPos || !endPos) return

    const color = props.pairColors[colorIdx % props.pairColors.length]
    drawCurve(startPos.x, startPos.y, endPos.x, endPos.y, color, false)
  })
}

function drawDrawingLine() {
  const line = props.drawingLine
  if (!line) return
  drawCurve(line.startX, line.startY, line.endX, line.endY, '#4A90D9', true)
}

function drawCurve(x1, y1, x2, y2, color, isDashed) {
  const cpOffset = Math.min(Math.abs(x2 - x1) * 0.4, 80)

  if (isDashed) {
    // #ifdef H5
    ctx.setLineDash([8, 6])
    // #endif
  }

  ctx.beginPath()
  ctx.moveTo(x1, y1)
  ctx.bezierCurveTo(x1 + cpOffset, y1, x2 - cpOffset, y2, x2, y2)
  ctx.strokeStyle = color
  ctx.lineWidth = isDashed ? 3 : 4
  ctx.lineCap = 'round'
  ctx.stroke()

  if (isDashed) {
    // #ifdef H5
    ctx.setLineDash([])
    // #endif
  }

  // 端点圆
  drawDot(x1, y1, color, isDashed ? 5 : 6)
  drawDot(x2, y2, color, isDashed ? 5 : 6)
}

function drawDot(x, y, color, radius) {
  ctx.beginPath()
  ctx.arc(x, y, radius, 0, Math.PI * 2)
  ctx.fillStyle = color
  ctx.fill()
}

// 清除画布
function clear() {
  if (!ctx) return
  // #ifdef H5
  if (canvasNode) {
    ctx.clearRect(0, 0, props.width, props.height)
    return
  }
  // #endif
  ctx.clearRect(0, 0, props.width, props.height)
  ctx.draw && ctx.draw(true)
}

defineExpose({ clear, drawAll })
</script>

<style lang="scss" scoped>
.match-canvas {
  position: absolute;
  top: 0;
  left: 0;
  pointer-events: none;
  z-index: 10;
  background: rgba(255, 0, 0, 0.1); /* 临时：红色半透明背景看canvas范围 */
  border: 2px dashed red; /* 临时：红色虚线边框 */
}
</style>
