<template>
  <view class="pet-content">
    <!-- Loading -->
    <FunLoadingState v-if="loading" title="召唤宠物中" mascot="🐱" />

    <template v-else>
    <view class="pet-layout">
      <!-- 左侧：宠物展示 -->
      <view class="pet-panel">
        <view class="pet-display">
          <view class="pet-stage animate-bounce-slow">
            <text class="pet-emoji">{{ petStore.currentImageUrl }}</text>
          </view>

          <view class="pet-name-row">
            <text class="text-xl text-bold" style="color: #2C3E50;">{{ petStore.name }}</text>
            <view class="level-badge">Lv.{{ petStore.level }}</view>
          </view>
          <text v-if="petStore.evolutionName" class="text-sm" style="color: #E74C3C; font-weight: bold; margin-top: 4px;">{{ petStore.evolutionName }}</text>

          <view class="wallet-row">
            <view class="wallet-pill gold-pill">
              <text class="wallet-icon">🪙</text>
              <text class="wallet-amount">{{ userStore.gold || 0 }}</text>
            </view>
            <view class="wallet-pill diamond-pill">
              <text class="wallet-icon">💎</text>
              <text class="wallet-amount">{{ userStore.userInfo?.diamond || 0 }}</text>
            </view>
          </view>
        </view>

        <!-- 进化进度 -->
        <view class="evolution-section">
          <view class="evo-header">
            <text class="text-sm text-bold" style="color: #2C3E50;">进化进度</text>
            <text class="text-xs text-bold" style="color: #FF6B6B;">{{ petStore.expInLevel }} / {{ petStore.nextLevelExp }} XP</text>
          </view>
          <view class="progress-bar-container">
            <tn-line-progress :percent="petStore.nextLevelExp > 0 ? petStore.expInLevel / petStore.nextLevelExp * 100 : 0" active-color="#FF6B6B" inactive-color="#FFE0E0" :height="14" :show-percent="false" />
          </view>
        </view>

        <!-- 状态条 -->
        <view class="stats-bars">
          <view class="stat-row">
            <view class="stat-label-wrap">
              <text class="stat-icon">🍗</text>
              <text class="stat-label text-sm text-bold">饱食</text>
            </view>
            <tn-line-progress :percent="petStore.hunger" active-color="#FFB74D" inactive-color="#FFF0D4" :height="12" :show-percent="false" style="flex: 1;" />
            <text class="stat-value text-xs text-bold">{{ petStore.hunger }}%</text>
          </view>
          <view class="stat-row">
            <view class="stat-label-wrap">
              <text class="stat-icon">💖</text>
              <text class="stat-label text-sm text-bold">心情</text>
            </view>
            <tn-line-progress :percent="petStore.moodPercent" active-color="#4ECDC4" inactive-color="#D4F0ED" :height="12" :show-percent="false" style="flex: 1;" />
            <text class="stat-value text-xs text-bold">{{ petStore.moodText }}</text>
          </view>
          <view class="stat-row">
            <view class="stat-label-wrap">
              <text class="stat-icon">⚡</text>
              <text class="stat-label text-sm text-bold">活力</text>
            </view>
            <tn-line-progress :percent="petStore.energy" active-color="#2ECC71" inactive-color="#D4F0E0" :height="12" :show-percent="false" style="flex: 1;" />
            <text class="stat-value text-xs text-bold">{{ petStore.energy }}%</text>
          </view>
        </view>

        <!-- 操作按钮 -->
        <view class="action-grid">
          <view class="action-btn-item card card-hover" style="background: linear-gradient(135deg, #FFEFD5, #FFDAB9);" @tap="handleFeed">
            <text class="action-emoji">🍖</text>
            <text class="text-sm text-bold" style="color: #D35400;">喂食</text>
          </view>
          <view class="action-btn-item card card-hover" style="background: linear-gradient(135deg, #E0FFFF, #AFEEEE);" @tap="handleBath">
            <text class="action-emoji">🛁</text>
            <text class="text-sm text-bold" style="color: #008080;">洗澡</text>
          </view>
          <view class="action-btn-item card card-hover" style="background: linear-gradient(135deg, #F0FFF0, #98FB98);" @tap="handlePlay">
            <text class="action-emoji">🎾</text>
            <text class="text-sm text-bold" style="color: #2E8B57;">玩耍</text>
          </view>
          <view class="action-btn-item card card-hover" style="background: linear-gradient(135deg, #F8F8FF, #E6E6FA);" @tap="goDress">
            <text class="action-emoji">👔</text>
            <text class="text-sm text-bold" style="color: #483D8B;">换装</text>
          </view>
          <view class="action-btn-item card card-hover" style="background: linear-gradient(135deg, #FFF0F5, #FFB6C1); grid-column: 1 / -1;" @tap="openPetPicker">
            <text class="action-emoji">🐾</text>
            <text class="text-sm text-bold" style="color: #C71585;">更换宠物伙伴</text>
          </view>
        </view>
      </view>

      <!-- 右侧：背包/物品 -->
      <view class="inventory-panel">
        <!-- 背包食物 -->
        <view class="inventory-section card">
          <view class="inv-header">
            <view style="display:flex;align-items:center;gap:8px">
              <text class="inv-title-icon">🎒</text>
              <text class="text-lg text-bold" style="color: #2C3E50;">背包食物</text>
            </view>
            <view class="shop-btn" @tap="goShop">
              <text class="text-sm text-bold">🛒 去商店</text>
            </view>
          </view>

          <scroll-view scroll-y style="flex:1; min-height: 200px;">
            <view v-if="foodItems.length > 0" class="item-grid">
              <view v-for="item in foodItems" :key="item.id" class="item-card card-hover" :class="{ empty: item.count <= 0 }" @tap="selectFood(item)">
                <view class="item-icon-bg">
                  <text class="item-emoji">{{ item.icon }}</text>
                </view>
                <text class="text-sm text-bold" style="margin-top:8px">{{ item.name }}</text>
                <view class="item-count-badge">x{{ item.count }}</view>
              </view>
            </view>
            <view v-else class="empty-hint">
              <text class="empty-emoji">🥣</text>
              <text class="text-sm text-light" style="margin-top:8px">背包空空，去商店买点好吃的吧</text>
            </view>
          </scroll-view>
        </view>

        <!-- 装扮物品 -->
        <view class="inventory-section card">
          <view class="inv-header">
            <view style="display:flex;align-items:center;gap:8px">
              <text class="inv-title-icon">🎭</text>
              <text class="text-lg text-bold" style="color: #2C3E50;">我的装扮</text>
            </view>
          </view>
          <scroll-view scroll-y style="flex:1; min-height: 160px;">
            <view v-if="costumeItems.length > 0" class="item-grid">
              <view v-for="item in costumeItems" :key="item.id" class="item-card">
                <view class="item-icon-bg" style="background: #F4ECF7;">
                  <text class="item-emoji">{{ item.icon }}</text>
                </view>
                <text class="text-sm text-bold" style="margin-top:8px">{{ item.name }}</text>
                <text class="text-xs" style="color: #8E44AD; font-weight:bold; margin-top:4px;">已拥有</text>
              </view>
            </view>
            <view v-else class="empty-hint">
              <text class="empty-emoji">👕</text>
              <text class="text-sm text-light" style="margin-top:8px">暂无装扮，快去收集吧</text>
            </view>
          </scroll-view>
        </view>
      </view>
    </view>

    <!-- 喂食弹窗 -->
    <tn-popup v-model="showFeedModal" direction="center" :custom-style="{ width: '420px', borderRadius: '24px' }">
      <view class="feed-modal">
        <text class="text-xl text-bold" style="margin-bottom: 20px; display:block; text-align:center; color:#D35400;">🍖 要喂哪个食物？</text>
        <view v-if="availableFood.length > 0" class="feed-grid">
          <view
            v-for="item in availableFood"
            :key="item.id"
            class="feed-item card"
            :class="{ selected: selectedFood === item.id }"
            @tap="selectedFood = item.id"
          >
            <text class="feed-emoji">{{ item.icon }}</text>
            <text class="text-md text-bold" style="margin-top:8px">{{ item.name }}</text>
            <view class="item-count-badge">余: {{ item.count }}</view>
          </view>
        </view>
        <view v-else class="empty-hint" style="padding: 40px 0;">
          <text class="text-md text-light">没有可用的食物了哦</text>
        </view>
        <view style="display: flex; gap: 16px; margin-top: 24px;">
          <tn-button shape="round" size="lg" @click="showFeedModal = false" style="flex:1; background:#F5F5F5; color:#666; border:none;">不吃了</tn-button>
          <tn-button type="primary" shape="round" size="lg" @click="confirmFeed" :disabled="!selectedFood" style="flex:1; background:linear-gradient(135deg, #FFB74D, #FF8C00); border:none;">确认喂食</tn-button>
        </view>
      </view>
    </tn-popup>

    <!-- 换宠物弹窗 -->
    <tn-popup v-model="showPetPicker" direction="center" :custom-style="{ width: '520px', maxHeight: '80vh', borderRadius: '24px' }">
      <view class="pet-picker-modal">
        <text class="text-xl text-bold" style="margin-bottom: 20px; display:block; text-align:center; color:#2C3E50;">🌟 召唤新的小伙伴</text>
        <scroll-view scroll-y style="max-height: 400px;">
          <view class="pet-picker-grid">
            <view v-for="pet in availablePets" :key="pet.id"
              class="pet-picker-item card"
              :class="{ selected: pickedPetId === pet.id }"
              @tap="pickedPetId = pet.id">
              <view class="pet-picker-bg">
                <text class="pet-emoji">{{ pet.imageUrl }}</text>
              </view>
              <text class="text-md text-bold" style="margin-top:12px; color:#34495E;">{{ pet.petName }}</text>
            </view>
          </view>
        </scroll-view>
        <view style="display: flex; gap: 16px; margin-top: 24px;">
          <tn-button shape="round" size="lg" @click="showPetPicker = false" style="flex:1; background:#F5F5F5; color:#666; border:none;">取消</tn-button>
          <tn-button type="primary" shape="round" size="lg" @click="confirmPetChange" :disabled="!pickedPetId" style="flex:1; background:linear-gradient(135deg, #4A90D9, #6BA3E0); border:none;">确认召唤</tn-button>
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
import { getPetStatus, getInventory, getDecorationInventory, feedPet as feedPetApi, playPet as playPetApi, bathPet as bathPetApi, getAvailablePets, selectPet } from '@/api/pet'
import { normalizeInventoryItems, normalizeDecorations } from '@/utils/petFeature.mjs'
import FunLoadingState from '@/components/common/FunLoadingState.vue'

