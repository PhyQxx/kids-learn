<template>
  <view class="drawer-mask" @tap="onMaskTap">
    <view class="drawer" @tap.stop>
      <!-- 顶部抓握条 -->
      <view class="drawer-handle" />

      <!-- 标题 + 关闭 -->
      <view class="drawer-header">
        <text class="drawer-title">今日小目标 · {{ completedCount }}/{{ totalCount }} 个已完成</text>
        <view class="close-btn" @tap="onCloseTap"><text>关闭</text></view>
      </view>

      <!-- 总进度 -->
      <view class="drawer-summary">
        <view class="summary-bar">
          <view class="summary-fill" :style="{ width: overallPercent + '%' }" />
        </view>
        <text class="summary-percent">{{ overallPercent }}%</text>
      </view>

      <!-- 任务明细 -->
      <scroll-view class="task-list" scroll-y>
        <view
          v-for="task in tasks"
          :key="task.subject"
          class="task-row"
          :class="statusClass(task)"
        >
          <text class="task-icon">{{ resolveIcon(task) }}</text>
          <view class="task-main">
            <text class="task-name">{{ task.subjectName || '学科' }}</text>
            <view class="task-bar">
              <view class="task-fill" :style="{ width: (task.progress || 0) + '%' }" />
            </view>
          </view>
          <view class="task-meta">
            <text class="task-minutes">{{ task.todayMinutes || 0 }}/{{ task.targetMinutes || 5 }}分</text>
            <text class="task-status">{{ statusText(task) }}</text>
          </view>
        </view>
        <view v-if="!tasks.length" class="empty"><text>今天还没有任务，去学习看看吧～</text></view>
      </scroll-view>

      <!-- CTA -->
      <view class="drawer-cta" @tap="onCtaTap"><text>去完成今天任务</text></view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  tasks: { type: Array, default: () => [] }
})

const emit = defineEmits(['close', 'go-learn'])

// 学科 code → emoji 兜底（后端 iconUrl 可能为空，与 HomeContent 保持一致）
const ICON_FALLBACK = {
  CHINESE: '📖', MATH: '🔢', ENGLISH: '🔤', LOGIC: '🧩', SCIENCE: '🔬', MUSIC: '🎵'
}

function resolveIcon(task) {
  const icon = task.subjectIcon
  if (icon && !icon.startsWith('http')) return icon
  return ICON_FALLBACK[task.subject] || '📚'
}

const completedCount = computed(() =>
  props.tasks.filter(t => t.status === 'COMPLETED').length
)
const totalCount = computed(() => props.tasks.length)
const overallPercent = computed(() =>
  totalCount.value ? Math.round(completedCount.value / totalCount.value * 100) : 0
)

function statusText(task) {
  if (task.status === 'COMPLETED') return '已完成'
  if (task.status === 'IN_PROGRESS') return '进行中'
  return '未开始'
}

function statusClass(task) {
  if (task.status === 'COMPLETED') return 'is-completed'
  if (task.status === 'IN_PROGRESS') return 'is-progress'
  return 'is-pending'
}

function onMaskTap() { emit('close') }
function onCloseTap() { emit('close') }
function onCtaTap() { emit('go-learn') }
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.drawer-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 9999;
}

.drawer {
  width: 100%;
  background: $white;
  border-radius: $radius-xl $radius-xl 0 0;
  padding: 12px 20px calc(20px + env(safe-area-inset-bottom));
  animation: slideUp 0.3s $ease-spring;
}

@keyframes slideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.drawer-handle {
  width: 40px;
  height: 4px;
  border-radius: 4px;
  background: $border;
  margin: 0 auto 12px;
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.drawer-title {
  font-size: 17px;
  font-weight: 800;
  color: $text;
}

.close-btn {
  width: 44px;
  height: 44px;
  min-width: 44px;
  min-height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #F5F5F5;
  border-radius: 50%;
  font-size: 16px;
  color: #999;
  &:active { transform: scale(0.92); }
}

.drawer-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.summary-bar {
  flex: 1;
  height: 8px;
  border-radius: 8px;
  background: $border;
  overflow: hidden;
}

.summary-fill {
  height: 100%;
  border-radius: 8px;
  background: linear-gradient(90deg, $primary, $primary-light);
  transition: width 0.3s ease;
}

.summary-percent {
  font-size: 13px;
  font-weight: 700;
  color: $primary;
  min-width: 36px;
  text-align: right;
}

.task-list {
  max-height: 50vh;
  margin-bottom: 16px;
}

.task-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid $border;
  &:last-child { border-bottom: none; }
}

.task-icon {
  font-size: 24px;
  width: 36px;
  text-align: center;
}

.task-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.task-name {
  font-size: 15px;
  font-weight: 700;
  color: $text;
}

.task-bar {
  height: 6px;
  border-radius: 6px;
  background: $border;
  overflow: hidden;
}

.task-fill {
  height: 100%;
  border-radius: 6px;
  background: $primary;
  transition: width 0.3s ease;
}

.task-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.task-minutes {
  font-size: 12px;
  color: $text-light;
}

.task-status {
  font-size: 12px;
  font-weight: 700;
}

.is-completed {
  .task-fill { background: $success; }
  .task-status { color: $success; }
}
.is-progress {
  .task-fill { background: $primary; }
  .task-status { color: $primary; }
}
.is-pending {
  .task-fill { background: $text-light; }
  .task-status { color: $text-light; }
}

.empty {
  padding: 32px 0;
  text-align: center;
  font-size: 14px;
  color: $text-light;
}

.drawer-cta {
  width: 100%;
  min-height: $tap-target-min;
  border-radius: $radius-md;
  background: $primary;
  color: $white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 800;
  box-shadow: 0 8px 18px rgba(255, 122, 89, 0.28);
  cursor: pointer;
  &:active { transform: scale(0.98); }
}

@include respond-sm {
  .drawer { padding: 12px 16px calc(16px + env(safe-area-inset-bottom)); }
  .drawer-title { font-size: 16px; }
  .task-icon { font-size: 22px; width: 32px; }
}
</style>
