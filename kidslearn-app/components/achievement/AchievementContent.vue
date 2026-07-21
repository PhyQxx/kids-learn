<template>
  <view class="achievement-content">
    <FunLoadingState v-if="loading" title="正在开启宝箱" mascot="" />

    <template v-else>
      <view class="summary-card">
        <image class="achievement-hero-art" src="/static/redesign/achievement-cabinet.png" mode="aspectFill" />
        <view class="summary-info">
          <view>
            <text class="achievement-kicker">成长收藏馆</text>
            <text class="achievement-hero-title">成就中心</text>
            <text class="achievement-hero-desc">已解锁 {{ completedCount }}/{{ totalCount }} 个成就</text>
          </view>
        </view>
        <view class="summary-right">
          <text class="current-title-label">当前称号</text>
          <view class="title-badge">
            <text>{{ currentTitle }}</text>
          </view>
        </view>
      </view>

      <view class="action-row">
        <tn-button
          type="primary"
          size="sm"
          shape="round"
          :disabled="claimingAll || claimableCount === 0"
          :loading="claimingAll"
          @click="claimAllRewards"
        >{{ claimAllLabel }}</tn-button>
      </view>

      <tn-tabs v-model="activeTab" active-color="#FF6B6B">
        <tn-tabs-item v-for="tab in tabItems" :key="tab.label" :title="tab.label" />
      </tn-tabs>

      <view class="achieve-grid stagger-spring">
        <view
          v-for="ach in achievements"
          :key="ach.id"
          class="achieve-card card"
          :class="ach.status"
        >
          <view class="achieve-icon-wrap" :class="ach.rarity">
            <text class="achieve-index">{{ String(achievements.indexOf(ach) + 1).padStart(2, '0') }}</text>
          </view>
          <view class="achieve-info">
            <text class="text-sm text-bold">{{ ach.name }}</text>
            <text class="text-xs text-light">{{ ach.desc }}</text>
            <view v-if="ach.status === 'progress'" class="achieve-progress">
              <tn-line-progress :percent="ach.percent" :height="10" :show-percent="false" style="flex: 1;" />
              <text class="text-xs text-light">{{ ach.current }}/{{ ach.target }}</text>
            </view>
            <view v-if="ach.status === 'done'" class="done-badge">
              <text class="text-xs text-success">已达成</text>
            </view>
            <view v-if="ach.status === 'locked'" class="locked-badge">
              <text class="text-xs text-light">尚未解锁</text>
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
import { computed, ref, watch, onMounted } from 'vue'
import { getAchievements, getMyProgress, receiveReward } from '@/api/achievement'
import { claimAchievementReward, claimAllAchievementRewards } from '@/utils/achievementClaim.mjs'
import FunLoadingState from '@/components/common/FunLoadingState.vue'

const loading = ref(true)
const claimingAll = ref(false)
const activeTab = ref(0)
const tabItems = ref([
  { label: '全部' },
  { label: '学习' },
  { label: '收集' },
  { label: '社交' }
])

const completedCount = ref(0)
const totalCount = ref(0)
const currentTitle = ref('暂无称号')

const achievements = ref([])

const claimableCount = computed(() =>
  achievements.value.filter(ach => ach.status === 'done' && !ach.claimed).length
)
const claimAllLabel = computed(() => {
  if (claimingAll.value) return '领取中...'
  return claimableCount.value > 0 ? `一键领取（${claimableCount.value}）` : '一键领取'
})

