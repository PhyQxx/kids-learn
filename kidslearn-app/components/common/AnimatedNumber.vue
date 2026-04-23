<template>
  <view class="animated-number" :class="{ 'number-up': direction === 'up', 'number-down': direction === 'down' }">
    <text class="number-prefix" v-if="prefix">{{ prefix }}</text>
    <text class="number-value">{{ displayValue }}</text>
    <text class="number-suffix" v-if="suffix">{{ suffix }}</text>
  </view>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'

const props = defineProps({
  value: {
    type: Number,
    default: 0
  },
  duration: {
    type: Number,
    default: 1000
  },
  prefix: {
    type: String,
    default: ''
  },
  suffix: {
    type: String,
    default: ''
  },
  decimals: {
    type: Number,
    default: 0
  },
  direction: {
    type: String,
    default: 'up' // 'up', 'down'
  }
})

const emit = defineEmits(['complete'])

const displayValue = ref(props.value)
let animationFrame = null

function animateValue(start, end, duration) {
  const startTime = performance.now()

  function update(currentTime) {
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / duration, 1)

    // Easing function (ease-out)
    const easeOut = 1 - Math.pow(1 - progress, 3)
    const current = start + (end - start) * easeOut

    displayValue.value = props.decimals > 0
      ? current.toFixed(props.decimals)
      : Math.round(current)

    if (progress < 1) {
      animationFrame = requestAnimationFrame(update)
    } else {
      emit('complete')
    }
  }

  animationFrame = requestAnimationFrame(update)
}

watch(() => props.value, (newVal, oldVal) => {
  if (animationFrame) {
    cancelAnimationFrame(animationFrame)
  }
  animateValue(oldVal || 0, newVal, props.duration)
}, { immediate: false })

onMounted(() => {
  displayValue.value = props.decimals > 0
    ? props.value.toFixed(props.decimals)
    : Math.round(props.value)
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.animated-number {
  display: inline-flex;
  align-items: baseline;
  font-weight: bold;
  font-variant-numeric: tabular-nums;
}

.number-prefix,
.number-suffix {
  font-size: 0.8em;
  color: $text-secondary;
}

.number-value {
  color: inherit;
}

.number-up {
  animation: number-up 0.5s var(--ease-spring);
}

.number-down {
  animation: number-down 0.5s var(--ease-spring);
}

@keyframes number-up {
  0% { transform: translateY(10px); opacity: 0; }
  100% { transform: translateY(0); opacity: 1; }
}

@keyframes number-down {
  0% { transform: translateY(-10px); opacity: 0; }
  100% { transform: translateY(0); opacity: 1; }
}
</style>