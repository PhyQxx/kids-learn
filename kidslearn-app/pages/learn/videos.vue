<template>
  <AppLayout theme="learn" title="视频课程" :show-back="true" active-nav="/pages/learn/index">
    <template #topbar-left-extra>
      <view class="course-badge badge-blue" style="margin-left: 8px;">
        <text class="text-xs">{{ courseName }}</text>
      </view>
    </template>

    <view v-if="hasVideos" class="videos-shell">
      <view class="player-panel">
        <!-- 锁定视频：显示升级提示而非黑屏播放器 -->
        <view v-if="currentVideo.locked" class="video-locked">
          <text class="locked-icon">🔒</text>
          <text class="text-md text-bold">这是会员专属课程</text>
          <text class="text-sm text-light">开通 VIP 即可解锁全部视频课程</text>
          <tn-button class="locked-cta" type="primary" shape="round" @click="goVip">开通会员</tn-button>
        </view>
        <template v-else>
          <video
            class="video-player"
            :src="currentVideo.videoUrl"
            :poster="currentVideo.coverUrl"
            :initial-time="currentVideo.progressSeconds"
            controls
            @timeupdate="handleTimeUpdate"
            @ended="handleVideoEnded"
          />
          <view class="video-meta">
            <view class="video-title-row">
              <text class="video-title text-md text-bold">{{ currentVideo.title }}</text>
              <text class="duration-pill">{{ currentVideo.durationText }}</text>
            </view>
            <text class="video-desc text-sm text-light">{{ currentVideo.description }}</text>
            <view class="watch-progress">
              <view class="progress-bar progress-bar-blue">
                <view class="progress-fill" :style="{ width: currentVideo.progressPercent + '%' }"></view>
              </view>
              <text class="progress-text text-xs">{{ currentVideo.completed ? '已完成' : currentVideo.progressPercent + '%' }}</text>
            </view>
            <tn-button
              v-if="currentVideo.courseLevelId"
              class="quiz-button"
              type="primary"
              shape="round"
              size="lg"
              @click="goQuiz(currentVideo)"
            >去练习</tn-button>
          </view>
        </template>
      </view>

      <view class="playlist">
        <view
          v-for="video in videos"
          :key="video.id"
          class="playlist-item card"
          :class="{ active: video.id === currentVideo.id, completed: video.completed, locked: video.locked }"
          @tap="selectVideo(video)"
        >
          <image v-if="video.coverUrl" class="playlist-cover" :src="video.coverUrl" mode="aspectFill" />
          <view v-else class="playlist-cover empty-cover">
            <text>课程</text>
          </view>
          <view class="playlist-info">
            <text class="playlist-title text-sm text-bold">
              {{ video.title }}
              <text v-if="video.locked" class="lock-mark">🔒</text>
            </text>
            <text class="playlist-sub text-xs text-light">{{ video.locked ? '会员专属' : (video.durationText + ' · ' + (video.completed ? '已完成' : video.progressPercent + '%')) }}</text>
            <view v-if="!video.locked" class="mini-progress">
              <view class="mini-progress-fill" :style="{ width: video.progressPercent + '%' }"></view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view v-else class="empty-state">
      <text class="text-md text-bold">暂无视频课程</text>
      <text class="text-sm text-light">可以先从关卡练习开始</text>
      <tn-button type="primary" shape="round" @click="goLevels">去练习</tn-button>
    </view>
  </AppLayout>
</template>

