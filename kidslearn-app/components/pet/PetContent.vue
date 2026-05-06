<template>
  <view class="pet-content">
    <!-- Loading -->
    <view v-if="loading" class="loading-state">
      <tn-loading size="60" />
      <text class="text-sm text-light" style="margin-top: 12px;">加载中...</text>
    </view>

    <template v-else>
    <view class="pet-layout">
      <!-- 左侧：宠物展示 -->
      <view class="pet-panel">
        <view class="pet-display">
          <text class="pet-emoji animate-float">{{ petStore.currentImageUrl }}</text>
          <view class="pet-name-row">
            <text class="text-lg text-bold">{{ petStore.name }}</text>
            <tn-badge type="primary" :text="'Lv.' + petStore.level" />
          </view>
          <text v-if="petStore.evolutionName" class="text-xs text-light">{{ petStore.evolutionName }}</text>
          <view class="wallet-row">
            <text class="wallet-pill">🪙 {{ userStore.gold || 0 }}</text>
            <text class="wallet-pill">💎 {{ userStore.userInfo?.diamond || 0 }}</text>
          </view>
        </view>

        <!-- 进化进度 -->
        <view class="evolution-section">
          <view class="evo-header">
            <text class="text-sm text-bold">进化进度</text>
            <text class="text-xs text-light">{{ petStore.expInLevel }}/{{ petStore.nextLevelExp }} XP</text>
          </view>
          <tn-line-progress :percent="petStore.nextLevelExp > 0 ? petStore.expInLevel / petStore.nextLevelExp * 100 : 0" active-color="#FF6B6B" inactive-color="rgba(255,255,255,0.3)" :height="12" :show-percent="false" />
        </view>

        <!-- 状态条 -->
        <view class="stats-bars">
          <view class="stat-row">
            <text class="stat-label text-sm">饱食度</text>
            <tn-line-progress :percent="petStore.hunger" active-color="#FFB74D" :height="12" :show-percent="false" style="flex: 1;" />
            <text class="text-xs text-light">{{ petStore.hunger }}%</text>
          </view>
          <view class="stat-row">
            <text class="stat-label text-sm">心情</text>
            <tn-line-progress :percent="petStore.moodPercent" active-color="#4ECDC4" :height="12" :show-percent="false" style="flex: 1;" />
            <text class="text-xs text-light">{{ petStore.moodText }}</text>
          </view>
          <view class="stat-row">
            <text class="stat-label text-sm">活力</text>
            <tn-line-progress :percent="petStore.energy" active-color="#2ECC71" :height="12" :show-percent="false" style="flex: 1;" />
            <text class="text-xs text-light">{{ petStore.energy }}%</text>
          </view>
        </view>

        <!-- 操作按钮 -->
        <view class="action-grid">
          <view class="action-btn-item card card-hover" @tap="handleFeed">
            <text class="action-emoji">🍖</text>
            <text class="text-sm">喂食</text>
          </view>
          <view class="action-btn-item card card-hover" @tap="handleBath">
            <text class="action-emoji">🛁</text>
            <text class="text-sm">洗澡</text>
          </view>
          <view class="action-btn-item card card-hover" @tap="handlePlay">
            <text class="action-emoji">🎾</text>
            <text class="text-sm">玩耍</text>
          </view>
          <view class="action-btn-item card card-hover" @tap="goDress">
            <text class="action-emoji">👔</text>
            <text class="text-sm">换装</text>
          </view>
        </view>
      </view>

      <!-- 右侧：背包/物品 -->
      <view class="inventory-panel">
        <!-- 背包食物 -->
        <view class="inventory-section card">
          <view class="inv-header">
            <text class="text-md text-bold">🎒 背包 / 食物</text>
            <text class="text-xs text-primary" @tap="goShop">去商店 →</text>
          </view>
          <view v-if="foodItems.length > 0" class="item-grid">
            <view v-for="item in foodItems" :key="item.id" class="item-card" :class="{ empty: item.count <= 0 }" @tap="selectFood(item)">
              <text class="item-emoji">{{ item.icon }}</text>
              <text class="text-xs">{{ item.name }}</text>
              <text class="text-xs text-light">x{{ item.count }}</text>
            </view>
          </view>
          <view v-else class="empty-hint">
            <text class="text-xs text-light">背包空空，去商店买点吧</text>
          </view>
        </view>

        <!-- 装扮物品 -->
        <view class="inventory-section card">
          <view class="inv-header">
            <text class="text-md text-bold">🎭 装扮</text>
          </view>
          <view v-if="costumeItems.length > 0" class="item-grid">
            <view v-for="item in costumeItems" :key="item.id" class="item-card">
              <text class="item-emoji">{{ item.icon }}</text>
              <text class="text-xs">{{ item.name }}</text>
              <text class="text-xs text-success">已拥有</text>
            </view>
          </view>
          <view v-else class="empty-hint">
            <text class="text-xs text-light">暂无装扮，去商店看看吧</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 喂食弹窗 -->
    <tn-popup v-model="showFeedModal" direction="center" :custom-style="{ width: '400px' }">
      <view class="feed-modal">
        <text class="text-lg text-bold" style="margin-bottom: 16px;">选择食物</text>
        <view v-if="availableFood.length > 0" class="feed-grid">
          <view
            v-for="item in availableFood"
            :key="item.id"
            class="feed-item card"
            :class="{ selected: selectedFood === item.id }"
            @tap="selectedFood = item.id"
          >
            <text class="feed-emoji">{{ item.icon }}</text>
            <text class="text-sm">{{ item.name }}</text>
            <text class="text-xs text-light">x{{ item.count }}</text>
          </view>
        </view>
        <view v-else class="empty-hint">
          <text class="text-sm text-light">没有可用的食物</text>
        </view>
        <view style="display: flex; gap: 12px; margin-top: 16px;">
          <tn-button type="primary" shape="round" @click="confirmFeed" :disabled="!selectedFood" block>确认喂食</tn-button>
          <tn-button shape="round" @click="showFeedModal = false" block>取消</tn-button>
        </view>
      </view>
    </tn-popup>
    </template>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { usePetStore } from '@/store/pet'
