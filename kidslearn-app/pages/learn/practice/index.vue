<template>
  <AppLayout theme="learn" :title="title" :show-back="true" active-nav="learn">
    <view class="practice-page">
      <!-- 顶部学科信息 -->
      <view class="hero card">
        <text class="hero-icon">{{ subjectInfo.icon || '📚' }}</text>
        <view class="hero-info">
          <text class="hero-name">{{ subjectInfo.name || '专项练习' }}</text>
          <text class="hero-desc">{{ totalQuestions }} 道题 · 选择你喜欢的练习方式</text>
        </view>
      </view>

      <!-- 模式列表（驾考宝典风格） -->
      <view class="mode-list">
        <view v-for="mode in modes" :key="mode.id" class="mode-card card">
          <view class="mode-head">
            <view class="mode-icon" :class="'icon-' + mode.type">{{ mode.icon }}</view>
            <view class="mode-info">
              <text class="mode-name">{{ mode.name }}</text>
              <text class="mode-desc">{{ mode.description }}</text>
            </view>
          </view>

          <!-- 进度信息 -->
          <view class="mode-stats">
            <view class="stat-line">
              <text class="text-xs text-light">题库共 {{ mode.questionCount || 0 }} 题</text>
              <text v-if="progressMap[mode.id] && progressMap[mode.id].hasSession" class="stat-tag">
                已做 {{ progressMap[mode.id].answered }} / {{ progressMap[mode.id].total }}
              </text>
            </view>
            <view v-if="progressMap[mode.id] && progressMap[mode.id].hasSession" class="progress-bar">
              <view class="progress-fill" :style="{ width: progressPercent(mode.id) + '%' }" />
            </view>
          </view>

          <!-- 操作按钮 -->
          <view class="mode-actions">
            <template v-if="progressMap[mode.id] && progressMap[mode.id].hasSession">
              <tn-button type="primary" size="lg" shape="round" block @click="continueMode(mode)">
                继续答题 · 第 {{ (progressMap[mode.id].currentIndex || 0) + 1 }} 题
              </tn-button>
              <view class="restart-btn" @click="restartMode(mode)">重新开始</view>
            </template>
            <tn-button v-else type="primary" size="lg" shape="round" block @click="startMode(mode)">
              开始练习
            </tn-button>
          </view>
        </view>

        <view v-if="!loading && modes.length === 0" class="empty-state">
          <text class="text-light">该学科下暂无题目</text>
        </view>
      </view>
    </view>
  </AppLayout>
</template>

<script>
import AppLayout from '@/components/AppLayout.vue'
import { getPracticeModes, getPracticeProgress, abandonPractice } from '@/api/learn'

export default {
  components: { AppLayout },
  data() {
    return {
      subjectId: null,
      subjectName: '',
      modes: [],
      progressMap: {},
      loading: true
    }
  },
  computed: {
    title() {
      return this.subjectName ? `${this.subjectName} · 练习` : '专项练习'
    },
    subjectInfo() {
      // 从 modes 里反推学科信息（兜底）
      return { name: this.subjectName, icon: '📚' }
    },
    totalQuestions() {
      if (!this.modes.length) return 0
      return Math.max(...this.modes.map(m => m.questionCount || 0))
    }
  },
  onLoad(options) {
    this.subjectId = options.subjectId
    this.subjectName = decodeURIComponent(options.subjectName || '')
  },
  onShow() {
    // 从答题页返回时刷新进度（断点续做状态可能变化）
    this.loadData()
  },
  methods: {
    progressPercent(modeId) {
      const p = this.progressMap[modeId]
      if (!p || !p.hasSession || !p.total) return 0
      return Math.round(p.answered / p.total * 100)
    },
    async loadData() {
      this.loading = true
      try {
        const modes = await getPracticeModes(this.subjectId)
        this.modes = Array.isArray(modes) ? modes : []
        // 并行拉取每个模式的进度
        const results = await Promise.allSettled(
          this.modes.map(m => getPracticeProgress(m.id))
        )
        const map = {}
        results.forEach((r, i) => {
          if (r.status === 'fulfilled' && r.value && r.value.hasSession) {
            map[this.modes[i].id] = r.value
          }
        })
        this.progressMap = map
      } catch (e) {
        console.error('加载练习模式失败', e)
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    startMode(mode) {
      uni.navigateTo({
        url: `/pages/learn/quiz?practiceModeId=${mode.id}&modeType=${mode.type}&timeLimit=${mode.timeLimitSeconds || 0}`
      })
    },
    continueMode(mode) {
      const p = this.progressMap[mode.id]
      if (!p || !p.hasSession) {
        this.startMode(mode)
        return
      }
      uni.navigateTo({
        url: `/pages/learn/quiz?practiceModeId=${mode.id}&sessionId=${p.sessionId}&modeType=${mode.type}&timeLimit=${mode.timeLimitSeconds || 0}`
      })
    },
    async restartMode(mode) {
      const p = this.progressMap[mode.id]
      if (!p || !p.hasSession) {
        this.startMode(mode)
        return
      }
      const confirmed = await new Promise(resolve => {
        uni.showModal({
          title: '重新开始',
          content: '当前进度将被清空，确定重新开始吗？',
          success: r => resolve(r.confirm)
        })
      })
      if (!confirmed) return
      try {
        await abandonPractice(p.sessionId)
        // 清除本地进度后直接新建会话
        const newMap = { ...this.progressMap }
        delete newMap[mode.id]
        this.progressMap = newMap
        this.startMode(mode)
      } catch (e) {
        uni.showToast({ title: '操作失败', icon: 'none' })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.practice-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
}

.hero-icon {
  font-size: 40px;
  width: 64px;
  height: 64px;
  background: #EEF3FF;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-info {
  flex: 1;
}

.hero-name {
  font-size: 20px;
  font-weight: bold;
  color: $text;
  display: block;
}

.hero-desc {
  font-size: 13px;
  color: $text-secondary;
  display: block;
  margin-top: 4px;
}

.mode-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.mode-card {
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.mode-head {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.mode-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  flex-shrink: 0;
  background: #EEF3FF;

  &.icon-SEQUENTIAL { background: #E8F4FF; }
  &.icon-RANDOM { background: #FFF3E6; }
  &.icon-MOCK_EXAM { background: #F3E8FF; }
}

.mode-info {
  flex: 1;
  min-width: 0;
}

.mode-name {
  font-size: 17px;
  font-weight: bold;
  color: $text;
  display: block;
}

.mode-desc {
  font-size: 13px;
  color: $text-secondary;
  display: block;
  margin-top: 4px;
  line-height: 1.4;
}

.mode-stats {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-tag {
  font-size: 12px;
  color: $primary;
  background: rgba(255, 122, 89, 0.1);
  padding: 2px 10px;
  border-radius: 100px;
  font-weight: bold;
}

.progress-bar {
  height: 6px;
  background: #F0F2F5;
  border-radius: 100px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, $primary, #FF9A7B);
  border-radius: 100px;
  transition: width 0.3s;
}

.mode-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
}

.restart-btn {
  font-size: 13px;
  color: $text-secondary;
  padding: 6px 16px;
  text-decoration: underline;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}
</style>
