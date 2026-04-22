<template>
  <AppLayout theme="kids" title="换装" :show-back="true" active-nav="/pages/pet/index">
    <view class="dress-content">
      <view class="dress-layout">
        <!-- 宠物预览 -->
        <view class="preview-panel">
          <text class="pet-emoji animate-float">🐱</text>
          <text class="text-lg text-bold">{{ petStore.name }}</text>
          <view class="equipped-list">
            <text class="text-sm text-light">已装备：</text>
            <text v-for="item in equippedItems" :key="item.id" class="equipped-tag">{{ item.icon }} {{ item.name }}</text>
            <text v-if="equippedItems.length === 0" class="text-sm text-light">暂无装备</text>
          </view>
        </view>

        <!-- 物品选择 -->
        <view class="items-panel">
          <tn-tabs v-model="activeTab" active-color="#FF6B6B">
            <tn-tabs-item v-for="tab in tabItems" :key="tab.label" :title="tab.label" />
          </tn-tabs>
          <view class="item-grid">
            <view
              v-for="item in currentItems"
              :key="item.id"
              class="item-card card"
              :class="{ owned: item.owned, equipped: item.equipped }"
              @tap="toggleEquip(item)"
            >
              <text class="item-emoji">{{ item.icon }}</text>
              <text class="text-sm">{{ item.name }}</text>
              <text v-if="item.owned" class="text-xs text-success">{{ item.equipped ? '已装备' : '点击装备' }}</text>
              <text v-else class="text-xs text-light">🔒 未拥有</text>
            </view>
          </view>
        </view>
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { usePetStore } from '@/store/pet'
import { getInventory, dressPet } from '@/api/pet'

const petStore = usePetStore()

const activeTab = ref(0)
const tabItems = ref([
  { label: '帽子' },
  { label: '配饰' },
  { label: '服装' }
])

const allItems = ref({
  hats: [
    { id: 1, name: '礼帽', icon: '🎩', owned: true, equipped: false },
    { id: 2, name: '皇冠', icon: '👑', owned: true, equipped: true },
    { id: 3, name: '巫师帽', icon: '🧙', owned: false, equipped: false },
    { id: 4, name: '厨师帽', icon: '👨‍🍳', owned: false, equipped: false }
  ],
  accessories: [
    { id: 5, name: '蝴蝶结', icon: '🎀', owned: true, equipped: false },
    { id: 6, name: '眼镜', icon: '🕶️', owned: true, equipped: false },
    { id: 7, name: '盾牌', icon: '🛡️', owned: false, equipped: false },
    { id: 8, name: '围巾', icon: '🧣', owned: false, equipped: false }
  ],
  outfits: [
    { id: 9, name: '披风', icon: '🦸', owned: true, equipped: false },
    { id: 10, name: '西装', icon: '🤵', owned: false, equipped: false },
    { id: 11, name: '花裙', icon: '👗', owned: false, equipped: false },
    { id: 12, name: '铠甲', icon: '⚔️', owned: false, equipped: false }
  ]
})

onMounted(async () => {
  try {
    const res = await getInventory()
    if (res && Array.isArray(res)) {
      const costumes = res.filter(r => r.itemType === 2 || r.type === 'costume')
      if (costumes.length > 0) {
        const items = costumes.map(c => ({
          id: c.id,
          name: c.itemName || c.name,
          icon: c.iconUrl || c.icon || '🎭',
          owned: c.owned !== false,
          equipped: c.equipped || false
        }))
        // Distribute across tabs
        const perTab = Math.ceil(items.length / 3)
        allItems.value.hats = items.slice(0, perTab)
        allItems.value.accessories = items.slice(perTab, perTab * 2)
        allItems.value.outfits = items.slice(perTab * 2)
      }
    }
  } catch (e) {
    console.log('dress: 使用模拟数据')
  }
})

const currentItems = computed(() => {
  const keys = ['hats', 'accessories', 'outfits']
  return allItems.value[keys[activeTab.value]] || []
})

const equippedItems = computed(() => {
  return [...allItems.value.hats, ...allItems.value.accessories, ...allItems.value.outfits]
    .filter(i => i.equipped)
})

async function toggleEquip(item) {
  if (!item.owned) {
    uni.showToast({ title: '还未拥有该物品', icon: 'none' })
    return
  }
  item.equipped = !item.equipped

  // 提交当前所有装备的ID给后端
  const ids = [...allItems.value.hats, ...allItems.value.accessories, ...allItems.value.outfits]
    .filter(i => i.equipped)
    .map(i => i.id)

  try {
    await dressPet(ids)
  } catch (e) {
    // 即使API失败也在本地显示效果
  }

  uni.showToast({ title: item.equipped ? `已装备${item.name}` : `已卸下${item.name}`, icon: 'none' })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.dress-content { height: 100%; }

.dress-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 16px;
  height: 100%;
}

.preview-panel {
  background: linear-gradient(135deg, #FFB6C1, #FFD4E5);
  border-radius: $radius-md;
  padding: 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.pet-emoji { font-size: 100px; }

.equipped-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.equipped-tag {
  background: rgba(255, 255, 255, 0.5);
  padding: 4px 10px;
  border-radius: 100px;
  font-size: 13px;
}

.items-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.item-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.item-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 18px 12px;
  cursor: pointer;
  border: 2px solid transparent;

  &.equipped { border-color: $primary; background: #FFF0F0; }
}

.item-emoji { font-size: 36px; }
</style>