import { useUserStore } from '@/store/user'
import { getPetStatus, getInventory, getDecorationInventory, feedPet as feedPetApi, playPet as playPetApi, bathPet as bathPetApi } from '@/api/pet'
import { normalizeInventoryItems, normalizeDecorations } from '@/utils/petFeature.mjs'

const petStore = usePetStore()
const userStore = useUserStore()

const loading = ref(true)
const showFeedModal = ref(false)
const selectedFood = ref(null)

const foodItems = ref([])
const costumeItems = ref([])

const availableFood = computed(() => foodItems.value.filter(f => f.count > 0))

async function loadData() {
  loading.value = true
  try {
    const [petRes, inventoryRes, decoInvRes] = await Promise.all([
      getPetStatus(),
      getInventory(),
      getDecorationInventory()
    ])

    if (petRes) {
      petStore.setPetInfo(petRes)
      syncUserBalance(petRes)
    }

    if (inventoryRes && Array.isArray(inventoryRes)) {
      foodItems.value = normalizeInventoryItems(inventoryRes)
    }

    if (decoInvRes && Array.isArray(decoInvRes)) {
      costumeItems.value = normalizeDecorations(
        decoInvRes.map(c => ({
          id: c.decorationId,
          decoName: c.decoName,
          slot: c.slot,
          imageUrl: c.imageUrl,
          rarity: c.rarity
        })),
        decoInvRes,
        petRes?.wearDecorationIds || []
      )
    }
  } catch (e) {
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})

function handleFeed() {
  selectedFood.value = null
  showFeedModal.value = true
}

function selectFood(item) {
  if (item.count <= 0) return
  selectedFood.value = item.id
  showFeedModal.value = true
}

async function handleBath() {
  try {
    const res = await bathPetApi()
    uni.showToast({ title: res.message || '洗澡啦~ 🛁', icon: 'none' })
    loadData()
  } catch (e) {
    uni.showToast({ title: e.message || '操作失败', icon: 'none' })
  }
}

async function handlePlay() {
  try {
    const res = await playPetApi()
    uni.showToast({ title: res.message || '玩耍中~ 🎾', icon: 'none' })
    loadData()
  } catch (e) {
    uni.showToast({ title: e.message || '操作失败', icon: 'none' })
  }
}

function goDress() { uni.navigateTo({ url: '/pages/pet/dress' }) }
function goShop() { uni.navigateTo({ url: '/pages/pet/shop' }) }

async function confirmFeed() {
  if (!selectedFood.value) return
  const food = foodItems.value.find(f => f.id === selectedFood.value)
  if (food && food.count > 0) {
    showFeedModal.value = false
    try {
      const res = await feedPetApi(food.id)
      uni.showToast({ title: res.message || '喂食成功！', icon: 'none' })
    } catch (e) {
      uni.showToast({ title: e.message || '喂食失败', icon: 'none' })
    }
    selectedFood.value = null
    loadData()
  }
}

function syncUserBalance(petRes) {
  if (!userStore.userInfo) return
  if (petRes.gold == null && petRes.diamond == null) return
  userStore.setUserInfo({
    ...userStore.userInfo,
    gold: petRes.gold ?? userStore.userInfo.gold,
    diamond: petRes.diamond ?? userStore.userInfo.diamond
  })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.pet-content { height: 100%; }

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
}

.pet-layout {
  display: grid;
  grid-template-columns: 340px 1fr;
  gap: 16px;
  height: 100%;
}

.pet-panel {
  background: linear-gradient(135deg, #FFB6C1, #FFD4E5);
  border-radius: $radius-md;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pet-display {
  text-align: center;
  padding: 12px 0;
}

.pet-emoji { font-size: 80px; display: block; margin-bottom: 8px; }

.pet-name-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.wallet-row {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 8px;
}

.wallet-pill {
  background: rgba(255, 255, 255, 0.58);
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 13px;
  font-weight: 600;
}

.evolution-section {
  background: rgba(255, 255, 255, 0.5);
  border-radius: $radius;
  padding: 10px 14px;
}

.evo-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}

.stats-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-label { width: 52px; }

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.action-btn-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 14px;
  cursor: pointer;
  min-height: 72px;
  min-width: 72px;
  &:active { transform: scale(0.94); opacity: 0.9; }
}

.action-emoji { font-size: 28px; }

.inventory-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.inventory-section {
  padding: 16px 20px;
  flex: 1;
}

.inv-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.item-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.item-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 8px;
  background: #F8F8F8;
  border-radius: $radius;

  &.empty { opacity: 0.4; }
}

.item-emoji { font-size: 28px; }

.empty-hint {
  text-align: center;
  padding: 20px 0;
}

.feed-modal {
  padding: 24px;
}

.feed-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.feed-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 14px;
  cursor: pointer;
  border: 2px solid transparent;

  &.selected { border-color: $primary; background: #FFF0F0; }
}

.feed-emoji { font-size: 32px; }

@media (max-width: 800px) {
  .pet-layout { grid-template-columns: 260px 1fr; gap: 10px; }
  .pet-emoji { font-size: 60px; }
  .pet-panel { padding: 16px; }
  .item-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 640px) {
  .pet-layout { grid-template-columns: 1fr; }
  .pet-panel { padding: 12px; }
}
</style>
