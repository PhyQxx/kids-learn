<template>
  <AppLayout theme="learn" title="关卡列表" :show-back="true" active-nav="/pages/learn/index">
    <template #topbar-left-extra>
      <view class="badge badge-blue" style="margin-left: 8px;">
        <text class="text-xs">{{ courseName }}</text>
      </view>
    </template>

    <view class="levels-content">
      <!-- 单元分组 -->
      <view v-for="unit in units" :key="unit.id" class="unit-section">
        <view class="unit-header">
          <text class="unit-title text-md text-bold">{{ unit.name }}</text>
          <text class="unit-progress text-xs text-light">{{ unit.completed }}/{{ unit.total }} 已完成</text>
        </view>
        <view class="level-grid" :class="{ 'boss-grid': unit.isBoss }">
          <view
            v-for="level in unit.levels"
            :key="level.id"
            class="level-card card"
            :class="level.status"
            @tap="level.status !== 'locked' && startQuiz(level)"
          >
            <text v-if="unit.isBoss" class="boss-icon">{{ level.status === 'locked' ? '🔒' : '👑' }}</text>
            <text class="level-num" :class="level.status">{{ level.num }}</text>
            <view class="level-stars">
              <text v-for="s in 3" :key="s" class="level-star" :class="{ filled: s <= level.stars }">⭐</text>
            </view>
            <text class="level-status-text text-xs">{{ statusText(level) }}</text>
          </view>
        </view>
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppLayout from '@/components/AppLayout.vue'
import { useLearnStore } from '@/store/learn'
import { useUserStore } from '@/store/user'
import { getLevels } from '@/api/learn'
import { getUserInfo } from '@/api/user'

const learnStore = useLearnStore()
const userStore = useUserStore()
const courseName = ref(learnStore.currentCourse?.name || '课程')

const units = ref([])

async function loadLevels() {
  const courseId = learnStore.currentCourse?.id
  if (courseId) {
    try {
      const res = await getLevels(courseId)
      if (res && Array.isArray(res) && res.length > 0) {
        const levelList = res.map(l => ({
          id: l.id,
          num: l.levelNum || l.id,
          name: l.levelName || `第${l.levelNum}关`,
          stars: l.myStars || 0,
          status: l.isPassed ? 'completed'
            : l.isUnlock ? 'current'
            : 'locked'
        }))

        // 每5个为一组
        const unitSize = 5
        const groupedUnits = []
        for (let i = 0; i < levelList.length; i += unitSize) {
          const chunk = levelList.slice(i, i + unitSize)
          const unitNum = Math.floor(i / unitSize) + 1
          const completed = chunk.filter(l => l.status === 'completed').length
          groupedUnits.push({
            id: unitNum,
            name: `第${['一','二','三','四','五','六','七','八'][unitNum - 1] || unitNum}单元`,
            completed,
            total: chunk.length,
            isBoss: false,
            levels: chunk
          })
        }
        if (groupedUnits.length > 0) {
          units.value = groupedUnits
        }
      }
    } catch (e) { console.log('使用模拟数据') }
  }
}

function statusText(level) {
  const map = { completed: '已完成', current: '进行中', locked: '未解锁' }
  return map[level.status] || ''
}

function startQuiz(level) {
  learnStore.setLevel(level)
  uni.navigateTo({ url: `/pages/learn/quiz?levelId=${level.id}` })
}

onMounted(() => {
  loadLevels()
})

// 返回时刷新关卡数据和用户信息
onShow(async () => {
  loadLevels()
  try {
    const info = await getUserInfo()
    if (info) userStore.setUserInfo(info)
  } catch (e) { console.log('刷新用户信息失败') }
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.levels-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.unit-section {}

.unit-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.level-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;

  &.boss-grid {
    grid-template-columns: repeat(3, 1fr);
    max-width: 50%;
  }
}

.level-card {
  padding: 16px 8px;
  text-align: center;
  border-radius: $radius-md;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all $transition-fast;
  position: relative;

  &:active { transform: scale(0.95); }

  &.completed {
    border-color: $success;
    background: #F0FFF8;
  }

  &.current {
    border-color: $learn-blue;
    background: #F0F7FF;
  }

  &.locked {
    opacity: 0.5;
    pointer-events: none;
  }
}

.boss-icon {
  font-size: 24px;
  display: block;
  margin-bottom: 4px;
}

.level-num {
  font-size: 24px;
  font-weight: bold;
  display: block;
  margin-bottom: 6px;

  &.completed { color: $success; }
  &.current { color: $learn-blue; }
  &.locked { color: #C0C0C0; }
}

.level-stars {
  display: flex;
  justify-content: center;
  gap: 2px;
  font-size: 12px;
  margin-bottom: 4px;
}

.level-star {
  color: #E0E0E0;
  &.filled { color: $gold; }
}

.level-status-text {
  color: $text-light;
}
</style>
