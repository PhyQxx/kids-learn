<template>
  <AppLayout theme="kids" title="贴纸册" :show-back="true" active-nav="/pages/achievement/index">
    <view class="sticker-content">
      <!-- 概览 -->
      <view class="summary-card">
        <view class="summary-info">
          <view>
            <text class="text-lg text-bold text-white">贴纸册</text>
            <text class="text-sm text-white" style="opacity: 0.8;">已收集 {{ ownedCount }}/{{ totalCount }} 张</text>
          </view>
        </view>
        <view class="progress-wrap">
          <view class="progress-bar" style="width: 160px; background: rgba(255,255,255,0.3);">
            <view class="progress-fill" :style="{ width: (totalCount ? ownedCount / totalCount * 100 : 0) + '%' }"></view>
          </view>
        </view>
      </view>

      <!-- 分类 -->
      <tn-tabs v-model="activeTab" active-color="#7B68EE">
        <tn-tabs-item v-for="tab in tabItems" :key="tab.label" :title="tab.label" />
      </tn-tabs>

      <!-- 贴纸网格 -->
      <view v-if="currentStickers.length" class="sticker-grid">
        <view
          v-for="sticker in currentStickers"
          :key="sticker.id"
          class="sticker-slot"
          :class="{ owned: sticker.owned }"
          @tap="sticker.owned && previewSticker(sticker)"
        >
          <template v-if="sticker.owned && resolvePetImage(sticker.icon).type === 'image'">
            <image class="sticker-emoji-img" :src="sticker.icon" mode="aspectFit" />
          </template>
          <text v-else-if="sticker.owned && resolvePetImage(sticker.icon).type === 'emoji'" class="sticker-emoji">{{ sticker.icon }}</text>
          <text v-else class="sticker-index">{{ sticker.owned ? String(sticker.id).padStart(2, '0') : '--' }}</text>
          <text class="text-xs">{{ sticker.owned ? sticker.name : '待收集' }}</text>
          <view v-if="sticker.owned && sticker.rarity === 'legendary'" class="legendary-glow"></view>
        </view>
      </view>
      <view v-else class="empty-state"><text>还没有可展示的贴纸</text></view>
    </view>
  </AppLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { getStickers } from '@/api/achievement'
import { resolvePetImage } from '@/utils/petFeature.mjs'

const activeTab = ref(0)
const tabItems = ref([
  { label: '全部' },
  { label: '动物' },
  { label: '自然' },
  { label: '太空' },
  { label: '节日' }
])

const allStickers = ref([])

const ownedCount = computed(() => allStickers.value.filter(s => s.owned).length)
const totalCount = computed(() => allStickers.value.length)

const currentStickers = computed(() => {
  if (activeTab.value === 0) return allStickers.value
  const categories = ['animals', 'nature', 'space', 'festival']
  return allStickers.value.filter(s => s.category === categories[activeTab.value - 1])
})

function previewSticker(sticker) {
  uni.showToast({ title: sticker.name, icon: 'none' })
}

onMounted(async () => {
  try {
    const res = await getStickers()
    if (res && Array.isArray(res) && res.length > 0) {
      allStickers.value = res.map(s => ({
        id: s.id,
        name: s.stickerName || s.name,
        icon: s.stickerUrl || s.iconUrl || s.icon || '',
        owned: s.owned || s.status === 'OWNED',
        category: s.seriesCode || s.category || 'animals',
        rarity: s.rarity || 'common'
      }))
    }
  } catch (e) {
    allStickers.value = []
    uni.showToast({ title: '贴纸加载失败，请稍后重试', icon: 'none' })
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
.sticker-emoji-img { width: 44px; height: 44px; }
.sticker-index { color: #5F3BB8; font-size: 22px; font-weight: 900; letter-spacing: 1px; }

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
