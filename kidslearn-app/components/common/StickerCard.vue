<template>
  <view
    class="sticker-card"
    :class="{ collected, locked: !collected && locked, 'animate-reward-pop': showPop }"
    @tap="handleTap"
  >
    <view class="sticker-inner">
      <text class="sticker-emoji">{{ emoji }}</text>
      <view v-if="collected" class="collected-badge">
        <text class="check-icon">✓</text>
      </view>
      <view v-if="!collected && locked" class="locked-overlay">
        <text class="lock-icon">🔒</text>
      </view>
    </view>
    <text v-if="showName" class="sticker-name text-xs">{{ name }}</text>
  </view>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  emoji: {
    type: String,
    default: '🌟'
  },
  name: {
    type: String,
    default: '贴纸'
  },
  collected: {
    type: Boolean,
    default: false
  },
  locked: {
    type: Boolean,
    default: false
  },
  showName: {
    type: Boolean,
    default: true
  },
  rarity: {
    type: String,
    default: 'bronze' // 'bronze', 'silver', 'gold', 'legendary'
  }
})

const emit = defineEmits(['tap', 'collect'])

const showPop = ref(false)

function handleTap() {
  emit('tap')

  if (!props.collected && !props.locked) {
    // Trigger collect animation
    showPop.value = true
    setTimeout(() => {
      showPop.value = false
      emit('collect')
    }, 500)
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.sticker-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: transform 0.2s var(--ease-spring);

  &:active {
    transform: scale(0.94);
  }

  &.locked {
    opacity: 0.4;
    cursor: not-allowed;
    &:active { transform: none; }
  }

  &.collected {
    .sticker-inner {
      &::after {
        content: '';
        position: absolute;
        inset: -4px;
        border-radius: 50%;
        background: linear-gradient(135deg, $gold, transparent);
        opacity: 0.3;
        animation: glow-pulse 2s infinite;
      }
    }
  }
}

.sticker-inner {
  position: relative;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: $white;
  box-shadow: $shadow-sm;
  display: flex;
  align-items: center;
  justify-content: center;

  .bronze & { border: 3px solid #CD7F32; }
  .silver & { border: 3px solid #C0C0C0; }
  .gold & { border: 3px solid #FFD700; }
  .legendary & {
    border: 3px solid #9B59B6;
    animation: glow-pulse 2s infinite;
  }
}

.sticker-emoji {
  font-size: 32px;
}

.collected-badge {
  position: absolute;
  bottom: -4px;
  right: -4px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: $success;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(46, 204, 113, 0.4);
}

.check-icon {
  font-size: 14px;
  color: $white;
  font-weight: bold;
}

.locked-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
}

.lock-icon {
  font-size: 20px;
  opacity: 0.7;
}

.sticker-name {
  color: $text-secondary;
  text-align: center;
  max-width: 70px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@keyframes glow-pulse {
  0%, 100% { box-shadow: 0 0 8px rgba(155, 89, 182, 0.3); }
  50% { box-shadow: 0 0 16px rgba(155, 89, 182, 0.6); }
}
</style>