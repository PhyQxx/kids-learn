<template>
  <view class="home-content">
    <!-- Loading -->
    <view v-if="loading" class="loading-state">
      <tn-loading size="60" />
      <text class="text-sm text-light" style="margin-top: 12px;">加载中...</text>
    </view>

    <template v-else>
      <!-- 任务横幅 -->
      <view class="task-banner" @tap="$emit('go-learn')">
        <view class="banner-info">
          <text class="banner-emoji">📋</text>
          <view class="banner-text">
            <text class="banner-title">今日学习任务</text>
            <text class="banner-desc">完成 {{ completedTasks }}/{{ totalTasks }} 个任务</text>
          </view>
        </view>
        <view class="banner-progress">
          <tn-line-progress :percent="taskProgress" active-color="#FFE66D" inactive-color="rgba(255,255,255,0.3)" :height="10" :show-percent="false" />
        </view>
      </view>

      <!-- 排名横幅 -->
      <view class="rank-banner" @tap="goRanking">
        <view class="rank-info">
          <text class="rank-emoji">🏆</text>
          <view class="rank-text">
            <text class="rank-title">本周排行</text>
            <text class="rank-desc">第 {{ myRank }} 名 · 继续加油！</text>
          </view>
        </view>
        <tn-button size="sm" shape="round" style="background: rgba(255,255,255,0.3); color: #5D4E60;">查看排行</tn-button>
      </view>

      <!-- 学科网格 -->
      <view class="section-title-row">
        <text class="text-lg text-bold">📚 今日学习</text>
        <text class="text-primary text-sm" @tap="$emit('go-learn')">查看全部 →</text>
      </view>
      <view class="subject-grid stagger-list">
        <view
          v-for="subject in subjects"
          :key="subject.id"
          class="subject-card card card-hover"
          :class="{ locked: subject.locked }"
          @tap="!subject.locked && $emit('go-subject', subject)"
        >
          <text class="subject-emoji">{{ subject.icon }}</text>
          <text class="subject-name">{{ subject.name }}</text>
          <text class="subject-progress-text text-xs">{{ subject.progress }}%</text>
        </view>
      </view>

      <!-- 快捷入口 -->
      <view class="quick-grid">
        <view class="quick-card card card-hover" @tap="goPet">
          <view class="quick-pet-info">
            <text class="pet-emoji animate-float">🐱</text>
            <view>
              <text class="pet-name">{{ petStore.name }}</text>
              <text class="pet-status-text text-xs text-light">{{ petStore.moodText }} · Lv.{{ petStore.level }}</text>
            </view>
          </view>
          <text class="text-primary text-sm">去看看 →</text>
        </view>
        <view class="quick-card card achievement-quick" @tap="goAchievement">
          <text class="ach-emoji">🏅</text>
          <text class="text-white text-md text-bold">成就中心</text>
          <text class="text-white text-xs" style="opacity: 0.8;">{{ achievementCount }} 个成就</text>
        </view>
      </view>

      <!-- 排行速览 -->
      <view class="ranking-quick card">
        <view class="ranking-header">
          <text class="text-md text-bold">🏆 排行榜</text>
          <text class="text-primary text-sm" @tap="goRanking">更多 →</text>
        </view>
        <view class="ranking-list">
          <view v-for="(r, i) in topRankers" :key="i" class="ranking-item">
            <text class="rank-medal">{{ ['🥇','🥈','🥉'][i] }}</text>
            <text class="rank-name text-sm">{{ r.name }}</text>
            <text class="rank-score text-xs text-light">{{ r.score }} 分</text>
          </view>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup>
import { ref, computed, inject, onMounted } from 'vue'
import { usePetStore } from '@/store/pet'
import { useLearnStore } from '@/store/learn'
import { getDailyTasks, getSubjects, getRecords } from '@/api/learn'
import { getPetStatus } from '@/api/pet'
import { getMyRank, getRanking } from '@/api/ranking'
import { getMyProgress } from '@/api/achievement'

defineEmits(['go-subject', 'go-learn'])

const switchTab = inject('switchTab', () => {})
const petStore = usePetStore()
const learnStore = useLearnStore()

const loading = ref(true)
const completedTasks = ref(0)
const totalTasks = ref(5)
const myRank = ref(12)
const achievementCount = ref(0)

const taskProgress = computed(() =>
  totalTasks.value ? Math.round(completedTasks.value / totalTasks.value * 100) : 0
)

const subjects = ref([
  { id: 1, name: '语文', icon: '📖', progress: 0, locked: false },
  { id: 2, name: '数学', icon: '🔢', progress: 0, locked: false },
  { id: 3, name: '英语', icon: '🔤', progress: 0, locked: false },
  { id: 4, name: '逻辑', icon: '🧩', progress: 0, locked: false },
  { id: 5, name: '科学', icon: '🔬', progress: 0, locked: false },
  { id: 6, name: '音乐', icon: '🎵', progress: 0, locked: true }
])

const topRankers = ref([
  { name: '小明', score: 2580 },
  { name: '小红', score: 2340 },
  { name: '小华', score: 2100 }
])

// Mock 数据 fallback
function applyMockData() {
  completedTasks.value = 2
  totalTasks.value = 5
  myRank.value = 12
  achievementCount.value = 32
  subjects.value = [
    { id: 1, name: '语文', icon: '📖', progress: 75, locked: false },
    { id: 2, name: '数学', icon: '🔢', progress: 60, locked: false },
    { id: 3, name: '英语', icon: '🔤', progress: 45, locked: false },
    { id: 4, name: '逻辑', icon: '🧩', progress: 30, locked: false },
    { id: 5, name: '科学', icon: '🔬', progress: 20, locked: false },
    { id: 6, name: '音乐', icon: '🎵', progress: 0, locked: true }
  ]
}