async function loadData() {
  loading.value = true
  const typeMap = [null, 1, 2, 3]
  try {
    const results = await Promise.allSettled([
      getAchievements(typeMap[activeTab.value]),
      getMyProgress()
    ])

    if (results[0].status === 'fulfilled' && results[0].value) {
      const list = results[0].value
      if (Array.isArray(list) && list.length > 0) {
        achievements.value = list.map(a => {
          const targetValue = getAchievementTarget(a)
          const currentValue = Number(a.currentValue || 0)
          return {
            id: a.id,
            name: a.achieveName,
            desc: a.achieveDesc,
            icon: a.achieveIcon || '🏅',
            achieveType: a.achieveType,
            isTiered: a.isTiered,
            tiers: a.tiers || [],
            status: a.isCompleted ? 'done' : (currentValue > 0 ? 'progress' : 'locked'),
            rarity: ['bronze', 'silver', 'gold', 'legendary'][(a.achieveType || 1) - 1] || 'bronze',
            claimed: a.isReceived || false,
            percent: targetValue > 0 ? Math.min(100, Math.round(currentValue / targetValue * 100)) : 0,
            current: currentValue,
            target: targetValue
          }
        })
        totalCount.value = list.length
        completedCount.value = list.filter(a => a.isCompleted).length
      }
    }

    if (results[1].status === 'fulfilled' && results[1].value) {
      const prog = results[1].value
      completedCount.value = prog.completedAchievements || completedCount.value
      totalCount.value = prog.totalAchievements || totalCount.value
    }
  } catch (e) {
    console.log('AchievementContent: 成就加载失败', e)
    completedCount.value = 0
    totalCount.value = 0
    currentTitle.value = '暂无称号'
    achievements.value = []
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

function getAchievementTarget(a) {
  const directTarget = Number(a.targetValue || 0)
  if (directTarget > 0) return directTarget

  const firstTier = Array.isArray(a.tiers) ? a.tiers[0] : null
  if (firstTier && firstTier.conditionJson) {
    try {
      const condition = typeof firstTier.conditionJson === 'string'
        ? JSON.parse(firstTier.conditionJson)
        : firstTier.conditionJson
      const tierTarget = Number(
        condition.target ||
        condition.targetValue ||
        condition.count ||
        condition.value ||
        condition.levelCount ||
        condition.starCount ||
        condition.stickerCount ||
        condition.subjectCount ||
        condition.days ||
        0
      )
      if (tierTarget > 0) return tierTarget
    } catch (e) {
      console.log('AchievementContent: conditionJson parse failed', e)
    }
  }

  return 1
}

function claimReward(ach) {
  claimAchievementReward(receiveReward, ach).catch(() => {})
}

async function claimAllRewards() {
  if (claimingAll.value) return
  claimingAll.value = true
  try {
    await claimAllAchievementRewards(receiveReward, achievements.value)
  } finally {
    claimingAll.value = false
  }
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

.action-row {
  display: flex;
  justify-content: flex-end;
}

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

/* Planet achievement gallery */
.summary-card {
  position: relative;
  min-height: 224px;
  padding: 0;
  overflow: hidden;
  border-radius: 28px;
  background: #FFF7EA;
  border: 1px solid rgba(229,154,18,.14);
  box-shadow: 0 12px 32px rgba(69,91,124,.10);
}
.achievement-hero-art { position: absolute; inset: 0; width: 100%; height: 100%; }
.summary-card::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(255,255,255,.98) 0%, rgba(255,255,255,.80) 36%, rgba(255,255,255,.04) 72%);
}
.summary-info {
  position: absolute;
  z-index: 2;
  left: 28px;
  top: 28px;
}
.achievement-kicker { display: block; color: #E59A12; font-size: 13px; font-weight: 850; }
.achievement-hero-title { display: block; margin-top: 6px; color: #18212F; font-size: 28px; font-weight: 850; }
.achievement-hero-desc { display: block; margin-top: 7px; color: #5D6A7A; font-size: 14px; }
.summary-right {
  position: absolute;
  z-index: 2;
  left: 28px;
  bottom: 22px;
  align-items: flex-start;
}
.current-title-label { color: #7A8797; font-size: 11px; font-weight: 750; }
.title-badge { min-height: 34px; padding: 0 14px; background: #FFF1CE; color: #9A6507; display: flex; align-items: center; font-size: 13px; font-weight: 850; }
.action-row { margin-top: -60px; margin-right: 20px; z-index: 3; min-height: 44px; align-items: center; }
.achieve-grid { gap: 14px; }
.achieve-card {
  min-height: 96px;
  padding: 16px 18px;
  border: 1px solid rgba(63,111,229,.10);
  border-left-width: 4px;
  border-radius: 20px;
  background: #FFFFFF;
  box-shadow: 0 8px 22px rgba(69,91,124,.07);
}
.achieve-icon-wrap { width: 48px; height: 48px; border-radius: 16px; box-shadow: inset 0 0 0 3px rgba(255,255,255,.46); }
.achieve-index { color: #FFFFFF; font-size: 13px; font-weight: 900; letter-spacing: .5px; }

@media (min-width: 1200px) and (min-height: 900px) {
  .summary-card { min-height: 270px; }
  .achieve-grid { grid-template-columns: repeat(3, minmax(0,1fr)); }
  .achieve-card { min-height: 108px; }
}
</style>