<script setup>
import { computed, onUnmounted, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import AppLayout from '@/components/AppLayout.vue'
import { getCourseVideos, submitVideoProgress } from '@/api/learn'
import { useLearnStore } from '@/store/learn'
import { useUserStore } from '@/store/user'
import { normalizeVideoCourses, updateVideoProgress } from '@/utils/videoCourse.mjs'

const learnStore = useLearnStore()
const userStore = useUserStore()

const courseId = ref(null)
const courseName = ref(learnStore.currentCourse?.name || '课程')
const videos = ref([])
const selectedVideoId = ref(null)
let lastReportedSecond = 0

const hasVideos = computed(() => videos.value.length > 0)
const currentVideo = computed(() =>
  videos.value.find(video => video.id === selectedVideoId.value)
  || videos.value[0]
  || {
    id: null,
    title: '',
    description: '',
    videoUrl: '',
    coverUrl: '',
    durationSeconds: 0,
    durationText: '00:00',
    progressSeconds: 0,
    progressPercent: 0,
    completed: false,
  }
)

onLoad((query) => {
  courseId.value = query.courseId || learnStore.currentCourse?.id || null
})

onShow(() => {
  loadVideos()
})

async function loadVideos() {
  if (!courseId.value) return
  if (learnStore.currentCourse?.name) {
    courseName.value = learnStore.currentCourse.name
  }
  try {
    const res = await getCourseVideos(courseId.value)
    videos.value = normalizeVideoCourses(res)
    // 默认选中第一个未锁定的视频（避免首屏就是黑屏锁定态）
    if (!selectedVideoId.value) {
      const firstPlayable = videos.value.find(v => !v.locked) || videos.value[0]
      if (firstPlayable) {
        selectedVideoId.value = firstPlayable.id
        lastReportedSecond = firstPlayable.progressSeconds || 0
      }
    }
  } catch (e) {
    console.log('videos: API failed')
  }
}

function selectVideo(video) {
  if (video.locked) {
    uni.showToast({ title: '该课程为会员专属，开通 VIP 解锁', icon: 'none' })
    return
  }
  reportProgress(currentVideo.value, true)
  selectedVideoId.value = video.id
  lastReportedSecond = video.progressSeconds || 0
}

function goVip() {
  uni.navigateTo({ url: '/pages/mine/vip' })
}

function handleTimeUpdate(event) {
  const detail = event.detail || {}
  const updated = updateVideoProgress(
    currentVideo.value,
    detail.currentTime || 0,
    detail.duration || currentVideo.value.durationSeconds
  )
  patchVideo(updated)
  reportProgress(updated, false)
}

function handleVideoEnded() {
  const updated = updateVideoProgress(
    currentVideo.value,
    currentVideo.value.durationSeconds,
    currentVideo.value.durationSeconds
  )
  patchVideo(updated)
  reportProgress(updated, true)
}

function patchVideo(updated) {
  videos.value = videos.value.map(video => video.id === updated.id ? updated : video)
}

async function reportProgress(video, force = false) {
  if (!video?.id) return
  if (!force && Math.abs(video.progressSeconds - lastReportedSecond) < 15) return
  lastReportedSecond = video.progressSeconds
  try {
    const res = await submitVideoProgress({
      videoId: video.id,
      progressSeconds: video.progressSeconds,
      durationSeconds: video.durationSeconds,
    })
    if (res) {
      const synced = updateVideoProgress(video, res.progressSeconds, res.durationSeconds)
      patchVideo({ ...synced, completed: Boolean(res.completed) || synced.completed })
    }
  } catch (e) {
    console.log('videos: progress sync failed')
  }
}

function goQuiz(video) {
  const gradeLevelId = userStore.userInfo?.gradeLevelId || ''
  uni.navigateTo({ url: `/pages/learn/quiz?levelId=${video.courseLevelId}&gradeLevelId=${gradeLevelId}` })
}

function goLevels() {
  const gradeLevelId = userStore.userInfo?.gradeLevelId || ''
  uni.redirectTo({ url: `/pages/learn/levels?courseId=${courseId.value}&gradeLevelId=${gradeLevelId}` })
}

onUnmounted(() => {
  reportProgress(currentVideo.value, true)
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.course-badge {
  display: inline-flex;
  padding: 4px 12px;
  border-radius: 100px;
}

.videos-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
  align-items: start;
}

.player-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 会员锁定视频提示 */
.video-locked {
  width: 100%;
  aspect-ratio: 16 / 9;
  min-height: 360px;
  border-radius: $radius-md;
  background: linear-gradient(135deg, #1f2937, #374151);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;

  .locked-icon {
    font-size: 48px;
    margin-bottom: 4px;
  }

  text {
    color: #fff;
  }

  .locked-cta {
    margin-top: 12px;
  }
}

.lock-mark {
  margin-left: 4px;
  font-size: 12px;
}

.playlist-item.locked {
  opacity: 0.7;
}

.video-player {
  width: 100%;
  aspect-ratio: 16 / 9;
  min-height: 360px;
  border-radius: $radius-md;
  background: #111827;
  overflow: hidden;
}

.video-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.video-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: space-between;
}

.video-title {
  color: $text;
}

.duration-pill {
  min-width: 58px;
  padding: 4px 10px;
  border-radius: 100px;
  background: #EEF6FF;
  color: $learn-blue;
  font-size: 12px;
  font-weight: 800;
  text-align: center;
}

.video-desc {
  line-height: 1.45;
}

.watch-progress {
  display: flex;
  align-items: center;
  gap: 10px;
}

.watch-progress .progress-bar {
  flex: 1;
  height: 10px;
}

.progress-text {
  min-width: 48px;
  text-align: right;
  color: $learn-blue;
  font-weight: 800;
}

.quiz-button {
  align-self: flex-start;
  margin-top: 4px;
}

.playlist {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.playlist-item {
  display: flex;
  gap: 10px;
  padding: 10px;
  border: 2px solid transparent;
  cursor: pointer;

  &.active {
    border-color: $learn-blue;
    background: #F0F7FF;
  }

  &.completed {
    border-color: $success;
  }
}

.playlist-cover {
  width: 86px;
  height: 58px;
  flex-shrink: 0;
  border-radius: $radius-sm;
  background: #E8F0FE;
}

.empty-cover {
  display: flex;
  align-items: center;
  justify-content: center;
  color: $learn-blue;
  font-size: 20px;
  font-weight: 800;
}

.playlist-info {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.playlist-title {
  color: $text;
  line-height: 1.3;
}

.playlist-sub {
  color: $text-light;
}

.mini-progress {
  width: 100%;
  height: 6px;
  border-radius: 6px;
  background: #E8F0FE;
  overflow: hidden;
}

.mini-progress-fill {
  height: 100%;
  border-radius: 6px;
  background: $learn-blue;
}

.empty-state {
  min-height: 420px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.empty-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #EEF6FF;
  color: $learn-blue;
  font-size: 30px;
  font-weight: 800;
}

@include respond-md-lg {
  .videos-shell {
    grid-template-columns: 1fr;
  }

  .video-player {
    min-height: 260px;
  }
}
</style>
