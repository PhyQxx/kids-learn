<template>
  <view class="achievement-content">
    <!-- Loading -->
    <view v-if="loading" class="loading-state">
      <tn-loading size="60" />
      <text class="text-sm text-light" style="margin-top: 12px;">加载中...</text>
    </view>

    <template v-else>
    <!-- 成就概览 -->
    <view class="summary-card">
      <view class="summary-info">
        <text class="summary-emoji">🏅</text>
        <view>
          <text class="text-lg text-bold text-white">成就中心</text>
          <text class="text-sm text-white" style="opacity: 0.8;">已解锁 {{ completedCount }}/{{ totalCount }} 个成就</text>
        </view>
      </view>
      <view class="summary-right">
        <text class="text-sm text-white" style="opacity: 0.8;">当前称号</text>
        <view class="title-badge">
          <text class="text-sm text-bold text-white">{{ currentTitle }}</text>
        </view>
      </view>
    </view>

    <!-- 分类筛选 -->
    <tn-tabs v-model="activeTab" active-color="#FF6B6B">
      <tn-tabs-item v-for="tab in tabItems" :key="tab.label" :title="tab.label" />
    </tn-tabs>

    <!-- 成就网格 -->
    <view class="achieve-grid stagger-list">
      <view
        v-for="ach in achievements"
        :key="ach.id"
        class="achieve-card card"
        :class="ach.status"
      >
        <view class="achieve-icon-wrap" :class="ach.rarity">
          <text class="achieve-emoji">{{ ach.icon }}</text>
        </view>
        <view class="achieve-info">
          <text class="text-sm text-bold">{{ ach.name }}</text>
          <text class="text-xs text-light">{{ ach.desc }}</text>
          <!-- 进度 -->
          <view v-if="ach.status === 'progress'" class="achieve-progress">
            <tn-line-progress :percent="ach.percent" :height="6" :show-percent="false" style="flex: 1;" />
            <text class="text-xs text-light">{{ ach.current }}/{{ ach.target }}</text>
          </view>
          <!-- 已完成 -->
          <view v-if="ach.status === 'done'" class="done-badge">
            <text class="text-xs text-success">✅ 已达成</text>
          </view>
          <!-- 锁定 -->
          <view v-if="ach.status === 'locked'" class="locked-badge">
            <text class="text-xs text-light">🔒 未解锁</text>
          </view>
        </view>
        <tn-button
          v-if="ach.status === 'done' && !ach.claimed"
          type="primary"
          size="sm"
          shape="round"
          @click="claimReward(ach)"
        >领取</tn-button>
      </view>
    </view>
    </template>
  </view>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { getAchievements, getMyProgress, receiveReward } from '@/api/achievement'

const loading = ref(true)
const activeTab = ref(0)
const tabItems = ref([
  { label: '全部' },
  { label: '学习' },
  { label: '收集' },
  { label: '社交' }
])

const completedCount = ref(0)
const totalCount = ref(36)
const currentTitle = ref('学习小将')

const achievements = ref([
  { id: 1, name: '初次通关', desc: '完成第一个关卡', icon: '🌟', status: 'done', rarity: 'bronze', claimed: true },
  { id: 2, name: '满星达人', desc: '获得10个三星评价', icon: '⭐', status: 'done', rarity: 'silver', claimed: false },
  { id: 3, name: '语文小能手', desc: '完成所有语文课程', icon: '📖', status: 'done', rarity: 'gold', claimed: false },
  { id: 4, name: '7日连续打卡', desc: '连续学习7天', icon: '🔥', status: 'progress', rarity: 'bronze', percent: 71, current: 5, target: 7 },
  { id: 5, name: '数学天才', desc: '完成50道数学题', icon: '🔢', status: 'progress', rarity: 'silver', percent: 50, current: 25, target: 50 },
  { id: 6, name: '全科达人', desc: '学习6门学科', icon: '🎓', status: 'progress', rarity: 'gold', percent: 67, current: 4, target: 6 },
  { id: 7, name: '贴纸收藏家', desc: '收集100张贴纸', icon: '🎭', status: 'locked', rarity: 'legendary' },
  { id: 8, name: '全国冠军', desc: '全国排行榜第一', icon: '🏆', status: 'locked', rarity: 'legendary' }
])

function applyMockData() {
  completedCount.value = 12
  totalCount.value = 36
  currentTitle.value = '学习小将'
}

async function loadData() {
  loading.value = true
  // achieveType 字典: 1=学习, 2=收集, 3=社交
  const typeMap = [null, 1, 2, 3]
  try {
    const results = await Promise.allSettled([
      getAchievements(typeMap[activeTab.value]),
      getMyProgress()
    ])

    // 成就列表
    if (results[0].status === 'fulfilled' && results[0].value) {
      const list = results[0].value
      if (Array.isArray(list) && list.length > 0) {
        achievements.value = list.map(a => ({
          id: a.id,
          name: a.achieveName,
          desc: a.achieveDesc,
          icon: a.achieveIcon || '🏅',
          achieveType: a.achieveType,
          isTiered: a.isTiered,
          tiers: a.tiers || [],
          status: a.isCompleted ? 'done' : (a.currentValue > 0 ? 'progress' : 'locked'),
          rarity: ['bronze', 'silver', 'gold', 'legendary'][(a.achieveType || 1) - 1] || 'bronze',
          claimed: a.isReceived || false,
          percent: a.currentValue ? Math.round(a.currentValue / 100 * 100) : 0,
          current: a.currentValue || 0,
          target: 100
        }))
        totalCount.value = list.length
        completedCount.value = list.filter(a => a.isCompleted).length
      }
    }

    // 进度概览
    if (results[1].status === 'fulfilled' && results[1].value) {
      const prog = results[1].value
      completedCount.value = prog.completedAchievements || completedCount.value
      totalCount.value = prog.totalAchievements || totalCount.value
    }
  } catch (e) {
    console.log('AchievementContent: 使用模拟数据', e)
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

function claimReward(ach) {
  receiveReward(ach.id).then(() => {
    ach.claimed = true
    uni.showToast({ title: '奖励已领取！', icon: 'success' })
  }).catch(() => {
    ach.claimed = true
    uni.showToast({ title: '奖励已领取！', icon: 'success' })
  })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.achievement-content {
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

/* 概览 */
.summary-card {
  background: linear-gradient(135deg, #7B68EE, #9B8BFF);
  border-radius: $radius-md;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 4px 20px rgba(123, 104, 238, 0.3);
}

.summary-info {
  display: flex;
  align-items: center;
  gap: 14px;
}

.summary-emoji { font-size: 44px; }

.summary-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.title-badge {
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 16px;
  border-radius: 100px;
}

/* 成就网格 */
.achieve-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.achieve-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border-left: 4px solid #E0E0E0;

  &.done { border-left-color: $success; }
  &.progress { border-left-color: $primary; }
}

.achieve-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.bronze { background: linear-gradient(135deg, #CD7F32, #E8A862); }
  &.silver { background: linear-gradient(135deg, #C0C0C0, #D8D8D8); }
  &.gold { background: linear-gradient(135deg, #FFD700, #FFA500); }
  &.legendary { background: linear-gradient(135deg, #9B59B6, #8E44AD); animation: glow 2s infinite; }
}

@keyframes glow {
  0%, 100% { box-shadow: 0 0 8px rgba(155, 89, 182, 0.4); }
  50% { box-shadow: 0 0 20px rgba(155, 89, 182, 0.8); }
}

.achieve-emoji { font-size: 24px; }

.achieve-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.achieve-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.done-badge, .locked-badge {
  margin-top: 2px;
}
</style>
