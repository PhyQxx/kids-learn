<template>
  <AppLayout title="排行榜" active-nav="/pages/ranking/index">
    <template #topbar-right>
      <view class="week-tag badge badge-primary"><text class="text-xs">本周</text></view>
    </template>

    <view class="ranking-content">
      <!-- 领奖台 -->
      <view class="podium-section">
        <view class="podium-item second" :class="{ mine: rankers[1]?.isMe }">
          <text class="podium-emoji">👦</text>
          <text class="podium-name text-sm text-bold">{{ rankers[1]?.name }}</text>
          <view class="podium-base silver"><text class="podium-rank">2</text></view>
        </view>
        <view class="podium-item first" :class="{ mine: rankers[0]?.isMe }">
          <text class="podium-emoji" style="font-size: 48px;">🧒</text>
          <text class="podium-name text-sm text-bold">{{ rankers[0]?.name }}</text>
          <view class="podium-base gold"><text class="podium-rank">1</text></view>
        </view>
        <view class="podium-item third" :class="{ mine: rankers[2]?.isMe }">
          <text class="podium-emoji">👧</text>
          <text class="podium-name text-sm text-bold">{{ rankers[2]?.name }}</text>
          <view class="podium-base bronze"><text class="podium-rank">3</text></view>
        </view>
      </view>

      <!-- Tab切换 -->
      <view class="tab-nav">
        <view v-for="tab in tabs" :key="tab.value" class="tab-item" :class="{ active: activeTab === tab.value }" @tap="activeTab = tab.value">
          <text class="text-sm">{{ tab.label }}</text>
        </view>
      </view>

      <!-- 我的排名 -->
      <view class="my-rank card">
        <text class="rank-medal text-lg text-bold" style="color: #B8860B;">12</text>
        <text class="rank-avatar">👦</text>
        <view class="rank-user-info">
          <text class="text-sm text-bold">{{ myInfo.name }}</text>
          <text class="text-xs text-light">Lv.{{ myInfo.level }}</text>
        </view>
        <text class="rank-score-text text-md text-bold text-primary">{{ myInfo.score }}</text>
        <text class="text-xs text-light">分</text>
        <view class="stars">
          <text v-for="s in 3" :key="s" :class="s <= myInfo.stars ? 'star-filled' : 'star-empty'">⭐</text>
        </view>
      </view>

      <!-- 排名列表 -->
      <view class="rank-list">
        <view v-for="(r, i) in rankList" :key="r.id" class="rank-row card" :class="{ mine: r.isMe }">
          <text class="rank-medal">{{ i < 3 ? ['🥇','🥈','🥉'][i] : (i + 1) }}</text>
          <text class="rank-avatar-small">{{ r.avatar }}</text>
          <view class="rank-user-info">
            <text class="text-sm text-bold">{{ r.name }}</text>
            <text class="text-xs text-light">Lv.{{ r.level }}</text>
          </view>
          <text class="rank-score-text text-sm text-bold">{{ r.score }} 分</text>
        </view>
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { getRanking } from '@/api/ranking'

const activeTab = ref('friends')
const tabs = [
  { label: '好友', value: 'friends' },
  { label: '班级', value: 'class' },
  { label: '全国', value: 'national' }
]

const myInfo = ref({ name: '我', level: 5, score: 1250, stars: 2 })

const rankers = ref([
  { name: '小明', score: 2580, isMe: false },
  { name: '小红', score: 2340, isMe: false },
  { name: '小华', score: 2100, isMe: false }
])

const rankList = ref([
  { id: 4, name: '小丽', avatar: '👧', level: 8, score: 1890, isMe: false },
  { id: 5, name: '小刚', avatar: '👦', level: 7, score: 1650, isMe: false },
  { id: 6, name: '小美', avatar: '👧', level: 6, score: 1480, isMe: false },
  { id: 7, name: '我', avatar: '👦', level: 5, score: 1250, isMe: true },
  { id: 8, name: '小龙', avatar: '👦', level: 4, score: 980, isMe: false }
])

async function loadData() {
  try {
    const res = await getRanking('weekly')
    if (res && Array.isArray(res) && res.length > 0) {
      const top3 = res.slice(0, 3)
      rankers.value = top3.map(r => ({
        name: r.nickname || r.name,
        score: r.score || 0,
        isMe: r.isMe || false
      }))
      rankList.value = res.slice(3).map(r => ({
        id: r.id,
        name: r.nickname || r.name,
        avatar: r.avatar || '👦',
        level: r.level || 1,
        score: r.score || 0,
        isMe: r.isMe || false
      }))
      const me = res.find(r => r.isMe)
      if (me) {
        myInfo.value = {
          name: '我',
          level: me.level || 5,
          score: me.score || 0,
          stars: Math.min(Math.floor((me.score || 0) / 500), 5)
        }
      }
    }
  } catch (e) {
    console.log('ranking/index: 使用模拟数据')
  }
}

onMounted(() => loadData())
watch(activeTab, () => loadData())
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.ranking-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 领奖台 */
.podium-section {
  background: linear-gradient(135deg, $primary, $primary-light);
  border-radius: $radius-lg $radius-lg 0 0;
  padding: 24px 40px 0;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 20px;
}

.podium-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;

  &.mine .podium-base { box-shadow: 0 0 0 3px $accent; }
}

.podium-emoji { font-size: 36px; margin-bottom: 4px; }
.podium-name { color: $white; margin-bottom: 8px; }

.podium-base {
  width: 80px;
  border-radius: 12px 12px 0 0;
  padding: 8px;
  text-align: center;

  &.gold { height: 60px; background: linear-gradient(180deg, #FFD700, #FFA500); }
  &.silver { height: 45px; background: linear-gradient(180deg, #E8E8E8, #C0C0C0); }
  &.bronze { height: 35px; background: linear-gradient(180deg, #FFDAB9, #CD7F32); }
}

.podium-rank { font-size: 20px; font-weight: bold; color: $white; }

/* Tabs */
.tab-nav {
  display: flex;
  gap: 4px;
  border-bottom: 2px solid #F0F0F0;
}
.tab-item {
  padding: 8px 20px;
  cursor: pointer;
  position: relative;
  &.active {
    &::after { content: ''; position: absolute; bottom: -2px; left: 0; right: 0; height: 3px; background: $primary; border-radius: 2px; }
    text { color: $primary; font-weight: 600; }
  }
}

/* 我的排名 */
.my-rank {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border: 2px solid $accent;
}

.rank-avatar { font-size: 32px; }
.rank-avatar-small { font-size: 24px; }
.rank-user-info { flex: 1; display: flex; flex-direction: column; }
.rank-score-text { }
.stars { display: flex; gap: 2px; font-size: 12px; }
.star-filled { color: $gold; }
.star-empty { color: #E0E0E0; }

/* 排名列表 */
.rank-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.rank-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;

  &.mine { border: 2px solid $accent; }
}

.rank-medal { width: 28px; text-align: center; font-size: 18px; }
</style>
