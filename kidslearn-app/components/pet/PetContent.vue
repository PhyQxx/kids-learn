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
          <text class="pet-emoji animate-float">🐱</text>
          <view class="pet-name-row">
            <text class="text-lg text-bold">{{ petStore.name }}</text>
            <tn-badge type="primary" :text="'Lv.' + petStore.level" />
          </view>
        </view>

        <!-- 进化进度 -->
        <view class="evolution-section">
          <view class="evo-header">
            <text class="text-sm text-bold">进化进度</text>
            <text class="text-xs text-light">{{ petExp.current }}/{{ petExp.max }} XP</text>
          </view>
          <tn-line-progress :percent="petExp.current / petExp.max * 100" active-color="#FF6B6B" inactive-color="rgba(255,255,255,0.3)" :height="12" :show-percent="false" />
        </view>

        <!-- 状态条 -->
        <view class="stats-bars">
          <view class="stat-row">
            <text class="stat-label text-sm">🍖 饱食度</text>
            <tn-line-progress :percent="petStore.hunger" active-color="#FFB74D" :height="12" :show-percent="false" style="flex: 1;" />
            <text class="text-xs text-light">{{ petStore.hunger }}%</text>
          </view>
          <view class="stat-row">
            <text class="stat-label text-sm">😊 心情</text>
            <tn-line-progress :percent="petMood" active-color="#4ECDC4" :height="12" :show-percent="false" style="flex: 1;" />
            <text class="text-xs text-light">{{ petStore.moodText }}</text>
          </view>
          <view class="stat-row">
            <text class="stat-label text-sm">⚡ 活力</text>
            <tn-line-progress :percent="petEnergy" active-color="#2ECC71" :height="12" :show-percent="false" style="flex: 1;" />
            <text class="text-xs text-light">{{ petEnergy }}%</text>
          </view>
        </view>

        <!-- 操作按钮 -->
        <view class="action-grid">
          <view class="action-btn-item card card-hover" @tap="feedPet">
            <text class="action-emoji">🍖</text>
            <text class="text-sm">喂食</text>
          </view>
          <view class="action-btn-item card card-hover" @tap="bathPet">
            <text class="action-emoji">🛁</text>
            <text class="text-sm">洗澡</text>
          </view>
          <view class="action-btn-item card card-hover" @tap="playPet">
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
          <view class="item-grid">
            <view v-for="item in foodItems" :key="item.id" class="item-card" :class="{ empty: item.count === 0 }">
              <text class="item-emoji">{{ item.icon }}</text>
              <text class="text-xs">{{ item.name }}</text>
              <text class="text-xs text-light">x{{ item.count }}</text>
            </view>
          </view>
        </view>

        <!-- 装扮物品 -->
        <view class="inventory-section card">
          <view class="inv-header">
            <text class="text-md text-bold">🎭 装扮</text>
          </view>
          <view class="item-grid">
            <view v-for="item in costumeItems" :key="item.id" class="item-card" :class="{ locked: !item.owned }">
              <text class="item-emoji">{{ item.icon }}</text>
              <text class="text-xs">{{ item.name }}</text>
              <text v-if="item.owned" class="text-xs text-success">已拥有</text>
              <text v-else class="text-xs text-light">🔒</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 喂食弹窗 -->
    <tn-popup v-model="showFeedModal" direction="center" :custom-style="{ width: '400px' }">
      <view class="feed-modal">
        <text class="text-lg text-bold" style="margin-bottom: 16px;">🍖 选择食物</text>
        <view class="feed-grid">
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
import { getPetStatus, getInventory, feedPet as feedPetApi, playPet as playPetApi } from '@/api/pet'

const petStore = usePetStore()

const loading = ref(true)
const showFeedModal = ref(false)
const selectedFood = ref(null)
const petExp = ref({ current: 300, max: 500 })
const petMood = ref(85)
const petEnergy = ref(90)

const foodItems = ref([
  { id: 1, name: '苹果', icon: '🍎', count: 3 },
  { id: 2, name: '蛋糕', icon: '🎂', count: 1 },
  { id: 3, name: '鱼', icon: '🐟', count: 5 },
  { id: 4, name: '星星糖', icon: '🍬', count: 2 },
  { id: 5, name: '披萨', icon: '🍕', count: 0 },
  { id: 6, name: '钻石果', icon: '💎', count: 0 }
])

