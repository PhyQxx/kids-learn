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
          <text class="course-meta text-xs text-light">
            {{ course.levels }}关 · {{ course.difficulty }} · {{ course.videoCount }}节视频
          </text>
          <view class="progress-bar progress-bar-blue" style="width: 160px; margin-top: 6px;">
            <view class="progress-fill" :style="{ width: course.progress + '%' }"></view>
          </view>
        </view>
        <view class="course-right">
          <view class="stars">
            <text v-for="s in 3" :key="s" :class="s <= course.starLevel ? 'star-filled' : 'star-empty'">★</text>
          </view>
          <tn-button v-if="course.locked" size="sm" disabled>锁定</tn-button>
          <view v-else class="course-actions">
            <tn-button
              v-if="course.videoCount > 0"
              size="sm"
              shape="round"
              @tap.stop="goVideos(course)"
            >视频</tn-button>
            <tn-button size="sm" shape="round" type="primary">{{ course.progress > 0 ? '继续' : '开始' }}</tn-button>
          </view>
        </view>
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onShow, onLoad } from '@dcloudio/uni-app'
import AppLayout from '@/components/AppLayout.vue'
import { useLearnStore } from '@/store/learn'
import { useUserStore } from '@/store/user'
import { getCourses } from '@/api/learn'

const learnStore = useLearnStore()
const userStore = useUserStore()
const subjectName = ref(learnStore.currentSubject?.name || '学科')
const pageSubjectId = ref(null)

const courses = ref([])

onLoad((query) => {
  pageSubjectId.value = query.subjectId || null
})

async function loadCourses() {
  const subjectId = learnStore.currentSubject?.id || pageSubjectId.value
  if (!subjectId) return
  if (learnStore.currentSubject?.name) {
    subjectName.value = learnStore.currentSubject.name
  }
  try {
    const gradeLevelId = userStore.userInfo?.gradeLevelId || null
    const res = await getCourses(subjectId, gradeLevelId)
    if (res && res.list) {
      courses.value = res.list.map(c => {
        const totalLevels = c.totalLevels || 0
        const completedLevels = c.completedLevels || 0
        const progress = totalLevels ? Math.round(completedLevels / totalLevels * 100) : 0
        return {
          id: c.id,
          name: c.courseName,
          icon: '📘',
          levels: totalLevels,
          difficulty: ['简单', '普通', '困难'][(c.difficulty || 2) - 1] || '普通',
          progress,
          starLevel: Math.min(3, Math.floor((c.totalStars || 0) / Math.max(1, totalLevels))),
          videoCount: c.videoCount || 0,
          isVip: c.isElite === 1,
          locked: false,
        }
      })
    }
  } catch (e) {
    console.log('courses: API failed')
  }
}

onMounted(() => {
  loadCourses()
})

onShow(() => {
  loadCourses()
})

function goLevels(course) {
  learnStore.setCourse(course)
  const gradeLevelId = userStore.userInfo?.gradeLevelId || ''
  uni.navigateTo({ url: `/pages/learn/levels?courseId=${course.id}&gradeLevelId=${gradeLevelId}` })
}

function goVideos(course) {
  learnStore.setCourse(course)
  uni.navigateTo({ url: `/pages/learn/videos?courseId=${course.id}` })
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

.course-emoji {
  font-size: 48px;
  flex-shrink: 0;
}

.course-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.course-meta {
  margin-top: 2px;
}

.course-right {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.course-actions {
  display: flex;
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
