<template>
  <div class="task-center">
    <AdminPageHeader title="任务中心" description="批量生成、AI 处理和导入导出任务可以在后台继续执行。" :count="taskStore.tasks.length">
      <template #secondary><el-button :disabled="!hasFinished" @click="taskStore.clearFinished">清除已完成</el-button></template>
    </AdminPageHeader>

    <div v-if="taskStore.tasks.length" class="task-list">
      <article v-for="task in taskStore.tasks" :key="task.id" class="task-card">
        <div class="task-card__top">
          <div>
            <div class="task-card__title-row">
              <h2>{{ task.title }}</h2>
              <AdminStatusTag :label="statusLabel(task.status)" :tone="statusTone(task.status)" />
            </div>
            <p>{{ task.message }} · {{ formatTime(task.createdAt) }}</p>
          </div>
          <el-button v-if="task.cancellable && ['queued', 'running'].includes(task.status)" link type="danger" @click="taskStore.cancelTask(task.id)">取消任务</el-button>
          <el-button v-else-if="['failed', 'partial', 'cancelled'].includes(task.status)" link type="primary" @click="router.push(retryPath(task))">重新发起</el-button>
        </div>
        <el-progress :percentage="progress(task)" :status="task.status === 'failed' ? 'exception' : task.status === 'success' ? 'success' : undefined" />
        <div class="task-card__meta">
          <span>总计 {{ task.total }}</span>
          <span class="success">成功 {{ task.successCount }}</span>
          <span :class="{ danger: task.failCount > 0 }">失败 {{ task.failCount }}</span>
          <span>已处理 {{ task.completed }}</span>
        </div>
        <details v-if="task.failures.length" class="task-card__failures">
          <summary>查看失败明细（{{ task.failures.length }}）</summary>
          <ul><li v-for="item in task.failures" :key="`${item.label}-${item.reason}`"><b>{{ item.label }}</b>：{{ item.reason }}</li></ul>
        </details>
      </article>
    </div>
    <AdminEmptyState v-else title="暂无后台任务" description="从题库等页面发起批量操作后，可在这里查看进度和失败明细。" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AdminEmptyState from '@/components/admin/AdminEmptyState.vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import AdminStatusTag from '@/components/admin/AdminStatusTag.vue'
import { useAdminTaskStore, type AdminTask, type AdminTaskStatus } from '@/stores/adminTasks'

const taskStore = useAdminTaskStore()
const router = useRouter()
const hasFinished = computed(() => taskStore.tasks.some(task => !['queued', 'running'].includes(task.status)))

function progress(task: AdminTask) { return task.total ? Math.min(100, Math.round(task.completed / task.total * 100)) : 0 }
function formatTime(value: string) { return new Date(value).toLocaleString('zh-CN', { hour12: false }) }
function statusLabel(status: AdminTaskStatus) { return ({ queued: '等待中', running: '处理中', partial: '部分成功', success: '已完成', failed: '失败', cancelled: '已取消' })[status] }
function statusTone(status: AdminTaskStatus): 'neutral' | 'info' | 'success' | 'warning' | 'danger' {
  return ({ queued: 'neutral', running: 'info', partial: 'warning', success: 'success', failed: 'danger', cancelled: 'neutral' })[status] as any
}
function retryPath(task: AdminTask) { return task.type.includes('question') || task.type.includes('audio') || task.type.includes('difficulty') ? '/question-bank' : '/dashboard' }
</script>

<style scoped>
.task-center { height: 100%; overflow-y: auto; }
.task-list { display: grid; gap: var(--space-3); }
.task-card { padding: var(--space-5); background: var(--admin-surface); border: 1px solid var(--admin-border); border-radius: var(--radius-card); }
.task-card__top, .task-card__title-row, .task-card__meta { display: flex; align-items: center; }
.task-card__top { justify-content: space-between; gap: var(--space-4); margin-bottom: var(--space-4); }
.task-card__title-row { gap: var(--space-2); }
h2 { margin: 0; font-size: var(--font-size-heading-2); }
p { margin: var(--space-1) 0 0; color: var(--admin-muted); }
.task-card__meta { gap: var(--space-5); margin-top: var(--space-2); color: var(--admin-muted); font-size: var(--font-size-caption); }
.success { color: var(--color-success-600); }
.danger { color: var(--color-danger-600); }
.task-card__failures { margin-top: var(--space-3); color: var(--color-gray-700); }
.task-card__failures summary { cursor: pointer; font-weight: 600; }
.task-card__failures ul { margin: var(--space-2) 0 0; padding-left: var(--space-5); }
@media (max-width: 767px) { .task-card__meta { gap: var(--space-2) var(--space-4); flex-wrap: wrap; } }
</style>
