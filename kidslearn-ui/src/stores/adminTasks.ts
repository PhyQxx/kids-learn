import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export type AdminTaskStatus = 'queued' | 'running' | 'partial' | 'success' | 'failed' | 'cancelled'

export type AdminTask = {
  id: string
  type: string
  title: string
  status: AdminTaskStatus
  total: number
  completed: number
  successCount: number
  failCount: number
  message: string
  createdAt: string
  startedAt?: string
  finishedAt?: string
  cancellable: boolean
  cancelRequested: boolean
  failures: Array<{ label: string; reason: string }>
}

export type AdminTaskReporter = {
  progress: (payload: { completed?: number; success?: number; failed?: number; message?: string }) => void
  addFailure: (label: string, reason: string) => void
  isCancelled: () => boolean
}

const STORAGE_KEY = 'admin_background_tasks'

function restoreTasks(): AdminTask[] {
  try {
    const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]') as AdminTask[]
    return parsed.slice(0, 50).map(task => task.status === 'running' || task.status === 'queued'
      ? { ...task, status: 'failed', message: '页面刷新导致任务中断，请重新发起。', finishedAt: new Date().toISOString() }
      : task)
  } catch {
    return []
  }
}

export const useAdminTaskStore = defineStore('adminTasks', () => {
  const tasks = ref<AdminTask[]>(restoreTasks())
  const activeCount = computed(() => tasks.value.filter(task => ['queued', 'running'].includes(task.status)).length)

  function persist() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(tasks.value.slice(0, 50)))
  }

  function patchTask(id: string, patch: Partial<AdminTask>) {
    const task = tasks.value.find(item => item.id === id)
    if (!task) return
    Object.assign(task, patch)
    persist()
  }

  async function runTask(options: {
    type: string
    title: string
    total: number
    cancellable?: boolean
    runner: (reporter: AdminTaskReporter) => Promise<void>
  }) {
    const id = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
    const task: AdminTask = {
      id,
      type: options.type,
      title: options.title,
      status: 'queued',
      total: options.total,
      completed: 0,
      successCount: 0,
      failCount: 0,
      message: '等待开始',
      createdAt: new Date().toISOString(),
      cancellable: options.cancellable !== false,
      cancelRequested: false,
      failures: [],
    }
    tasks.value.unshift(task)
    persist()

    patchTask(id, { status: 'running', startedAt: new Date().toISOString(), message: '处理中' })
    const reporter: AdminTaskReporter = {
      progress(payload) {
        const current = tasks.value.find(item => item.id === id)
        if (!current) return
        patchTask(id, {
          completed: payload.completed ?? current.completed,
          successCount: payload.success ?? current.successCount,
          failCount: payload.failed ?? current.failCount,
          message: payload.message ?? current.message,
        })
      },
      addFailure(label, reason) {
        const current = tasks.value.find(item => item.id === id)
        if (!current) return
        current.failures.push({ label, reason })
        persist()
      },
      isCancelled() {
        return tasks.value.find(item => item.id === id)?.cancelRequested || false
      },
    }

    try {
      await options.runner(reporter)
      const current = tasks.value.find(item => item.id === id)
      if (!current) return id
      const status: AdminTaskStatus = current.cancelRequested
        ? 'cancelled'
        : current.failCount > 0
          ? (current.successCount > 0 ? 'partial' : 'failed')
          : 'success'
      patchTask(id, {
        status,
        finishedAt: new Date().toISOString(),
        message: status === 'success' ? '处理完成' : status === 'partial' ? '部分项目处理失败' : status === 'cancelled' ? '任务已取消' : '任务失败',
      })
    } catch (error: any) {
      patchTask(id, { status: 'failed', message: error?.message || '任务执行失败', finishedAt: new Date().toISOString() })
    }
    return id
  }

  function cancelTask(id: string) {
    const task = tasks.value.find(item => item.id === id)
    if (!task || !task.cancellable || !['queued', 'running'].includes(task.status)) return
    patchTask(id, { cancelRequested: true, message: '正在取消' })
  }

  function clearFinished() {
    tasks.value = tasks.value.filter(task => ['queued', 'running'].includes(task.status))
    persist()
  }

  return { tasks, activeCount, runTask, cancelTask, clearFinished }
})

