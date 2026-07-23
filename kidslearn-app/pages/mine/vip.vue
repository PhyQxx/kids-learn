<template>
  <AppLayout theme="kids" title="会员中心" active-nav="/pages/mine/vip">
    <view class="vip-content">
      <view class="hero-banner">
        <text class="hero-crown animate-float">👑</text>
        <text class="text-2xl text-bold text-gold">VIP会员</text>
        <text class="status-text">{{ currentVipText }}</text>
      </view>

      <view class="plan-grid">
        <view
          v-for="plan in plans"
          :key="plan.planType"
          class="plan-card card"
          :class="{ featured: plan.featured }"
        >
          <view v-if="plan.featured" class="featured-tag"><text class="text-xs text-bold">推荐</text></view>
          <text class="plan-name">{{ plan.name }}</text>
          <view class="plan-price">
            <text class="plan-price-value" :class="{ gold: plan.featured }">{{ plan.priceText }}</text>
            <text class="plan-price-unit" :class="{ gold: plan.featured }">{{ plan.unitText }}</text>
          </view>
          <text class="original-text">{{ plan.originalText || '专属会员权益' }}</text>
          <tn-button
            :type="plan.featured ? 'warning' : undefined"
            shape="round"
            size="lg"
            block
            :loading="loadingPlanType === plan.planType"
            @click="buyPlan(plan)"
          >
            {{ paymentsEnabled ? '开通' : '筹备中' }}
          </tn-button>
        </view>
      </view>

      <view class="compare-card card">
        <text class="text-md text-bold compare-title">特权对比</text>
        <view class="compare-header">
          <text class="compare-col">特权</text>
          <text class="compare-col">免费</text>
          <text class="compare-col gold">VIP</text>
        </view>
        <view v-for="feature in features" :key="feature.name" class="compare-row">
          <text class="compare-col text-sm">{{ feature.name }}</text>
          <text class="compare-col">{{ feature.free ? '有' : '无' }}</text>
          <text class="compare-col gold">有</text>
        </view>
      </view>

      <view class="faq-card card">
        <text class="text-md text-bold faq-title">常见问题</text>
        <view v-for="faq in faqs" :key="faq.q" class="faq-item">
          <text class="text-sm text-bold">{{ faq.q }}</text>
          <text class="text-xs text-light">{{ faq.a }}</text>
        </view>
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { getCurrentSubscription, getSubscriptionPlans } from '@/api/subscription'
import { createOrder } from '@/api/order'
import { normalizeVipPlans, vipStatusText } from '@/utils/vipPlans.mjs'

const plans = ref([])
const currentSubscription = ref(null)
const loadingPlanType = ref(null)
const payChannel = (import.meta.env.VITE_PAY_CHANNEL || '').toLowerCase()
const paymentsEnabled = import.meta.env.VITE_ENABLE_PAYMENT === 'true'
  && ['wechat', 'alipay', 'apple'].includes(payChannel)

const currentVipText = computed(() => vipStatusText(currentSubscription.value))

const features = [
  { name: '基础课程', free: true },
  { name: '全部课程', free: false },
  { name: '宠物换装', free: true },
  { name: '高级装扮', free: false },
  { name: '排行榜', free: true },
  { name: '挑战赛', free: false },
  { name: '学习报告', free: true },
  { name: 'AI辅导', free: false }
]

const faqs = [
  { q: '什么时候可以购买？', a: '会员购买功能正在筹备，开放后会在页面内通知。' },
  { q: '多设备可以使用吗？', a: 'VIP支持同一账号下的多台设备。' },
  { q: '到期后数据会丢失吗？', a: '学习数据永久保留，VIP内容将不可访问。' }
]

async function loadVipData() {
  const [plansResult, currentResult] = await Promise.allSettled([
    getSubscriptionPlans(),
    getCurrentSubscription()
  ])

  if (plansResult.status === 'fulfilled') {
    plans.value = normalizeVipPlans(plansResult.value)
  }
  if (currentResult.status === 'fulfilled') {
    currentSubscription.value = currentResult.value
  }
}

async function buyPlan(plan) {
  if (!plan?.planType || loadingPlanType.value) return
  if (!paymentsEnabled) {
    uni.showToast({ title: '会员购买功能筹备中', icon: 'none' })
    return
  }
  loadingPlanType.value = plan.planType
  try {
    const order = await createOrder(plan.planType, payChannel)
    uni.showModal({
      title: '订单已创建',
      content: `订单号：${order.orderNo}\n金额：¥${order.amount}`,
      showCancel: false,
      confirmText: '知道了'
    })
  } catch (e) {
    uni.showToast({ title: '创建订单失败', icon: 'none' })
  } finally {
    loadingPlanType.value = null
  }
}

onMounted(loadVipData)
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.vip-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-banner {
  text-align: center;
  padding: 32px;
  background: #fff;
  border: 1px solid rgba(73, 98, 128, 0.08);
  border-radius: $radius-lg;
  box-shadow: 0 8px 24px rgba(73, 98, 128, 0.08);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.hero-crown { font-size: 64px; }
.text-gold { color: $gold; }
.status-text {
  color: $text-secondary;
  font-size: 15px;
}

.status-text,
.original-text { opacity: 0.8; }

.plan-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.plan-card {
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  background: #fff;
  border: 1px solid rgba(73, 98, 128, 0.08);
  border-radius: $radius-lg;
  box-shadow: 0 8px 24px rgba(73, 98, 128, 0.08);
  position: relative;

  &.featured {
    border-color: $gold;
    background: #FFF9E6;
  }
}

.featured-tag {
  position: absolute;
  top: -1px;
  right: 16px;
  background: $gold;
  color: $text;
  padding: 2px 12px;
  border-radius: 0 0 8px 8px;
}

.plan-name { font-size: 16px; color: $text-secondary; }
.plan-price { display: flex; align-items: baseline; gap: 4px; }

.plan-price-value {
  color: $primary;
  font-size: 32px;
  font-weight: 800;

  &.gold {
    color: $gold;
  }
}

.plan-price-unit {
  color: $text-light;
  font-size: 12px;

  &.gold {
    color: $gold;
  }
}

.original-text {
  color: $text-light;
  font-size: 12px;
}

.compare-card,
.faq-card {
  padding: 16px 20px;
  background: #fff;
  border: 1px solid rgba(73, 98, 128, 0.08);
  border-radius: $radius-lg;
  box-shadow: 0 8px 24px rgba(73, 98, 128, 0.08);
}

.compare-title,
.faq-title {
  margin-bottom: 12px;
}

.compare-header,
.compare-row {
  display: flex;
  padding: 8px 0;
  color: $text-secondary;

  & + .compare-row {
    border-top: 1px solid rgba(73, 98, 128, 0.08);
  }
}

.compare-col {
  flex: 1;
  text-align: center;

  &.gold {
    color: $gold;
    font-weight: 600;
  }
}

.faq-item {
  padding: 10px 0;
  display: flex;
  flex-direction: column;
  gap: 4px;

  & + .faq-item {
    border-top: 1px solid rgba(73, 98, 128, 0.08);
  }
}

@include respond-md-lg {
  .plan-grid {
    grid-template-columns: 1fr;
  }
}
</style>
