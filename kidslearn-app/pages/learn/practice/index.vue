<template>
  <view class="practice-page">
    <view class="header">
      <view class="back-btn" @click="goBack">⬅️ 返回</view>
      <text class="title">专项练习</text>
    </view>

    <scroll-view scroll-x class="subject-tabs" show-scrollbar="false">
      <view class="tab" :class="{ active: currentSubject === null }" @click="switchSubject(null)">全部</view>
      <view class="tab" v-for="sub in subjects" :key="sub.id" :class="{ active: currentSubject === sub.id }" @click="switchSubject(sub.id)">
        {{ sub.name }}
      </view>
    </scroll-view>

    <scroll-view scroll-y class="content-scroll">
      <view class="section" v-if="endlessModes.length > 0">
        <view class="section-title">基础巩固 (无尽模式)</view>
        <view class="grid">
          <view class="card" v-for="item in endlessModes" :key="item.id" @click="start(item)">
            <view class="card-icon">{{ item.icon }}</view>
            <view class="card-info">
              <text class="card-name">{{ item.name }}</text>
              <text class="card-desc">{{ item.description }}</text>
            </view>
            <view class="btn">开始</view>
          </view>
        </view>
      </view>

      <view class="section" v-if="timedModes.length > 0">
        <view class="section-title">进阶挑战 (限时模式)</view>
        <view class="grid">
          <view class="card challenge" v-for="item in timedModes" :key="item.id" @click="start(item)">
            <view class="tag" v-if="item.tags">{{ item.tags }}</view>
            <view class="card-icon">{{ item.icon }}</view>
            <view class="card-info">
              <text class="card-name">{{ item.name }}</text>
              <text class="card-desc">{{ item.description }}</text>
            </view>
            <view class="btn challenge-btn">挑战</view>
          </view>
        </view>
      </view>

      <view v-if="endlessModes.length === 0 && timedModes.length === 0" class="empty-state">
        <text>暂无相关练习模式</text>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { getSubjects, getPracticeModes } from '@/api/learn'
import { useUserStore } from '@/store/user'

export default {
  data() {
    return {
      subjects: [],
      currentSubject: null,
      modes: []
    }
  },
  computed: {
    endlessModes() {
      return this.modes.filter(m => m.type === 'ENDLESS')
    },
    timedModes() {
      return this.modes.filter(m => m.type === 'TIMED')
    }
  },
  onLoad() {
    this.loadSubjects()
    this.loadModes()
  },
  methods: {
    goBack() {
      uni.navigateBack()
    },
    async loadSubjects() {
      try {
        const userStore = useUserStore()
        const res = await getSubjects(userStore.userInfo?.gradeLevelId)
        if (res && Array.isArray(res)) {
          this.subjects = res
        }
      } catch (e) {
        console.error('Failed to load subjects', e)
      }
    },
    async loadModes() {
      try {
        const res = await getPracticeModes(this.currentSubject)
        if (res && Array.isArray(res)) {
          this.modes = res
        }
      } catch (e) {
        console.error('Failed to load modes', e)
        uni.showToast({ title: '加载失败', icon: 'none' })
      }
    },
    switchSubject(id) {
      this.currentSubject = id
      this.loadModes()
    },
    start(item) {
      uni.navigateTo({
        url: `/pages/learn/practice/quiz?modeId=${item.id}&type=${item.type}&timeLimit=${item.timeLimitSeconds || 0}`
      })
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.practice-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #F5F7FA;
}

.header {
  padding: 32rpx;
  background: #FFF;
  display: flex;
  align-items: center;
  box-shadow: 0 2rpx 10rpx rgba(0,0,0,0.02);
  z-index: 10;
}

.back-btn {
  font-size: 32rpx;
  color: #666;
  font-weight: bold;
  margin-right: 32rpx;
}

.title {
  font-size: 36rpx;
  font-weight: bold;
}

.subject-tabs {
  white-space: nowrap;
  padding: 24rpx 32rpx;
  background: #FFF;
}

.tab {
  display: inline-block;
  padding: 12rpx 32rpx;
  background: #F0F2F5;
  border-radius: 32rpx;
  font-size: 28rpx;
  color: #666;
  margin-right: 16rpx;

  &.active {
    background: #E8F0FE;
    color: $primary;
    font-weight: bold;
  }
}

.content-scroll {
  flex: 1;
  padding: 32rpx;
  box-sizing: border-box;
}

.section {
  margin-bottom: 40rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 24rpx;
}

.grid {
  display: flex;
  flex-wrap: wrap;
  gap: 24rpx;
}

.card {
  width: calc(33.33% - 16rpx);
  background: #FFF;
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
  display: flex;
  flex-direction: column;
  position: relative;
  box-sizing: border-box;

  &.challenge {
    border: 2rpx solid #FFF3E0;
    background: #FFFAF5;
  }
}

.card-icon {
  font-size: 64rpx;
  margin-bottom: 16rpx;
  background: #F5F9FF;
  width: 96rpx;
  height: 96rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.challenge .card-icon {
  background: #FFF3E0;
}

.card-info {
  flex: 1;
}

.card-name {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  display: block;
  margin-bottom: 8rpx;
}

.card-desc {
  font-size: 24rpx;
  color: #999;
  display: block;
  margin-bottom: 24rpx;
  line-height: 1.4;
  height: 68rpx;
  overflow: hidden;
}

.btn {
  background: $primary;
  color: #FFF;
  text-align: center;
  padding: 16rpx 0;
  border-radius: 16rpx;
  font-size: 28rpx;
  font-weight: bold;
}

.challenge-btn {
  background: $warning;
}

.tag {
  position: absolute;
  top: 16rpx;
  right: 16rpx;
  background: #FFE8E8;
  color: #E74C3C;
  font-size: 20rpx;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
  font-weight: bold;
}

.empty-state {
  text-align: center;
  color: #999;
  padding: 100rpx 0;
}
</style>
