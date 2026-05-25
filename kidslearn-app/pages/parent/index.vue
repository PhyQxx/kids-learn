<template>
  <AppLayout theme="kids" title="家长中心" :show-back="true" active-nav="/pages/parent/index">
    <view class="parent-page">
      <view class="summary-grid">
        <view class="summary-card">
          <text class="summary-value">{{ report.today.learnMinutes }}</text>
          <text class="summary-label">今日分钟</text>
        </view>
        <view class="summary-card">
          <text class="summary-value">{{ report.today.completedLevels }}</text>
          <text class="summary-label">完成关卡</text>
        </view>
        <view class="summary-card">
          <text class="summary-value">{{ report.today.accuracy }}%</text>
          <text class="summary-label">正确率</text>
        </view>
      </view>

      <view class="panel ai-panel">
        <view class="panel-head">
          <view class="panel-heading">
            <text class="panel-title">AI学习建议</text>
            <text class="panel-subtitle">基于学习报告生成</text>
          </view>
          <button class="save-btn ai-refresh" :disabled="aiLoading" @tap="loadAiSummary(true)">
            {{ aiLoading ? '生成中' : '刷新建议' }}
          </button>
        </view>
        <view v-if="aiLoading && !hasAiSummary" class="empty-text">正在生成建议...</view>
        <view v-else-if="hasAiSummary" class="ai-summary-body">
          <text v-if="aiSummary.summary" class="ai-summary-text">{{ aiSummary.summary }}</text>
          <view v-if="aiSummary.highlights.length" class="ai-section">
            <text class="ai-section-title">学习亮点</text>
            <text v-for="item in aiSummary.highlights" :key="item" class="ai-item">{{ item }}</text>
          </view>
          <view v-if="aiSummary.concerns.length" class="ai-section">
            <text class="ai-section-title">需要关注</text>
            <text v-for="item in aiSummary.concerns" :key="item" class="ai-item">{{ item }}</text>
          </view>
          <view v-if="aiSummary.suggestions.length" class="ai-section">
            <text class="ai-section-title">陪伴建议</text>
            <text v-for="item in aiSummary.suggestions" :key="item" class="ai-item">{{ item }}</text>
          </view>
        </view>
        <view v-else class="empty-text">点击刷新建议后生成 AI 学习建议</view>
      </view>

      <view class="panel">
        <view class="panel-head">
          <text class="panel-title">时间管控</text>
          <button class="save-btn" :disabled="saving" @tap="saveControl">{{ saving ? '保存中' : '保存' }}</button>
        </view>
        <view class="form-row">
          <text class="form-label">每日上限</text>
          <input class="form-input" type="number" v-model="control.dailyLimitMinutes" />
          <text class="form-unit">分钟</text>
        </view>
        <view class="form-row">
          <text class="form-label">允许时段</text>
          <input class="time-input" v-model="control.allowedStartTime" />
          <text class="form-separator">至</text>
          <input class="time-input" v-model="control.allowedEndTime" />
        </view>
        <view class="toggle-row">
          <view>
            <text class="toggle-title">休息提醒</text>
            <text class="toggle-desc">达到上限后暂停进入学习</text>
          </view>
          <switch :checked="control.restReminder" color="#FF6B6B" @change="control.restReminder = $event.detail.value" />
        </view>
        <view class="access-preview" :class="{ blocked: !accessPreview.allowed }">
          <text>{{ accessPreview.allowed ? '当前可开始学习' : accessPreview.message }}</text>
        </view>
      </view>

      <view class="panel">
        <view class="panel-head">
          <text class="panel-title">今日记录</text>
          <text class="panel-subtitle">{{ report.stats.totalTime }} 分钟 / 本月</text>
        </view>
        <view v-if="report.todayRecords.length > 0" class="record-list">
          <view v-for="record in report.todayRecords" :key="record.id" class="record-row">
            <view class="record-main">
              <text class="record-title">{{ record.subjectName || '学习' }} · {{ record.levelName || '练习' }}</text>
              <text class="record-meta">{{ record.playTime }} · {{ record.durationMinutes }} 分钟</text>
            </view>
            <view class="record-score" :class="{ pass: record.isPass }">{{ record.score }}分</view>
          </view>
        </view>
        <view v-else class="empty-text">今天还没有学习记录</view>
      </view>

    </view>
  </AppLayout>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppLayout from '@/components/AppLayout.vue'
import {
  getParentAiSummary,
  getParentReport,
  getTimeControl,
  saveTimeControl
} from '@/api/parent'
import { normalizeParentAiSummary } from '@/utils/parentAiSummary.mjs'
import { evaluateLearningAccess, normalizeTimeControl } from '@/utils/timeControl.mjs'

const report = reactive({
  today: { learnMinutes: 0, completedLevels: 0, accuracy: 0 },
  stats: { totalTime: 0 },
  todayRecords: []
})
const control = reactive(normalizeTimeControl({}))
const aiSummary = reactive(normalizeParentAiSummary({}))
const aiLoading = ref(false)
const saving = ref(false)

const accessPreview = computed(() => evaluateLearningAccess({
  timeControl: control,
  todayMinutes: report.today.learnMinutes,
  now: new Date()
}))

