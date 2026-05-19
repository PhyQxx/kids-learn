<template>
  <view class="ranking-content">
    <!-- Loading -->
    <FunLoadingState v-if="loading" title="正在核对排行榜" mascot="🏆" />

    <template v-else>
    <!-- 领奖台 -->
    <view class="podium-section">
      <view class="podium-row">
        <!-- 第2名 -->
        <view class="podium-item">
          <view class="podium-avatar silver-bg">
            <text class="podium-emoji">{{ podiumData[1]?.avatar || '🐰' }}</text>
          </view>
          <text class="text-sm text-bold">{{ podiumData[1]?.name || '-' }}</text>
          <text class="text-xs text-light">{{ podiumData[1]?.score || 0 }} 分</text>
          <view class="pedestal silver-ped">
            <text class="ped-rank">2</text>
          </view>
        </view>
        <!-- 第1名 -->
        <view class="podium-item first">
          <text class="crown">👑</text>
          <view class="podium-avatar gold-bg">
            <text class="podium-emoji">{{ podiumData[0]?.avatar || '🦁' }}</text>
          </view>
          <text class="text-sm text-bold">{{ podiumData[0]?.name || '-' }}</text>
          <text class="text-xs text-light">{{ podiumData[0]?.score || 0 }} 分</text>
          <view class="pedestal gold-ped">
            <text class="ped-rank">1</text>
          </view>
        </view>
        <!-- 第3名 -->
        <view class="podium-item">
          <view class="podium-avatar bronze-bg">
            <text class="podium-emoji">{{ podiumData[2]?.avatar || '🦊' }}</text>
          </view>
          <text class="text-sm text-bold">{{ podiumData[2]?.name || '-' }}</text>
          <text class="text-xs text-light">{{ podiumData[2]?.score || 0 }} 分</text>
          <view class="pedestal bronze-ped">
            <text class="ped-rank">3</text>
          </view>
        </view>
      </view>
    </view>

    <!-- Tab切换 -->
    <tn-tabs v-model="activeTab" active-color="#FF6B6B">
      <tn-tabs-item v-for="tab in tabItems" :key="tab.label" :title="tab.label" />
    </tn-tabs>

    <view class="challenge-entry card" @tap="goChallenge">
      <view class="challenge-entry-copy">
        <text class="challenge-entry-icon">⚔️</text>
        <view>
          <text class="text-md text-bold">好友PK与段位赛</text>
          <text class="text-xs text-light">完成挑战赢金币，提升段位积分</text>
        </view>
      </view>
      <text class="text-primary text-sm">去挑战 →</text>
    </view>

    <!-- 我的排名 -->
    <view class="my-rank-card card">
      <view class="my-rank-left">
        <text class="rank-num">#{{ myRankData.rank }}</text>
        <view class="my-avatar-sm">
          <text>{{ myRankData.avatar }}</text>
        </view>
        <view>
          <text class="text-md text-bold">我</text>
          <text class="text-xs text-light">Lv.{{ myRankData.level }} · {{ myRankData.city }}</text>
        </view>
      </view>
      <view class="my-rank-right">
        <text class="text-md text-bold text-primary">{{ myRankData.score }} 分</text>
        <view class="star-row">
          <text v-for="s in myRankData.stars" :key="s">⭐</text>
        </view>
      </view>
    </view>

    <!-- 排名列表 -->
    <view class="rank-list card">
      <view v-for="(r, i) in rankList" :key="r.id || i" class="rank-item" :class="{ me: r.isMe }">
        <text class="rank-pos" :class="{ 'top-three': r.rank <= 3 }">{{ r.rank }}</text>
        <view class="rank-avatar-sm">
          <text>{{ r.avatar }}</text>
        </view>
        <view class="rank-user-info">
          <text class="text-sm text-bold">{{ r.name }}</text>
          <text class="text-xs text-light">Lv.{{ r.level }} · {{ r.city }}</text>
        </view>
        <text class="rank-score text-sm text-bold">{{ r.score }} 分</text>
        <view class="star-row">
          <text v-for="s in r.stars" :key="s">⭐</text>
        </view>
      </view>
      <view v-if="podiumData.length === 0 && rankList.length === 0" class="rank-empty">
        <text class="text-sm text-light">暂无排行数据</text>
      </view>
    </view>
    </template>
  </view>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { getRanking } from '@/api/ranking'
