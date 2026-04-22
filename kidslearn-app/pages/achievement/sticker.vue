<template>
  <AppLayout theme="kids" title="贴纸册" :show-back="true" active-nav="/pages/achievement/index">
    <view class="sticker-content">
      <!-- 概览 -->
      <view class="summary-card">
        <view class="summary-info">
          <text class="summary-emoji">🎭</text>
          <view>
            <text class="text-lg text-bold text-white">贴纸册</text>
            <text class="text-sm text-white" style="opacity: 0.8;">已收集 {{ ownedCount }}/{{ totalCount }} 张</text>
          </view>
        </view>
        <view class="progress-wrap">
          <view class="progress-bar" style="width: 160px; background: rgba(255,255,255,0.3);">
            <view class="progress-fill" :style="{ width: (ownedCount / totalCount * 100) + '%' }"></view>
          </view>
        </view>
      </view>

      <!-- 分类 -->
      <tn-tabs v-model="activeTab" active-color="#7B68EE">
        <tn-tabs-item v-for="tab in tabItems" :key="tab.label" :title="tab.label" />
      </tn-tabs>

      <!-- 贴纸网格 -->
      <view class="sticker-grid">
        <view
          v-for="sticker in currentStickers"
          :key="sticker.id"
          class="sticker-slot"
          :class="{ owned: sticker.owned }"
          @tap="sticker.owned && previewSticker(sticker)"
        >
          <text class="sticker-emoji">{{ sticker.owned ? sticker.icon : '❓' }}</text>
          <text class="text-xs">{{ sticker.owned ? sticker.name : '???' }}</text>
          <view v-if="sticker.owned && sticker.rarity === 'legendary'" class="legendary-glow"></view>
        </view>
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { getStickers } from '@/api/achievement'

const activeTab = ref(0)
const tabItems = ref([
  { label: '全部' },
  { label: '动物' },
  { label: '自然' },
  { label: '太空' },
  { label: '节日' }
])

const allStickers = ref([
  // 动物
  { id: 1, name: '小狗', icon: '🐶', owned: true, category: 'animals', rarity: 'common' },
  { id: 2, name: '小猫', icon: '🐱', owned: true, category: 'animals', rarity: 'common' },
  { id: 3, name: '兔子', icon: '🐰', owned: true, category: 'animals', rarity: 'common' },
  { id: 4, name: '狐狸', icon: '🦊', owned: true, category: 'animals', rarity: 'rare' },
  { id: 5, name: '小熊', icon: '🐻', owned: false, category: 'animals', rarity: 'common' },
  { id: 6, name: '熊猫', icon: '🐼', owned: false, category: 'animals', rarity: 'rare' },
  { id: 7, name: '考拉', icon: '🐨', owned: true, category: 'animals', rarity: 'rare' },
  { id: 8, name: '狮子', icon: '🦁', owned: false, category: 'animals', rarity: 'legendary' },
  // 自然
  { id: 9, name: '彩虹', icon: '🌈', owned: true, category: 'nature', rarity: 'common' },
  { id: 10, name: '星星', icon: '⭐', owned: true, category: 'nature', rarity: 'common' },
  { id: 11, name: '樱花', icon: '🌸', owned: false, category: 'nature', rarity: 'rare' },
  { id: 12, name: '向日葵', icon: '🌻', owned: true, category: 'nature', rarity: 'common' },
  { id: 13, name: '月亮', icon: '🌙', owned: false, category: 'nature', rarity: 'legendary' },
  // 太空
  { id: 14, name: '火箭', icon: '🚀', owned: true, category: 'space', rarity: 'rare' },
  { id: 15, name: '地球', icon: '🌍', owned: true, category: 'space', rarity: 'common' },
  { id: 16, name: '土星', icon: '🪐', owned: false, category: 'space', rarity: 'rare' },
  { id: 17, name: '银河', icon: '🌌', owned: false, category: 'space', rarity: 'legendary' },
  // 节日
  { id: 18, name: '圣诞树', icon: '🎄', owned: false, category: 'festival', rarity: 'rare' },
  { id: 19, name: '南瓜', icon: '🎃', owned: true, category: 'festival', rarity: 'common' },
  { id: 20, name: '礼物', icon: '🎁', owned: true, category: 'festival', rarity: 'common' },
  { id: 21, name: '烟花', icon: '🎆', owned: false, category: 'festival', rarity: 'legendary' }
])

const ownedCount = computed(() => allStickers.value.filter(s => s.owned).length)
const totalCount = computed(() => allStickers.value.length)

const currentStickers = computed(() => {
  if (activeTab.value === 0) return allStickers.value
  const categories = ['animals', 'nature', 'space', 'festival']
  return allStickers.value.filter(s => s.category === categories[activeTab.value - 1])
})

function previewSticker(sticker) {
  uni.showToast({ title: `${sticker.icon} ${sticker.name}`, icon: 'none' })
}

onMounted(async () => {
  try {
    const res = await getStickers()
    if (res && Array.isArray(res) && res.length > 0) {
      allStickers.value = res.map(s => ({
        id: s.id,
        name: s.stickerName || s.name,
        icon: s.iconUrl || s.icon || '🎭',
        owned: s.owned || s.status === 'OWNED',
        category: s.seriesCode || s.category || 'animals',
        rarity: s.rarity || 'common'
      }))
    }
  } catch (e) {
    console.log('sticker: 使用模拟数据')
  }
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.sticker-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-card {
  background: linear-gradient(135deg, #7B68EE, #9B8BFF);
  border-radius: $radius-md;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 4px 20px rgba(123, 104, 238, 0.3);
}

.summary-info {
  display: flex;
  align-items: center;
  gap: 14px;
}

.summary-emoji { font-size: 44px; }
.progress-fill { height: 100%; border-radius: 5px; background: $accent; }

.sticker-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.sticker-slot {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 18px 8px;
  background: #F5F5F5;
  border-radius: $radius;
  cursor: pointer;
  transition: all $transition-fast;
  position: relative;

  &.owned {
    background: $white;
    box-shadow: $shadow-sm;
    &:active { transform: scale(0.95); }
  }
}

.sticker-emoji { font-size: 36px; }

.legendary-glow {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  border-radius: $radius;
  border: 2px solid rgba(155, 89, 182, 0.4);
  animation: glow 2s infinite;
}

@keyframes glow {
  0%, 100% { box-shadow: 0 0 6px rgba(155, 89, 182, 0.3); }
  50% { box-shadow: 0 0 16px rgba(155, 89, 182, 0.6); }
}
</style>
