<template>
  <view class="subjects-page">
    <view class="header">
      <view class="back-btn" @click="goBack">⬅️ 返回</view>
      <text class="title">选择学科</text>
    </view>
    <view class="subject-grid">
      <view class="subject-card" v-for="item in subjects" :key="item.code"
        :style="{ background: item.bgColor }" @click="goNext(item)">
        <text class="subject-icon">{{ item.icon }}</text>
        <text class="subject-name">{{ item.name }}</text>
        <text v-if="!isPractice" class="subject-count">{{ item.levelCount }}个关卡</text>
        <view class="progress-bar" v-if="!isPractice && item.progress > 0">
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
      isPractice: false,
      subjects: [],
    }
  },
  onLoad(options) {
    this.isPractice = options.practice === 'true'
    this.loadSubjects()
  },
  methods: {
    goBack() {
      uni.navigateBack()
    },
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
            levelCount: s.levelCount || 0,
            progress: s.progress || 0,
            locked: s.locked || s.status === 0
          }))
        }
      } catch (e) {
        console.log('加载学科失败', e)
      }
    },
    goNext(item) {
      const learnStore = useLearnStore()
      learnStore.setSubject(item)
      if (this.isPractice) {
        // 直接跳转到答题页，传递 practiceModeId
        uni.navigateTo({ url: `/pages/learn/quiz?practiceModeId=${item.id}&timeLimit=0` })
      } else {
        uni.navigateTo({ url: `/pages/learn/levels?subjectId=${item.id}&subjectName=${encodeURIComponent(item.name)}` })
      }
    },
  },
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.subjects-page {
  min-height: 100vh;
  padding: 32rpx;
  background: #F7F7F7;
}

.header {
  margin-bottom: 32rpx;
  display: flex;
  align-items: center;
}

.back-btn {
  font-size: 32rpx;
  color: #666;
  font-weight: bold;
  margin-right: 32rpx;
}

.title {
  font-size: 40rpx;
  font-weight: 800;
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
