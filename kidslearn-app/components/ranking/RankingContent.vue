<template>
  <view class="ranking-content">
    <!-- Loading -->
    <view v-if="loading" class="loading-state">
      <tn-loading size="60" />
      <text class="text-sm text-light" style="margin-top: 12px;">加载中...</text>
    </view>

    <template v-else>
    <!-- 领奖台 -->
    <view class="podium-section">
      <view class="podium-row">
        <!-- 第2名 -->
        <view class="podium-item">
          <view class="podium-avatar silver-bg">
            <text class="podium-emoji">{{ podiumData[1]?.avatar || '🐰' }}</text>
          </view>
          <text class="text-sm text-bold">{{ podiumData[1]?.name || '小红' }}</text>
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
          <text class="text-sm text-bold">{{ podiumData[0]?.name || '小明' }}</text>
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
          <text class="text-sm text-bold">{{ podiumData[2]?.name || '小华' }}</text>
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
      <view v-for="(r, i) in rankList" :key="r.id" class="rank-item" :class="{ me: r.isMe }">
        <text class="rank-pos" :class="{ 'top-three': i < 3 }">{{ i + 4 }}</text>
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
    </view>
    </template>
  </view>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { getRanking, getMyRank } from '@/api/ranking'

const loading = ref(true)
const activeTab = ref(0)
const tabItems = ref([
  { label: '好友' },
  { label: '班级' },
  { label: '全国' }
])

const podiumData = ref([
  { rank: 1, name: '小明', avatar: '🦁', score: 4520 },
  { rank: 2, name: '小红', avatar: '🐰', score: 4120 },
  { rank: 3, name: '小华', avatar: '🦊', score: 3800 }
])

const myRankData = ref({ rank: 5, name: '我', avatar: '👦', level: 5, city: '北京', score: 2150, stars: 3 })

const rankList = ref([
  { id: 1, name: '小丽', avatar: '👧', level: 6, city: '上海', score: 3200, stars: 4, isMe: false },
  { id: 2, name: '小明', avatar: '👦', level: 5, city: '北京', score: 2150, stars: 3, isMe: true },
  { id: 3, name: '小龙', avatar: '🧒', level: 5, city: '深圳', score: 1980, stars: 3, isMe: false },
  { id: 4, name: '小雨', avatar: '👧', level: 4, city: '广州', score: 1650, stars: 2, isMe: false }
])

function applyMockData() {
  podiumData.value = [
    { rank: 1, name: '小明', avatar: '🦁', score: 4520 },
    { rank: 2, name: '小红', avatar: '🐰', score: 4120 },
    { rank: 3, name: '小华', avatar: '🦊', score: 3800 }
  ]
  myRankData.value = { rank: 5, name: '我', avatar: '👦', level: 5, city: '北京', score: 2150, stars: 3 }
  rankList.value = [
    { id: 1, name: '小丽', avatar: '👧', level: 6, city: '上海', score: 3200, stars: 4, isMe: false },
    { id: 2, name: '小明', avatar: '👦', level: 5, city: '北京', score: 2150, stars: 3, isMe: true },
    { id: 3, name: '小龙', avatar: '🧒', level: 5, city: '深圳', score: 1980, stars: 3, isMe: false },
    { id: 4, name: '小雨', avatar: '👧', level: 4, city: '广州', score: 1650, stars: 2, isMe: false }
  ]
}

async function loadData() {
  loading.value = true
  try {
    const result = await getRanking('weekly')

    if (result && Array.isArray(result) && result.length > 0) {
      const top3 = result.slice(0, 3)
      podiumData.value = top3.map((r, i) => ({
        rank: i + 1,
        name: r.nickname || r.name,
        avatar: r.avatar || ['🦁', '🐰', '🦊'][i],
        score: r.score || 0
      }))
      rankList.value = result.slice(3).map(r => ({
        id: r.id,
        name: r.nickname || r.name,
        avatar: r.avatar || '👦',
        level: r.level || 1,
        city: r.city || '',
        score: r.score || 0,
        stars: Math.min(Math.floor((r.score || 0) / 500), 5),
        isMe: r.isMe || false
      }))

      // 从列表中找到"我"的排名
      const me = result.find(r => r.isMe)
      if (me) {
        const rank = result.indexOf(me) + 1
        myRankData.value = {
          rank,
          name: '我',
          avatar: me.avatar || '👦',
          level: me.level || 5,
          city: me.city || '',
          score: me.score || 0,
          stars: Math.min(Math.floor((me.score || 0) / 500), 5)
        }
      }
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
</style>
