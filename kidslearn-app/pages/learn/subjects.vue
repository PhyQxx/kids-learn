<template>
  <AppLayout theme="learn" :title="isPractice ? '选择练习学科' : '选择闯关学科'" :show-back="true" active-nav="learn">
    <view class="subjects-page">
      <view class="subject-hero">
        <image class="subject-hero-art" src="/static/redesign/subject-dioramas.png" mode="aspectFill" />
        <view class="subject-hero-copy">
          <text class="subject-kicker">学科星域</text>
          <text class="subject-hero-title">{{ isPractice ? '选择今天要巩固的学科' : '选择一条闯关航线' }}</text>
          <text class="subject-hero-desc">内容会根据当前年级自动匹配</text>
        </view>
      </view>

      <view class="subject-grid">
        <view class="subject-card" v-for="(item, index) in subjects" :key="item.code" @click="goNext(item)">
          <view class="subject-number"><text>{{ String(index + 1).padStart(2, '0') }}</text></view>
          <text class="subject-name">{{ item.name }}</text>
          <text class="subject-count">{{ isPractice ? '专项练习' : `${item.levelCount} 个关卡` }}</text>
          <view class="progress-bar" v-if="!isPractice">
            <view class="progress-fill" :style="{ width: item.progress + '%' }"></view>
          </view>
          <view class="subject-action"><text>{{ isPractice ? '开始训练' : '查看关卡' }}</text></view>
        </view>
      </view>
    </view>
  </AppLayout>
</template>

<script>
import { getSubjects } from '@/api/learn'
import { useUserStore } from '@/store/user'
import { useLearnStore } from '@/store/learn'
import AppLayout from '@/components/AppLayout.vue'

export default {
  components: { AppLayout },
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
        // 跳转到练习模式选择页（顺序练习/随机练习/模拟考试）
        uni.navigateTo({ url: `/pages/learn/practice/index?subjectId=${item.id}&subjectName=${encodeURIComponent(item.name)}` })
      } else {
        uni.navigateTo({ url: `/pages/learn/levels?subjectId=${item.id}&subjectName=${encodeURIComponent(item.name)}` })
      }
    },
  },
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.subjects-page { display: flex; flex-direction: column; gap: 16px; }
.subject-hero {
  position: relative;
  min-height: 230px;
  overflow: hidden;
  border-radius: 28px;
  border: 1px solid rgba(63,111,229,.10);
  background: #EEF5FF;
  box-shadow: 0 12px 32px rgba(69,91,124,.10);
}
.subject-hero-art { position: absolute; inset: 0; width: 100%; height: 100%; }
.subject-hero::after { content: ''; position: absolute; inset: 0; background: linear-gradient(90deg, rgba(255,255,255,.98), rgba(255,255,255,.78) 38%, rgba(255,255,255,.04) 72%); }
.subject-hero-copy { position: absolute; z-index: 2; left: 28px; top: 28px; display: flex; flex-direction: column; gap: 7px; }
.subject-kicker { color: #3F6FE5; font-size: 13px; font-weight: 850; }
.subject-hero-title { color: #18212F; font-size: 28px; font-weight: 850; }
.subject-hero-desc { color: #5D6A7A; font-size: 14px; }

.subject-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.subject-card {
  position: relative;
  min-height: 164px;
  padding: 18px;
  border-radius: 22px;
  background: #FFFFFF;
  border: 1px solid rgba(63,111,229,.10);
  box-shadow: 0 9px 24px rgba(69,91,124,.08);
  cursor: pointer;
}
.subject-card:active { transform: scale(.985); }
.subject-number { width: 42px; height: 42px; border-radius: 14px; background: #EAF1FF; color: #315EBA; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 900; }
.subject-name { display: block; margin-top: 12px; color: #18212F; font-size: 20px; font-weight: 850; }
.subject-count { display: block; margin-top: 4px; color: #7A8797; font-size: 12px; }

.progress-bar {
  height: 5px;
  background: #E8EEF7;
  border-radius: 3px;
  margin-top: 12px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: $primary;
  border-radius: 3px;
  transition: width 0.3s;
}
.subject-action { position: absolute; right: 14px; bottom: 14px; min-height: 36px; padding: 0 14px; border-radius: 12px; background: #3F7CE5; color: #FFFFFF; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 850; }

@media (min-width: 1200px) and (min-height: 900px) {
  .subject-hero { min-height: 280px; }
  .subject-card { min-height: 190px; }
}

@include respond-md {
  .subject-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