const costumeItems = ref([
  { id: 1, name: '帽子', icon: '🎩', owned: true },
  { id: 2, name: '蝴蝶结', icon: '🎀', owned: true },
  { id: 3, name: '眼镜', icon: '🕶️', owned: false },
  { id: 4, name: '盾牌', icon: '🛡️', owned: false }
])

const availableFood = computed(() => foodItems.value.filter(f => f.count > 0))

function applyMockData() {
  petExp.value = { current: 300, max: 500 }
  petMood.value = 85
  petEnergy.value = 90
  foodItems.value = [
    { id: 1, name: '苹果', icon: '🍎', count: 3 },
    { id: 2, name: '蛋糕', icon: '🎂', count: 1 },
    { id: 3, name: '鱼', icon: '🐟', count: 5 },
    { id: 4, name: '星星糖', icon: '🍬', count: 2 },
    { id: 5, name: '披萨', icon: '🍕', count: 0 },
    { id: 6, name: '钻石果', icon: '💎', count: 0 }
  ]
  costumeItems.value = [
    { id: 1, name: '帽子', icon: '🎩', owned: true },
    { id: 2, name: '蝴蝶结', icon: '🎀', owned: true },
    { id: 3, name: '眼镜', icon: '🕶️', owned: false },
    { id: 4, name: '盾牌', icon: '🛡️', owned: false }
  ]
}

async function loadData() {
  loading.value = true
  try {
    const results = await Promise.allSettled([
      getPetStatus(),
      getInventory()
    ])

    // 宠物状态
    if (results[0].status === 'fulfilled' && results[0].value) {
      const pet = results[0].value
      petStore.setPetInfo(pet)
      petExp.value = { current: pet.currentExp || 300, max: pet.nextLevelExp || 500 }
      petMood.value = pet.moodValue || pet.mood || 85
      petEnergy.value = pet.energy || 90
    }

    // 背包物品
    if (results[1].status === 'fulfilled' && results[1].value) {
      const inv = results[1].value
      if (inv.foods && Array.isArray(inv.foods) && inv.foods.length > 0) {
        foodItems.value = inv.foods.map(f => ({
          id: f.id,
          name: f.itemName || f.name,
          icon: f.iconUrl || f.icon || '🍽️',
          count: f.count || 0
        }))
      }
      if (inv.costumes && Array.isArray(inv.costumes) && inv.costumes.length > 0) {
        costumeItems.value = inv.costumes.map(c => ({
          id: c.id,
          name: c.itemName || c.name,
          icon: c.iconUrl || c.icon || '🎭',
          owned: c.owned !== false
        }))
      }
    }
  } catch (e) {
    console.log('PetContent: 使用模拟数据', e)
    applyMockData()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})

function feedPet() { showFeedModal.value = true }
function bathPet() { uni.showToast({ title: '洗澡啦~ 🛁', icon: 'none' }) }
function playPet() {
  playPetApi().then(() => {
    uni.showToast({ title: '玩耍中~ 🎾', icon: 'none' })
    loadData()
  }).catch(() => {
    uni.showToast({ title: '玩耍中~ 🎾', icon: 'none' })
  })
}
function goDress() { uni.navigateTo({ url: '/pages/pet/dress' }) }
function goShop() { uni.navigateTo({ url: '/pages/pet/shop' }) }

function confirmFeed() {
  if (!selectedFood.value) return
  const food = foodItems.value.find(f => f.id === selectedFood.value)
  if (food && food.count > 0) {
    feedPetApi(food.id).then(() => {
      food.count--
      uni.showToast({ title: `${petStore.name}吃了${food.name}！`, icon: 'none' })
      loadData()
    }).catch(() => {
      food.count--
      uni.showToast({ title: `${petStore.name}吃了${food.name}！`, icon: 'none' })
    })
    showFeedModal.value = false
    selectedFood.value = null
  }
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

/* 左侧宠物面板 */
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

.stat-label { width: 64px; }

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

/* 右侧物品面板 */
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
  &.locked { opacity: 0.5; }
}

.item-emoji { font-size: 28px; }

/* 喂食弹窗 */
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

/* 响应式 */
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