async function loadData() {
  loading.value = true
  try {
    const results = await Promise.allSettled([
      getDailyTasks(),
      getSubjects(),
      getPetStatus(),
      getMyRank('weekly').catch(() => null),
      getMyProgress().catch(() => null)
    ])

    // 今日任务
    if (results[0].status === 'fulfilled' && results[0].value) {
      const tasks = results[0].value.tasks || results[0].value || []
      learnStore.setDailyTasks(tasks)
      completedTasks.value = tasks.filter(t => t.status === 'COMPLETED').length
      totalTasks.value = tasks.length || 5
    }

    // 学科列表
    if (results[1].status === 'fulfilled' && results[1].value) {
      const list = results[1].value
      subjects.value = (Array.isArray(list) ? list : []).map(s => ({
        id: s.id,
        name: s.subjectName || s.name,
        icon: s.iconUrl || { 'CHINESE': '📖', 'MATH': '🔢', 'ENGLISH': '🔤', 'LOGIC': '🧩', 'SCIENCE': '🔬', 'MUSIC': '🎵' }[s.subjectCode] || '📚',
        progress: s.progress || 0,
        locked: s.status === 0 || s.isVip
      }))
    }

    // 宠物状态
    if (results[2].status === 'fulfilled' && results[2].value) {
      petStore.setPetInfo(results[2].value)
    }

    // 排名
    if (results[3].status === 'fulfilled' && results[3].value) {
      const list = results[3].value
      if (Array.isArray(list)) {
        const me = list.find(r => r.isMe)
        if (me) {
          myRank.value = list.indexOf(me) + 1
        }
        // 取前三名显示
        if (list.length >= 3) {
          topRankers.value = list.slice(0, 3).map(r => ({
            name: r.nickname || r.name || '未知',
            score: r.score || 0
          }))
        }
      }
    }

    // 成就数
    if (results[4].status === 'fulfilled' && results[4].value) {
      const prog = results[4].value
      achievementCount.value = prog.completedCount || prog.length || 0
    }
  } catch (e) {
    console.log('HomeContent: 使用模拟数据', e)
    applyMockData()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})

function goRanking() { switchTab('ranking') }
function goPet() { switchTab('pet') }
function goAchievement() { switchTab('achievement') }
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.home-content { display: flex; flex-direction: column; gap: 16px; }

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
}

.task-banner {
  background: linear-gradient(135deg, $primary, $primary-light); border-radius: $radius-md;
  padding: 20px 24px; display: flex; align-items: center; justify-content: space-between; cursor: pointer;
}
.banner-info { display: flex; align-items: center; gap: 12px; }
.banner-emoji { font-size: 40px; }
.banner-text { display: flex; flex-direction: column; }
.banner-title { font-size: 18px; font-weight: 600; color: $white; }
.banner-desc { font-size: 13px; color: rgba(255,255,255,0.8); }
.progress-fill-accent { height: 100%; border-radius: 5px; background: $accent; }

.rank-banner {
  background: linear-gradient(135deg, $accent, #FFD700); border-radius: $radius-md;
  padding: 16px 24px; display: flex; align-items: center; justify-content: space-between; cursor: pointer;
}
.rank-info { display: flex; align-items: center; gap: 12px; }
.rank-emoji { font-size: 36px; }
.rank-text { display: flex; flex-direction: column; }
.rank-title { font-size: 16px; font-weight: 600; color: #5D4E60; }
.rank-desc { font-size: 13px; color: #7D6E80; }

.section-title-row { display: flex; align-items: center; justify-content: space-between; margin-top: 4px; }
.subject-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.subject-card {
  padding: 24px 16px; text-align: center; cursor: pointer; transition: all $transition-fast;
  &:active { transform: scale(0.97); }
  &.locked { opacity: 0.5; background: #F5F5F5; box-shadow: none; }
}
.subject-emoji { font-size: 48px; display: block; margin-bottom: 8px; }
.subject-name { font-size: 16px; font-weight: 600; color: $text; display: block; }
.subject-progress-text { font-size: 13px; color: $text-light; display: block; margin-top: 4px; }

.quick-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.quick-card {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 24px; cursor: pointer;
  &:active { transform: scale(0.97); }
}
.quick-pet-info { display: flex; align-items: center; gap: 12px; }
.pet-emoji { font-size: 48px; }
.pet-name { font-size: 16px; font-weight: 600; color: $text; display: block; }
.pet-status-text { display: block; margin-top: 2px; }
.achievement-quick {
  background: linear-gradient(135deg, #7B68EE, #9B8BFF); flex-direction: column;
  align-items: flex-start; gap: 4px; box-shadow: 0 4px 20px rgba(123,104,238,0.3);
}
.ach-emoji { font-size: 32px; display: block; margin-bottom: 4px; }

.ranking-quick { padding: 16px 20px; }
.ranking-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.ranking-list { display: flex; flex-direction: column; gap: 8px; }
.ranking-item { display: flex; align-items: center; gap: 10px; }
.rank-medal { font-size: 20px; width: 28px; text-align: center; }
.rank-name { flex: 1; }

/* 响应式 */
@media (max-width: 800px) {
  .subject-grid { grid-template-columns: repeat(3, 1fr); gap: 8px; }
  .subject-emoji { font-size: 36px; }
  .subject-card { padding: 16px 10px; }
  .quick-grid { grid-template-columns: 1fr; }
  .banner-emoji { font-size: 28px; }
  .rank-emoji { font-size: 28px; }
  .pet-emoji { font-size: 36px; }
  .ranking-quick { display: none; }
}
</style>