const hasAiSummary = computed(() => Boolean(
  aiSummary.summary ||
  aiSummary.highlights.length ||
  aiSummary.concerns.length ||
  aiSummary.suggestions.length
))

function assignReport(data = {}) {
  Object.assign(report.today, {
    learnMinutes: Number(data.today?.learnMinutes || 0),
    completedLevels: Number(data.today?.completedLevels || 0),
    accuracy: Number(data.today?.accuracy || 0)
  })
  Object.assign(report.stats, {
    totalTime: Number(data.stats?.totalTime || 0)
  })
  report.todayRecords = Array.isArray(data.todayRecords) ? data.todayRecords : []
}

function assignControl(data = {}) {
  Object.assign(control, normalizeTimeControl(data))
}

function assignAiSummary(data = {}) {
  Object.assign(aiSummary, normalizeParentAiSummary(data))
}

async function loadParentData() {
  const [reportResult, controlResult] = await Promise.allSettled([
    getParentReport(),
    getTimeControl()
  ])

  if (reportResult.status === 'fulfilled') assignReport(reportResult.value)
  if (controlResult.status === 'fulfilled') assignControl(controlResult.value)
}

async function loadAiSummary(showResultToast = false) {
  aiLoading.value = true
  try {
    const result = await getParentAiSummary()
    assignAiSummary(result)
    if (showResultToast) {
      uni.showToast({ title: hasAiSummary.value ? '已生成建议' : '暂无建议', icon: 'none' })
    }
  } catch (error) {
    assignAiSummary({})
    if (showResultToast) {
      uni.showToast({ title: '生成失败', icon: 'none' })
    }
  } finally {
    aiLoading.value = false
  }
}

async function saveControl() {
  saving.value = true
  try {
    await saveTimeControl({
      dailyLimitMinutes: Number(control.dailyLimitMinutes || 0),
      allowedStartTime: control.allowedStartTime,
      allowedEndTime: control.allowedEndTime,
      restReminder: control.restReminder,
      autoLockAfterTask: control.autoLockAfterTask
    })
    uni.showToast({ title: '已保存', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

onShow(loadParentData)
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.parent-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.summary-card,
.panel {
  background: #fff;
  border-radius: $radius-lg;
  box-shadow: 0 8px 24px rgba(73, 98, 128, 0.08);
  border: 1px solid rgba(73, 98, 128, 0.08);
}

.summary-card {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.summary-value {
  font-size: 28px;
  line-height: 1.1;
  font-weight: 800;
  color: $primary;
}

.summary-label,
.panel-subtitle,
.form-unit,
.form-separator,
.toggle-desc,
.record-meta,
.empty-text {
  color: $text-light;
  font-size: 13px;
}

.panel-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
  gap: 16px;
}

.panel {
  padding: 18px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-title {
  color: $text;
  font-size: 18px;
  font-weight: 800;
}

.panel-heading {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.save-btn {
  margin-right: 0;
  min-width: 78px;
  height: 36px;
  line-height: 36px;
  padding: 0 16px;
  border-radius: $radius;
  background: $primary;
  color: #fff;
  font-size: 14px;
}

.ai-refresh {
  min-width: 92px;
  white-space: nowrap;
}

.ai-panel {
  border-color: rgba(78, 205, 196, 0.24);
}

.ai-summary-body {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.ai-summary-text {
  color: $text;
  font-size: 15px;
  line-height: 1.7;
  font-weight: 700;
}

.ai-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ai-section-title {
  color: $text-secondary;
  font-size: 13px;
  font-weight: 800;
}

.ai-item {
  color: $text;
  font-size: 14px;
  line-height: 1.6;
}

.form-row,
.toggle-row,
.record-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.form-row,
.toggle-row {
  min-height: 48px;
  border-bottom: 1px solid rgba(73, 98, 128, 0.08);
}

.form-label {
  width: 76px;
  color: $text-secondary;
  font-size: 14px;
}

.form-input,
.time-input {
  height: 36px;
  padding: 0 10px;
  border-radius: $radius-sm;
  background: #F6F8FB;
  color: $text;
  font-size: 14px;
}

.form-input {
  width: 92px;
}

.time-input {
  width: 92px;
}

.toggle-row {
  justify-content: space-between;
}

.toggle-title {
  display: block;
  color: $text;
  font-size: 14px;
  font-weight: 700;
}

.access-preview {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: $radius;
  background: #E8F8F0;
  color: #1E8449;
  font-size: 14px;
}

.access-preview.blocked {
  background: #FFF0F0;
  color: #C0392B;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.record-main {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.record-title {
  color: $text;
  font-size: 15px;
  font-weight: 700;
}

.record-score {
  min-width: 58px;
  height: 32px;
  line-height: 32px;
  text-align: center;
  border-radius: $radius-sm;
  background: #FFF8E0;
  color: #B7791F;
  font-weight: 800;
}

.record-score.pass {
  background: #E8F8F0;
  color: #1E8449;
}

.empty-text {
  padding: 18px 0 6px;
  text-align: center;
}

@media (max-width: 900px) {
  .summary-grid,
  .panel-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .ai-summary-body {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .summary-grid,
  .panel-grid {
    grid-template-columns: 1fr;
  }

  .ai-summary-body {
    grid-template-columns: 1fr;
  }
}
</style>