import { normalizeRankingList } from '@/utils/challengeData.mjs'
import FunLoadingState from '@/components/common/FunLoadingState.vue'

const loading = ref(true)
const activeTab = ref(0)
const tabItems = ref([
  { label: '周榜', type: 'weekly' },
  { label: '总榜', type: 'total' },
  { label: '挑战榜', type: 'challenge' }
])

const podiumData = ref([])

const myRankData = ref({ rank: '-', name: '我', avatar: '👦', level: 0, city: '', score: 0, stars: 0 })

const rankList = ref([])

function applyMockData() {
  podiumData.value = []
  rankList.value = []
  myRankData.value = { rank: '-', name: '我', avatar: '👦', level: 0, city: '', score: 0, stars: 0 }
}

function goChallenge() {
  uni.navigateTo({ url: '/pages/challenge/index' })
}

async function loadData() {
  loading.value = true
  try {
    const rankType = tabItems.value[activeTab.value]?.type || 'weekly'
    const result = await getRanking(rankType)

    if (result && Array.isArray(result) && result.length > 0) {
      const normalized = normalizeRankingList(result)
      podiumData.value = normalized.podium.map((r, i) => ({
        rank: r.rank,
        name: r.name,
        avatar: r.avatar || ['🦁', '🐰', '🦊'][i],
        score: r.score
      }))
      rankList.value = normalized.list
      myRankData.value = normalized.me
    }
  } catch (e) {
    console.log('RankingContent: 使用模拟数据', e)
    applyMockData()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})

watch(activeTab, () => {
  loadData()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.ranking-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
}

/* 领奖台 */
.podium-section {
  background: linear-gradient(135deg, $primary, $primary-light);
  border-radius: $radius-md;
  padding: 24px 32px;
}

.podium-row {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 24px;
}

.podium-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;

  &.first { margin-bottom: 12px; }
}

.crown { font-size: 28px; margin-bottom: -4px; }

.podium-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  &.gold-bg { background: linear-gradient(135deg, #FFD700, #FFA500); }
  &.silver-bg { background: linear-gradient(135deg, #C0C0C0, #A8A8A8); }
  &.bronze-bg { background: linear-gradient(135deg, #CD7F32, #B87333); }
}

.podium-emoji { font-size: 28px; }

.pedestal {
  width: 80px;
  border-radius: $radius $radius 0 0;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 4px;
  &.gold-ped { height: 60px; background: #FFD700; }
  &.silver-ped { height: 44px; background: #C0C0C0; }
  &.bronze-ped { height: 32px; background: #CD7F32; }
}

.ped-rank {
  font-size: 24px;
  font-weight: bold;
  color: #fff;
}

/* 我的排名 */
.my-rank-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border: 2px solid $primary;
}

.challenge-entry {
  padding: 14px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  cursor: pointer;
}

.challenge-entry-copy {
  display: flex;
  align-items: center;
  gap: 12px;
}

.challenge-entry-icon { font-size: 32px; }

.my-rank-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rank-num {
  font-size: 20px;
  font-weight: bold;
  color: $primary;
}

.my-avatar-sm {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #F0F0F0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.my-rank-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.star-row { display: flex; gap: 2px; font-size: 12px; }

/* 排名列表 */
.rank-list {
  padding: 12px 16px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 4px;
  border-bottom: 1px solid #F5F5F5;

  &:last-child { border-bottom: none; }
  &.me { background: #FFF8E0; border-radius: $radius; margin: 0 -8px; padding: 10px 12px; }
}

.rank-pos {
  width: 24px;
  font-size: 14px;
  font-weight: 600;
  text-align: center;
  color: $text-light;
  &.top-three { color: $primary; }
}

.rank-avatar-sm {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #F0F0F0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.rank-user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.rank-score {
  margin-right: 8px;
}

.rank-empty {
  padding: 18px 0;
  text-align: center;
}
</style>
