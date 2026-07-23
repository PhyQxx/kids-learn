<template>
  <AppLayout theme="kids" title="宠物商店" :show-back="true" active-nav="/pages/pet/index">
    <view class="shop-content">
      <!-- 金币余额 -->
      <view class="balance-bar">
        <text class="balance-label">账户余额</text>
        <text class="text-lg text-bold">{{ userStore.gold || 0 }} 金币</text>
        <text class="text-sm text-light">钻石 {{ userStore.userInfo?.diamond || 0 }}</text>
      </view>

      <!-- 分类 -->
      <tn-tabs v-model="activeTab" active-color="#FF6B6B">
        <tn-tabs-item v-for="tab in tabItems" :key="tab.label" :title="tab.label" />
      </tn-tabs>

      <!-- 商品网格 -->
      <view v-if="currentItems.length > 0" class="shop-grid">
        <view v-for="item in currentItems" :key="item.id" class="shop-card card">
          <view class="shop-icon-wrap" :style="{ background: item.bg }">
            <image v-if="isImageUrl(item.icon)" class="shop-emoji-img" :src="item.icon" mode="aspectFit" />
            <text v-else class="shop-emoji">{{ item.icon }}</text>
          </view>
          <text class="text-sm text-bold">{{ item.name }}</text>
          <text v-if="item.effectDesc" class="text-xs text-light">{{ item.effectDesc }}</text>
          <view class="shop-meta">
            <view class="price-tag">
              <text class="text-xs text-bold text-primary">{{ item.priceType === 'diamond' ? '晶石' : '金币' }} {{ item.price }}</text>
            </view>
          </view>
          <tn-button
            type="primary"
            size="sm"
            shape="round"
            :disabled="!item.affordable"
            @click="handleBuy(item)"
          >{{ item.affordable ? '购买' : '余额不足' }}</tn-button>
        </view>
      </view>
      <view v-else class="empty-hint">
        <text class="text-sm text-light">暂无商品</text>
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useUserStore } from '@/store/user'
import { getShopItems, buyItem as buyItemApi, getDecorations, buyDecoration as buyDecoApi } from '@/api/pet'
import { applyPetPurchaseBalance, normalizeShopItems, isImageUrl } from '@/utils/petFeature.mjs'

const userStore = useUserStore()
const activeTab = ref(0)
const tabItems = ref([
  { label: '食物' },
  { label: '玩具' },
  { label: '装扮' }
])

const allItems = ref({
  food: [],
  toy: [],
  decoration: []
})

const tabKeys = ['food', 'toy', 'decoration']

const currentItems = computed(() => allItems.value[tabKeys[activeTab.value]] || [])

const bgColors = ['#FFF0F0', '#FFF8E0', '#E0F7F7', '#F3E8FF', '#E8F0FE', '#E8F8F0']
const balance = computed(() => ({
  gold: userStore.gold || 0,
  diamond: userStore.userInfo?.diamond || 0
}))

async function loadShop() {
  const key = tabKeys[activeTab.value]
  try {
    if (key === 'decoration') {
      const res = await getDecorations()
      if (res && Array.isArray(res) && res.length > 0) {
        allItems.value.decoration = normalizeShopItems(res, balance.value)
          .map(item => ({ ...item, bg: bgColors[item.id % 6] }))
      } else {
        allItems.value.decoration = []
      }
    } else {
      const typeMap = { food: 1, toy: 2 }
      const res = await getShopItems(typeMap[key])
      if (res && Array.isArray(res) && res.length > 0) {
        allItems.value[key] = normalizeShopItems(res, balance.value)
          .map(item => ({ ...item, bg: bgColors[item.id % 6] }))
      } else {
        allItems.value[key] = []
      }
    }
  } catch (e) {
    allItems.value[key] = []
  }
}

onMounted(() => loadShop())
watch(activeTab, () => loadShop())

function handleBuy(item) {
  const currency = item.priceType === 'diamond' ? '钻石' : '金币'
  uni.showModal({
    title: '确认购买',
    content: `是否花费 ${item.price} ${currency}购买 ${item.name}？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          const key = tabKeys[activeTab.value]
          let buyRes
          if (key === 'decoration') {
            buyRes = await buyDecoApi(item.id)
          } else {
            buyRes = await buyItemApi(item.id)
          }
          uni.showToast({ title: '购买成功！', icon: 'success' })
          if (userStore.userInfo) {
            userStore.setUserInfo(applyPetPurchaseBalance(userStore.userInfo, buyRes || {}, item))
          }
          loadShop()
        } catch (e) {
          uni.showToast({ title: e.message || '购买失败', icon: 'none' })
        }
      }
    }
  })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.shop-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.balance-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  background: linear-gradient(135deg, #FFF8E0, #FFE66D);
  padding: 14px 20px;
  border-radius: $radius-md;
}

.balance-icon { font-size: 28px; }

.shop-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.shop-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 18px 12px;
}

.shop-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.shop-emoji { font-size: 28px; }
.shop-emoji-img { width: 40px; height: 40px; }

.shop-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.price-tag {
  background: #FFF0F0;
  padding: 2px 8px;
  border-radius: 100px;
}

.empty-hint {
  text-align: center;
  padding: 40px 0;
}
</style>
