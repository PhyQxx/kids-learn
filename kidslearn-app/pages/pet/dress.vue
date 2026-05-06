<template>
  <AppLayout theme="kids" title="换装" :show-back="true" active-nav="/pages/pet/index">
    <view class="dress-content">
      <view class="dress-layout">
        <!-- 宠物预览 -->
        <view class="preview-panel">
          <text class="pet-emoji animate-float">{{ petStore.currentImageUrl }}</text>
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
          <view v-if="currentItems.length > 0" class="item-grid">
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
              <text v-else class="text-xs text-light">未拥有</text>
            </view>
          </view>
          <view v-else class="empty-hint">
            <text class="text-sm text-light">暂无装扮</text>
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
import { getPetStatus, getDecorations, getDecorationInventory, dressPet } from '@/api/pet'
import { normalizeDecorations, toggleDecorationEquip } from '@/utils/petFeature.mjs'

const petStore = usePetStore()

const activeTab = ref(0)
const tabItems = ref([
  { label: '帽子' },
  { label: '配饰' },
  { label: '服装' }
])

const slotKeys = ['head', 'accessory', 'outfit']

const allItems = ref({
  head: [],
  accessory: [],
  outfit: []
})

const currentItems = computed(() => allItems.value[slotKeys[activeTab.value]] || [])

const equippedItems = computed(() => {
  return [...allItems.value.head, ...allItems.value.accessory, ...allItems.value.outfit]
    .filter(i => i.equipped)
})

onMounted(async () => {
  try {
    const [decos, ownedDecos, petStatus] = await Promise.all([
      getDecorations(),
      getDecorationInventory(),
      getPetStatus()
    ])

    if (petStatus) {
      petStore.setPetInfo(petStatus)
    }

    const normalizedItems = normalizeDecorations(
      decos && Array.isArray(decos) ? decos : [],
      ownedDecos && Array.isArray(ownedDecos) ? ownedDecos : [],
      petStatus?.wearDecorationIds || []
    )

    for (const slotKey of slotKeys) {
      allItems.value[slotKey] = normalizedItems.filter(d => d.slot === slotKey)
    }
  } catch (e) {
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
})

async function toggleEquip(item) {
  if (!item.owned) {
    uni.showToast({ title: '还未拥有该物品，去商店购买吧', icon: 'none' })
    return
  }

  const beforeEquipped = item.equipped
  const flatItems = [...allItems.value.head, ...allItems.value.accessory, ...allItems.value.outfit]
  const updatedItems = toggleDecorationEquip(flatItems, item)
  for (const slotKey of slotKeys) {
    allItems.value[slotKey] = updatedItems.filter(i => i.slot === slotKey)
  }

  const ids = [...allItems.value.head, ...allItems.value.accessory, ...allItems.value.outfit]
    .filter(i => i.equipped)
    .map(i => i.id)

  try {
    await dressPet(ids)
    petStore.setPetInfo({ ...petStore.petInfo, wearDecorationIds: ids })
  } catch (e) {
    for (const slotKey of slotKeys) {
      allItems.value[slotKey] = flatItems.filter(i => i.slot === slotKey)
    }
    uni.showToast({ title: e.message || '操作失败', icon: 'none' })
    return
  }

  uni.showToast({ title: beforeEquipped ? `已卸下${item.name}` : `已装备${item.name}`, icon: 'none' })
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
  &:not(.owned) { opacity: 0.5; }
}

.item-emoji { font-size: 36px; }

.empty-hint {
  text-align: center;
  padding: 40px 0;
}
</style>
