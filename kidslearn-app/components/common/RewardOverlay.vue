<template>
  <transition name="reward-fade">
    <view v-if="visible" class="reward-overlay" @tap="closeReward">
      <!-- Confetti particles -->
      <view
        v-for="particle in particles"
        :key="particle.id"
        class="confetti"
        :style="particle.style"
      >
        <text class="confetti-emoji">{{ particle.emoji }}</text>
      </view>

      <!-- Reward icon fly-in -->
      <view class="reward-item" :class="[rewardType, { 'animate-reward-pop': visible }]">
        <text class="reward-emoji">{{ icon }}</text>
      </view>

      <!-- Score increment -->
      <view v-if="score !== 0" class="score-fly" :class="score > 0 ? 'positive' : 'negative'">
        <text>{{ score > 0 ? '+' : '' }}{{ score }}</text>
      </view>

      <!-- Message text -->
      <view v-if="message" class="reward-message">
        <text class="reward-text">{{ message }}</text>
      </view>
    </view>
  </transition>
</template>

<script setup>
import { ref, watch, computed } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  icon: {
    type: String,
    default: '🎉'
  },
  rewardType: {
    type: String,
    default: 'default' // 'default', 'star', 'trophy', 'coin'
  },
  score: {
    type: Number,
    default: 0
  },
  message: {
    type: String,
    default: ''
  },
  confettiCount: {
    type: Number,
    default: 20
  },
  duration: {
    type: Number,
    default: 2000
  }
})

const emit = defineEmits(['close', 'update:visible'])

const particles = ref([])

// Generate confetti particles
function generateParticles() {
  const emojis = ['⭐', '🌟', '✨', '💫', '🎊', '🎉', '🏅', '🔔', '💎']
  const newParticles = []

  for (let i = 0; i < props.confettiCount; i++) {
    const angle = (360 / props.confettiCount) * i
    const startX = Math.random() * 100 - 50
    const startY = -20

    newParticles.push({
      id: i,
      emoji: emojis[Math.floor(Math.random() * emojis.length)],
      style: {
        left: `${50 + startX}%`,
        top: `${startY}%`,
        animationDelay: `${Math.random() * 0.5}s`,
        animationDuration: `${1.5 + Math.random() * 1}s`,
        '--angle': `${angle}deg`,
        '--drift': `${Math.random() * 40 - 20}px`
      }
    })
  }

  particles.value = newParticles
}

watch(() => props.visible, (newVal) => {
  if (newVal) {
    generateParticles()

    // Auto close after duration
    setTimeout(() => {
      closeReward()
    }, props.duration)
  }
})

function closeReward() {
  emit('update:visible', false)
  emit('close')
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.reward-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.4);
  pointer-events: all;
}

/* Confetti animation */
.confetti {
  position: absolute;
  font-size: 24px;
  animation: confetti-fall var(--duration, 2s) ease-out forwards;
  pointer-events: none;
}

@keyframes confetti-fall {
  0% {
    transform: translateY(-20px) rotate(0deg) translateX(0);
    opacity: 1;
  }
  100% {
    transform: translateY(100vh) rotate(720deg) translateX(var(--drift, 20px));
    opacity: 0;
  }
}

/* Reward item */
.reward-item {
  position: relative;
  z-index: 10;

  &.animate-reward-pop {
    animation: reward-pop 0.6s var(--ease-bounce);
  }

  &.star .reward-emoji { color: $gold; }
  &.trophy .reward-emoji { color: #CD7F32; }
  &.coin .reward-emoji { color: #FFD700; }
}

.reward-emoji {
  font-size: 80px;
  filter: drop-shadow(0 4px 20px rgba(0, 0, 0, 0.3));
}

/* Score fly */
.score-fly {
  position: absolute;
  font-size: 32px;
  font-weight: bold;
  animation: score-fly-up 1s ease-out forwards;

  &.positive {
    color: $gold;
    text-shadow: 0 2px 10px rgba(255, 215, 0, 0.5);
  }

  &.negative {
    color: $error;
    text-shadow: 0 2px 10px rgba(231, 76, 60, 0.5);
  }
}

@keyframes score-fly-up {
  0% {
    transform: translateY(0) scale(0.5);
    opacity: 0;
  }
  30% {
    transform: translateY(-30px) scale(1.2);
    opacity: 1;
  }
  100% {
    transform: translateY(-100px) scale(1);
    opacity: 0;
  }
}

/* Message text */
.reward-message {
  position: absolute;
  bottom: 20%;
  text-align: center;
}

.reward-text {
  font-size: 24px;
  font-weight: bold;
  color: $white;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
  animation: text-pop 0.5s var(--ease-spring) 0.3s both;
}

@keyframes text-pop {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  70% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

/* Transition */
.reward-fade-enter-active,
.reward-fade-leave-active {
  transition: opacity 0.3s ease;
}

.reward-fade-enter-from,
.reward-fade-leave-to {
  opacity: 0;
}
</style>