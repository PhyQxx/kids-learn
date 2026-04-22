<template>
  <AppLayout theme="learn" title="课程列表" :show-back="true" active-nav="/pages/learn/index">
    <template #topbar-left-extra>
      <view class="subject-badge badge-blue" style="margin-left: 8px;">
        <text class="text-xs">{{ subjectName }}</text>
      </view>
    </template>

    <view class="courses-content">
      <view
        v-for="course in courses"
        :key="course.id"
        class="course-card card card-hover"
        :class="{ vip: course.isVip, locked: course.locked }"
        @tap="!course.locked && goLevels(course)"
      >
        <text class="course-emoji">{{ course.icon }}</text>
        <view class="course-info">
          <text class="course-name text-md text-bold">{{ course.name }}</text>
          <text class="course-meta text-xs text-light">{{ course.levels }}关 · {{ course.difficulty }}</text>
          <view class="progress-bar progress-bar-blue" style="width: 160px; margin-top: 6px;">
            <view class="progress-fill" :style="{ width: course.progress + '%' }"></view>
          </view>
        </view>
        <view class="course-right">
          <view class="stars">
            <text v-for="s in 3" :key="s" :class="s <= course.stars ? 'star-filled' : 'star-empty'">⭐</text>
          </view>
          <view v-if="course.locked">
            <tn-button size="sm" disabled>🔒</tn-button>
          </view>
          <tn-button v-else-if="course.isVip" size="sm" shape="round" type="warning">👑 VIP</tn-button>
          <tn-button v-else size="sm" shape="round" type="primary">{{ course.progress > 0 ? '继续' : '开始' }}</tn-button>
        </view>
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useLearnStore } from '@/store/learn'
import { useUserStore } from '@/store/user'
import { getCourses } from '@/api/learn'

const learnStore = useLearnStore()
const userStore = useUserStore()
const subjectName = ref(learnStore.currentSubject?.name || '学科')

const courses = ref([])

onMounted(async () => {
  const pages = getCurrentPages()
  const page = pages[pages.length - 1]
  const subjectId = page.$page?.options?.subjectId || learnStore.currentSubject?.id
  if (subjectId) {
    try {
      const gradeLevelId = userStore.userInfo?.gradeLevelId || null
      const res = await getCourses(subjectId, gradeLevelId)
      if (res && res.list) {
        courses.value = res.list.map(c => ({
          id: c.id, name: c.courseName, icon: '📚',
          levels: c.totalLevels || 0,
          difficulty: ['简单','普通','困难'][(c.difficulty || 2) - 1] || '普通',
          progress: c.totalLevels ? Math.round((c.completedLevels || 0) / c.totalLevels * 100) : 0,
          stars: c.totalStars || 0,
          isVip: c.isElite === 1, locked: false
        }))
      }
    } catch (e) {
      console.log('courses: API 失败，列表为空')
    }
  }
})

function goLevels(course) {
  learnStore.setCourse(course)
  uni.navigateTo({ url: `/pages/learn/levels?courseId=${course.id}` })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.subject-badge {
  display: inline-flex;
  padding: 4px 12px;
  border-radius: 100px;
}

.courses-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.course-card {
  display: flex;
  align-items: center;
  padding: 20px 24px;
  gap: 20px;
  border: 2px solid transparent;
  transition: all $transition-fast;

  &:active { transform: scale(0.98); }
  &.vip {
    border-color: $gold;
    background: linear-gradient(135deg, #FFFEF5, #FFF8E8);
  }
  &.locked {
    opacity: 0.6;
    pointer-events: none;
  }
}

.course-emoji { font-size: 48px; flex-shrink: 0; }

.course-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.course-meta { margin-top: 2px; }

.course-right {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.stars {
  display: flex;
  gap: 2px;
  font-size: 14px;
}
.star-filled { color: $gold; }
.star-empty { color: #E0E0E0; }
</style>
