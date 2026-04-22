<template>
  <AppLayout theme="kids" title="答题结果" :show-back="true">
    <view class="result-container">
      <text class="result-emoji animate-pop-in">🎉</text>
      <view class="result-stars">
        <text v-for="s in 3" :key="s" class="result-star" :class="{ filled: s <= 3 }">⭐</text>
      </view>
      <text class="result-title text-title text-bold">太棒了！</text>
      <text class="result-subtitle text-light">你获得了满星评价！</text>

      <view class="reward-row">
        <view class="reward-card card">
          <text class="reward-icon">🪙</text>
          <text class="text-md text-bold">+30</text>
          <text class="text-xs text-light">金币</text>
        </view>
        <view class="reward-card card">
          <text class="reward-icon">⚡</text>
          <text class="text-md text-bold">+25</text>
          <text class="text-xs text-light">经验</text>
        </view>
        <view class="reward-card card">
          <text class="reward-icon">🎨</text>
          <text class="text-md text-bold">x1</text>
          <text class="text-xs text-light">贴纸</text>
        </view>
      </view>

      <view class="stats-card card">
        <view class="stat-row"><text class="text-sm text-light">正确率</text><text class="text-sm text-bold">100%</text></view>
        <view class="stat-row"><text class="text-sm text-light">用时</text><text class="text-sm text-bold">45秒</text></view>
        <view class="stat-row"><text class="text-sm text-light">得分</text><text class="text-sm text-bold text-primary">50分</text></view>
      </view>

      <view class="actions">
        <tn-button type="primary" size="lg" shape="round" @click="goNext">下一关</tn-button>
        <tn-button size="lg" shape="round" @click="goBack">返回关卡列表</tn-button>
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import AppLayout from '@/components/AppLayout.vue'

function goNext() { uni.navigateBack() }
function goBack() { uni.navigateBack() }
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.result-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 20px;
}

.result-emoji { font-size: 80px; }
.result-stars { display: flex; gap: 8px; font-size: 40px; }
.result-star { color: #E0E0E0; &.filled { color: $gold; } }
.result-title { text-align: center; }
.result-subtitle { text-align: center; }

.reward-row { display: flex; gap: 12px; }
.reward-card { padding: 12px 20px; text-align: center; min-width: 80px; }
.reward-icon { font-size: 24px; display: block; margin-bottom: 4px; }

.stats-card { width: 300px; padding: 16px; }
.stat-row {
  display: flex; justify-content: space-between; padding: 6px 0;
  & + .stat-row { border-top: 1px solid #F5F5F5; }
}

.actions { display: flex; gap: 12px; margin-top: 4px; }
</style>