const petStore = usePetStore()
const userStore = useUserStore()

const loading = ref(true)
const showFeedModal = ref(false)
const selectedFood = ref(null)
const showPetPicker = ref(false)
const availablePets = ref([])
const pickedPetId = ref(null)

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

async function openPetPicker() {
  pickedPetId.value = null
  try {
    const res = await getAvailablePets()
    availablePets.value = res.data || res || []
  } catch (_) {}
  showPetPicker.value = true
}

async function confirmPetChange() {
  if (!pickedPetId.value) return
  try {
    const res = await selectPet(pickedPetId.value)
    if (res.code === 200 || res) {
      petStore.setPetInfo(res.data || res)
      showPetPicker.value = false
      uni.showToast({ title: '更换成功！', icon: 'success' })
      loadData()
    }
  } catch (e) {
    uni.showToast({ title: e?.msg || '更换失败', icon: 'none' })
  }
}

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
  grid-template-columns: 360px 1fr;
  gap: 20px;
  height: 100%;
}

.pet-panel {
  background: linear-gradient(135deg, #FFF0F5, #FFD4E5);
  border: 4px solid #FFF;
  border-radius: $radius-xl;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  box-shadow: 0 12px 36px rgba(255, 182, 193, 0.3);
}

.pet-display {
  text-align: center;
  padding: 16px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.pet-stage {
  width: 140px;
  height: 140px;
  background: rgba(255,255,255,0.6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  box-shadow: inset 0 -8px 12px rgba(0,0,0,0.05), 0 12px 24px rgba(255, 182, 193, 0.4);
}

.pet-emoji { font-size: 80px; }

.pet-name-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.level-badge {
  background: #FF6B6B;
  color: #FFF;
  font-size: 12px;
  font-weight: 900;
  padding: 4px 10px;
  border-radius: 12px;
  box-shadow: 0 4px 8px rgba(255, 107, 107, 0.3);
}

.wallet-row {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}

.wallet-pill {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #FFF;
  border-radius: 20px;
  padding: 6px 14px;
  font-weight: 800;
  box-shadow: 0 4px 12px rgba(0,0,0,0.06);
}

.gold-pill .wallet-amount { color: #F39C12; }
.diamond-pill .wallet-amount { color: #3498DB; }
.wallet-icon { font-size: 16px; }

.evolution-section {
  background: rgba(255, 255, 255, 0.8);
  border-radius: $radius-lg;
  padding: 14px 18px;
}

.evo-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.stats-bars {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: $radius-lg;
  padding: 16px 18px;
}

.stat-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stat-label-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 68px;
}
.stat-icon { font-size: 16px; }

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.action-btn-item {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 16px;
  cursor: pointer;
  border: none;
  box-shadow: 0 6px 16px rgba(0,0,0,0.06);
  border-radius: 20px;
  &:active { transform: scale(0.94); }
}

.action-emoji { font-size: 24px; }

.inventory-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.inventory-section {
  padding: 20px 24px;
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #FFF;
  border-radius: $radius-xl;
  box-shadow: 0 4px 24px rgba(0,0,0,0.02);
  border: 1px solid rgba(0,0,0,0.02);
}

.inv-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px dashed #F0F0F0;
}

.inv-title-icon { font-size: 24px; }

.shop-btn {
  background: #EEF6FF;
  color: #4A90D9;
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  &:active { background: #DBEAFE; }
}

.item-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 16px;
  padding: 8px 0;
}

.item-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 12px;
  background: #F8F9FA;
  border-radius: 20px;
  border: 2px solid transparent;
  position: relative;
  transition: all 0.2s;

  &.empty {
    opacity: 0.5;
    filter: grayscale(1);
  }
}

.item-icon-bg {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: #FFF;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0,0,0,0.04);
}

.item-emoji { font-size: 36px; }

.item-count-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  background: #E74C3C;
  color: #FFF;
  font-size: 11px;
  font-weight: 900;
  padding: 2px 8px;
  border-radius: 12px;
  border: 2px solid #FFF;
  box-shadow: 0 2px 6px rgba(231,76,60,0.3);
}

.empty-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  height: 100%;
}
.empty-emoji { font-size: 48px; opacity: 0.5; margin-bottom: 12px; }

