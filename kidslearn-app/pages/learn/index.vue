<template>
  <view class="learn-page">
    <view class="header">
      <text class="title">学习中心</text>
      <text class="subtitle">选择你想学习的科目吧！</text>
    </view>
    <view class="subject-grid">
      <view class="subject-card" v-for="item in subjects" :key="item.code"
        :style="{ background: item.bgColor }" @click="goSubject(item)">
        <text class="subject-icon">{{ item.icon }}</text>
        <text class="subject-name">{{ item.name }}</text>
        <text class="subject-count">{{ item.courseCount }}门课程</text>
        <view class="progress-bar" v-if="item.progress > 0">
          <view class="progress-fill" :style="{ width: item.progress + '%' }"></view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getSubjects } from '@/api/learn'
import { useUserStore } from '@/store/user'
import { useLearnStore } from '@/store/learn'

export default {
  data() {
    return {
      subjects: [
        { code: 'CHINESE', name: '语文', icon: '📝', bgColor: '#FFF0F0', courseCount: 0, progress: 0 },
        { code: 'MATH', name: '数学', icon: '🔢', bgColor: '#E8FFF8', courseCount: 0, progress: 0 },
        { code: 'ENGLISH', name: '英语', icon: '🔤', bgColor: '#FFFBE6', courseCount: 0, progress: 0 },
        { code: 'LOGIC', name: '逻辑', icon: '🧩', bgColor: '#F0EAFF', courseCount: 0, progress: 0 },
        { code: 'SCIENCE', name: '科学', icon: '🔬', bgColor: '#E8F8E8', courseCount: 0, progress: 0 },
        { code: 'MUSIC', name: '音乐', icon: '🎵', bgColor: '#FFF3E0', courseCount: 0, progress: 0 },
      ],
    }
  },
  onShow() {
    this.loadSubjects()
  },
  methods: {
    async loadSubjects() {
      try {
        const userStore = useUserStore()
        const gradeLevelId = userStore.userInfo?.gradeLevelId || null
        const res = await getSubjects(gradeLevelId)
        if (res && Array.isArray(res)) {
          const iconMap = { 'CHINESE': '📝', 'MATH': '🔢', 'ENGLISH': '🔤', 'LOGIC': '🧩', 'SCIENCE': '🔬', 'MUSIC': '🎵' }
          const bgMap = { 'CHINESE': '#FFF0F0', 'MATH': '#E8FFF8', 'ENGLISH': '#FFFBE6', 'LOGIC': '#F0EAFF', 'SCIENCE': '#E8F8E8', 'MUSIC': '#FFF3E0' }
          this.subjects = res.map(s => ({
            ...s,
            code: s.code || s.subjectCode,
            name: s.name || s.subjectName,
            icon: iconMap[s.code || s.subjectCode] || '📚',
            bgColor: bgMap[s.code || s.subjectCode] || '#F5F5F5',
            courseCount: s.courseCount || 0,
            progress: s.progress || 0,
            locked: s.locked || s.status === 0
          }))
        }
      } catch (e) {
        console.log('加载学科失败，使用默认数据')
      }
    },
    goSubject(item) {
      const learnStore = useLearnStore()
      learnStore.setSubject(item)
      uni.navigateTo({ url: `/pages/learn/courses?subjectId=${item.id}&subjectCode=${item.code}` })
    },
  },
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.learn-page {
  min-height: 100vh;
  padding: 32rpx;
  padding-bottom: 180rpx;
}

.header {
  margin-bottom: 32rpx;
}

.title {
  font-size: 40rpx;
  font-weight: 800;
  display: block;
}

.subtitle {
  font-size: 26rpx;
  color: #999;
  margin-top: 8rpx;
}

.subject-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 24rpx;
}

.subject-card {
  width: calc(50% - 12rpx);
  padding: 40rpx 32rpx;
  border-radius: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);
}

.subject-icon {
  font-size: 64rpx;
  display: block;
}

.subject-name {
  font-size: 32rpx;
  font-weight: 700;
  margin-top: 16rpx;
  display: block;
}

.subject-count {
  font-size: 24rpx;
  color: #999;
  margin-top: 4rpx;
}

.progress-bar {
  height: 6px;
  background: rgba(0,0,0,0.1);
  border-radius: 3px;
  margin-top: 12rpx;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: $primary;
  border-radius: 3px;
  transition: width 0.3s;
}
</style>
