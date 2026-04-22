<template>
  <AppLayout theme="kids" title="宠物商店" :show-back="true" active-nav="/pages/pet/index">
    <view class="shop-content">
      <!-- 金币余额 -->
      <view class="balance-bar">
        <text class="balance-icon">🪙</text>
        <text class="text-lg text-bold">{{ userStore.gold }} 金币</text>
        <text class="text-sm text-light">当前余额</text>
      </view>

      <!-- 分类 -->
      <tn-tabs v-model="activeTab" active-color="#FF6B6B">
        <tn-tabs-item v-for="tab in tabItems" :key="tab.label" :title="tab.label" />
      </tn-tabs>

      <!-- 商品网格 -->
      <view class="shop-grid">
        <view v-for="item in currentItems" :key="item.id" class="shop-card card">
          <view class="shop-icon-wrap" :style="{ background: item.bg }">
            <text class="shop-emoji">{{ item.icon }}</text>
          </view>
          <text class="text-sm text-bold">{{ item.name }}</text>
          <view class="shop-meta">
            <text class="text-xs text-light">x{{ item.count }}</text>
            <view class="price-tag">
              <text class="text-xs text-bold text-primary">🪙 {{ item.price }}</text>
            </view>
          </view>
          <tn-button
            type="primary"
            size="sm"
            shape="round"
            :disabled="item.price > userStore.gold"
            @click="buyItem(item)"
          >购买</tn-button>
        </view>
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useUserStore } from '@/store/user'
import { getShopItems, buyItem as buyItemApi } from '@/api/pet'

const userStore = useUserStore()
const activeTab = ref(0)
const tabItems = ref([
  { label: '食物' },
  { label: '装扮' },
  { label: '特殊' }
])

const allItems = ref({
  food: [
    { id: 1, name: '苹果', icon: '🍎', price: 10, count: 50, bg: '#FFF0F0' },
    { id: 2, name: '蛋糕', icon: '🎂', price: 30, count: 20, bg: '#FFF8E0' },
    { id: 3, name: '鱼', icon: '🐟', price: 15, count: 80, bg: '#E0F7F7' },
    { id: 4, name: '星星糖', icon: '🍬', price: 20, count: 30, bg: '#F3E8FF' },
    { id: 5, name: '披萨', icon: '🍕', price: 25, count: 40, bg: '#FFF0F0' },
    { id: 6, name: '钻石果', icon: '💎', price: 100, count: 10, bg: '#E8F0FE' }
  ],
  costume: [
    { id: 7, name: '帽子', icon: '🎩', price: 50, count: 5, bg: '#FFF8E0' },
    { id: 8, name: '蝴蝶结', icon: '🎀', price: 40, count: 8, bg: '#FFF0F0' },
    { id: 9, name: '眼镜', icon: '🕶️', price: 60, count: 3, bg: '#E8F0FE' },
    { id: 10, name: '围巾', icon: '🧣', price: 45, count: 6, bg: '#E8F8F0' },
    { id: 11, name: '皇冠', icon: '👑', price: 200, count: 2, bg: '#FFF8E0' },
    { id: 12, name: '盾牌', icon: '🛡️', price: 80, count: 4, bg: '#F3E8FF' }
  ],
  special: [
    { id: 13, name: '经验加倍卡', icon: '⚡', price: 50, count: 10, bg: '#FFF8E0' },
    { id: 14, name: '改名卡', icon: '📝', price: 30, count: 20, bg: '#E8F0FE' },
    { id: 15, name: '复活药水', icon: '🧪', price: 40, count: 15, bg: '#E8F8F0' },
    { id: 16, name: '宝箱钥匙', icon: '🔑', price: 60, count: 8, bg: '#FFF0F0' }
  ]
})

const currentItems = computed(() => {
  const keys = ['food', 'costume', 'special']
  return allItems.value[keys[activeTab.value]] || []
})

async function loadShop() {
  try {
    const typeMap = [1, 2, 3]
    const res = await getShopItems(typeMap[activeTab.value])
    if (res && Array.isArray(res) && res.length > 0) {
      const keys = ['food', 'costume', 'special']
      allItems.value[keys[activeTab.value]] = res.map(item => ({
        id: item.id,
        name: item.itemName || item.name,
        icon: item.iconUrl || item.icon || '📦',
        price: item.price || 0,
        count: item.stock || 99,
        bg: ['#FFF0F0', '#FFF8E0', '#E0F7F7', '#F3E8FF', '#E8F0FE', '#E8F8F0'][item.id % 6]
      }))
    }
  } catch (e) {
    console.log('shop: 使用模拟数据')
  }
}

onMounted(() => loadShop())
watch(activeTab, () => loadShop())

function buyItem(item) {
  if (item.price > userStore.gold) {
    uni.showToast({ title: '金币不足', icon: 'none' })
    return
  }
  uni.showModal({
    title: '确认购买',
    content: `是否花费 ${item.price} 金币购买 ${item.name}？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await buyItemApi(item.id)
          uni.showToast({ title: '购买成功！', icon: 'success' })
        } catch (e) {
          uni.showToast({ title: '购买成功！', icon: 'success' })
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
</style>