.feed-modal {
  padding: 10px;
}

.feed-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.feed-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px;
  cursor: pointer;
  border: 3px solid transparent;
  background: #F8F9FA;
  border-radius: 20px;

  &.selected {
    border-color: #FFB74D;
    background: #FFF8E6;
    transform: scale(1.02);
  }
}

.feed-emoji { font-size: 48px; }

.pet-picker-modal {
  padding: 10px;
}
.pet-picker-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  padding: 10px;
}
.pet-picker-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 16px;
  border: 3px solid transparent;
  background: #F8F9FA;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s;

  &.selected {
    border-color: #4A90D9;
    background: #EEF6FF;
    transform: scale(1.05);
  }
}
.pet-picker-bg {
  width: 72px;
  height: 72px;
  background: #FFF;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}
.pet-picker-item .pet-emoji { font-size: 48px; }

@media (max-width: 800px) {
  .pet-layout { grid-template-columns: 300px 1fr; gap: 16px; }
  .pet-emoji { font-size: 64px; }
  .pet-stage { width: 110px; height: 110px; }
  .item-grid { grid-template-columns: repeat(auto-fill, minmax(80px, 1fr)); }
}

@media (max-width: 640px) {
  .pet-layout { grid-template-columns: 1fr; }
  .pet-panel { padding: 20px; }
  .pet-picker-grid { grid-template-columns: repeat(2, 1fr); }
}

/* Animations */
.animate-bounce-slow {
  animation: bounce-slow 3s ease-in-out infinite;
}
@keyframes bounce-slow {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}
</style>
