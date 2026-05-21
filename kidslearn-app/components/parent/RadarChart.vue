<template>
  <view class="radar-chart-container">
    <svg width="100%" height="100%" viewBox="0 0 300 300" class="radar-svg">
      <!-- 背景网格 (多边形) -->
      <polygon
        v-for="i in 4"
        :key="'grid-' + i"
        :points="getGridPoints(i * 0.25)"
        class="grid-line"
      />
      
      <!-- 轴线 -->
      <line
        v-for="(axis, i) in axisPoints"
        :key="'axis-' + i"
        x1="150" y1="150"
        :x2="axis.x" :y2="axis.y"
        class="axis-line"
      />
      
      <!-- 数据多边形 -->
      <polygon
        :points="dataPoints"
        class="data-polygon"
      />
      
      <!-- 数据点 -->
      <circle
        v-for="(p, i) in dataNodePoints"
        :key="'node-' + i"
        :cx="p.x" :cy="p.y"
        r="4"
        class="data-node"
      />
      
      <!-- 标签 -->
      <text
        v-for="(label, i) in labels"
        :key="'label-' + i"
        :x="labelPoints[i].x"
        :y="labelPoints[i].y"
        class="label-text"
        :text-anchor="labelPoints[i].anchor"
      >
        {{ label }}
      </text>
    </svg>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  values: {
    type: Array,
    default: () => [80, 70, 60, 90, 50]
  },
  labels: {
    type: Array,
    default: () => ['语文', '数学', '英语', '逻辑', '科学']
  }
})

const centerX = 150
const centerY = 150
const radius = 100
const totalAxes = computed(() => props.labels.length)

// 计算轴线端点
const axisPoints = computed(() => {
  const points = []
  const angleStep = (Math.PI * 2) / totalAxes.value
  for (let i = 0; i < totalAxes.value; i++) {
    const angle = i * angleStep - Math.PI / 2
    points.push({
      x: centerX + radius * Math.cos(angle),
      y: centerY + radius * Math.sin(angle)
    })
  }
  return points
})

// 计算网格多边形顶点
function getGridPoints(scale) {
  const points = []
  const angleStep = (Math.PI * 2) / totalAxes.value
  for (let i = 0; i < totalAxes.value; i++) {
    const angle = i * angleStep - Math.PI / 2
    const x = centerX + radius * scale * Math.cos(angle)
    const y = centerY + radius * scale * Math.sin(angle)
    points.push(`${x},${y}`)
  }
  return points.join(' ')
}

// 计算数据多边形顶点
const dataPoints = computed(() => {
  const points = []
  const angleStep = (Math.PI * 2) / totalAxes.value
  for (let i = 0; i < totalAxes.value; i++) {
    const angle = i * angleStep - Math.PI / 2
    const scale = (props.values[i] || 0) / 100
    const x = centerX + radius * scale * Math.cos(angle)
    const y = centerY + radius * scale * Math.sin(angle)
    points.push(`${x},${y}`)
  }
  return points.join(' ')
})

// 计算数据节点坐标
const dataNodePoints = computed(() => {
  const points = []
  const angleStep = (Math.PI * 2) / totalAxes.value
  for (let i = 0; i < totalAxes.value; i++) {
    const angle = i * angleStep - Math.PI / 2
    const scale = (props.values[i] || 0) / 100
    points.push({
      x: centerX + radius * scale * Math.cos(angle),
      y: centerY + radius * scale * Math.sin(angle)
    })
  }
  return points
})

// 计算标签位置
const labelPoints = computed(() => {
  const points = []
  const angleStep = (Math.PI * 2) / totalAxes.value
  const labelRadius = radius + 25
  for (let i = 0; i < totalAxes.value; i++) {
    const angle = i * angleStep - Math.PI / 2
    const x = centerX + labelRadius * Math.cos(angle)
    const y = centerY + labelRadius * Math.sin(angle)
    
    let anchor = 'middle'
    if (Math.cos(angle) > 0.1) anchor = 'start'
    else if (Math.cos(angle) < -0.1) anchor = 'end'
    
    points.push({ x, y, anchor })
  }
  return points
})
</script>

<style lang="scss" scoped>
.radar-chart-container {
  width: 100%;
  height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.radar-svg {
  max-width: 320px;
  max-height: 320px;
}

.grid-line {
  fill: none;
  stroke: #E0E0E0;
  stroke-width: 1;
}

.axis-line {
  stroke: #E0E0E0;
  stroke-width: 1;
}

.data-polygon {
  fill: rgba(78, 205, 196, 0.3);
  stroke: #4ECDC4;
  stroke-width: 2;
  stroke-linejoin: round;
}

.data-node {
  fill: #4ECDC4;
  stroke: #FFF;
  stroke-width: 2;
}

.label-text {
  font-size: 13px;
  font-weight: 800;
  fill: #666;
}
</style>
