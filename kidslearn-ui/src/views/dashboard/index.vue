<template>
  <div v-loading="loading" class="dashboard">
    <section class="dashboard-header">
      <div>
        <p class="eyebrow">运营控制台</p>
        <h1>首页概览</h1>
        <p class="header-copy">汇总用户、学习、内容、商业与系统风险，快速判断今天要处理什么。</p>
      </div>
      <div class="header-actions">
        <span v-if="lastUpdated" class="last-updated">更新于 {{ lastUpdated }}</span>
        <el-segmented v-model="timeRange" :options="timeRangeOptions" />
        <el-button type="primary" plain @click="fetchStats">刷新</el-button>
      </div>
    </section>

    <el-alert v-if="loadError" title="部分概览数据暂时不可用" :description="loadError" type="warning" show-icon :closable="false">
      <template #default><el-button link type="primary" @click="fetchStats">重新加载</el-button></template>
    </el-alert>

    <section class="metric-grid">
      <article
        v-for="metric in summaryMetrics"
        :key="metric.key"
        class="metric-card"
        :class="`tone-${metric.tone}`"
      >
        <div class="metric-topline">
          <span>{{ metric.label }}</span>
          <b>{{ metric.badge }}</b>
        </div>
        <div class="metric-value">{{ metric.value }}</div>
        <div class="metric-note">{{ metric.note }}</div>
      </article>
    </section>

    <section class="dashboard-grid">
      <div class="main-stack">
        <section class="panel trend-panel">
          <div class="panel-header">
            <div>
              <h2>学习趋势</h2>
              <p>答题数、活跃用户和完成率的近 7 日变化</p>
            </div>
            <el-tag type="success" effect="light">趋势稳定</el-tag>
          </div>
          <div class="trend-chart" aria-label="学习趋势柱状图">
            <div v-for="item in trendData" :key="item.day" class="trend-day">
              <div class="bar-group">
                <span class="bar answer" :style="{ height: `${trendHeight(item.answers)}%` }"></span>
                <span class="bar active" :style="{ height: `${trendHeight(item.active)}%` }"></span>
                <span class="bar finish" :style="{ height: `${item.finish}%` }"></span>
              </div>
              <span class="day-label">{{ item.day }}</span>
            </div>
          </div>
          <div class="legend-row">
            <span><i class="legend answer"></i>答题数</span>
            <span><i class="legend active"></i>活跃用户</span>
            <span><i class="legend finish"></i>完成率</span>
          </div>
        </section>

        <section class="panel">
          <div class="panel-header">
            <div>
              <h2>学习漏斗</h2>
              <p>从注册到持续学习的关键转化节点</p>
            </div>
          </div>
          <div class="funnel-list">
            <div v-for="item in funnelData" :key="item.label" class="funnel-row">
              <div class="funnel-label">
                <span>{{ item.label }}</span>
                <b>{{ item.rate }}%</b>
              </div>
              <div class="funnel-track">
                <span :style="{ width: `${item.rate}%`, background: item.color }"></span>
              </div>
              <small>{{ formatNumber(item.count) || '0' }} 人</small>
            </div>
          </div>
        </section>
      </div>

      <div class="insight-stack">
        <section class="panel">
          <div class="panel-header compact">
            <div>
              <h2>学科表现</h2>
              <p>近 7 日闯关占比</p>
            </div>
          </div>
          <div class="progress-list">
            <div v-for="subject in subjectPerformance" :key="subject.name" class="progress-item">
              <div class="progress-meta">
                <span>{{ subject.name }}</span>
                <b>{{ subject.value }}%</b>
              </div>
              <div class="progress-track">
                <span :style="{ width: `${subject.value}%`, background: subject.color }"></span>
              </div>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel-header compact">
            <div>
              <h2>内容健康度</h2>
              <p>题库和关卡供给状态</p>
            </div>
          </div>
          <div class="health-list">
            <div v-for="item in contentHealth" :key="item.label" class="health-item">
              <div>
                <span>{{ item.label }}</span>
                <small>{{ item.desc }}</small>
              </div>
              <el-progress
                :percentage="item.value"
                :color="item.color"
                :stroke-width="8"
                :show-text="false"
              />
              <b>{{ item.value }}%</b>
            </div>
          </div>
        </section>
      </div>

      <aside class="ops-stack">
        <section class="panel">
          <div class="panel-header compact">
            <div>
              <h2>待办与异常</h2>
              <p>优先处理影响体验的事项</p>
            </div>
          </div>
          <div class="todo-list">
            <button v-for="todo in todos" :key="todo.label" type="button" class="todo-item" @click="router.push(todo.path || '/dashboard')">
              <span class="todo-dot" :class="todo.level"></span>
              <span>{{ todo.label }}</span>
              <b>{{ todo.count }}</b>
            </button>
          </div>
        </section>

        <section class="panel">
          <div class="panel-header compact">
            <div>
              <h2>快捷入口</h2>
              <p>高频后台操作</p>
            </div>
          </div>
          <div class="shortcut-grid">
            <button
              v-for="shortcut in shortcuts"
              :key="shortcut.label"
              type="button"
              class="shortcut-button"
              @click="router.push(shortcut.path)"
            >
              <span>{{ shortcut.short }}</span>
              <b>{{ shortcut.label }}</b>
            </button>
          </div>
        </section>

        <section class="panel">
          <div class="panel-header compact">
            <div>
              <h2>最近动态</h2>
              <p>内容和系统变更</p>
            </div>
          </div>
          <div class="activity-list">
            <div v-for="activity in recentActivities" :key="activity.title" class="activity-item">
              <span>{{ activity.time }}</span>
              <b>{{ activity.title }}</b>
              <small>{{ activity.desc }}</small>
            </div>
          </div>
        </section>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboardStats } from '@/api/request'

type MetricTone = 'coral' | 'teal' | 'gold' | 'blue' | 'violet' | 'red'

type SummaryMetric = {
  key: string
  label: string
  value: string
  badge: string
  note: string
  tone: MetricTone
}

type TrendPoint = {
  day: string
  answers: number
  active: number
  finish: number
}

type FunnelPoint = {
  label: string
  rate: number
  count: number | string
  color: string
}

type ProgressPoint = {
  name: string
  value: number
  count?: number
  color: string
}

type HealthPoint = {
  label: string
  desc: string
  value: number
  color: string
}

type TodoPoint = {
  label: string
  count: number
  level: string
  path?: string
}

type ActivityPoint = {
  time: string
  title: string
  desc: string
}

const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const lastUpdated = ref('')
const timeRange = ref('近 7 天')
const timeRangeOptions = ['今日', '近 7 天', '近 30 天']

const summaryMetrics = ref<SummaryMetric[]>([
  { key: 'totalUsers', label: '注册用户', value: '0', badge: '实时', note: '累计注册普通账号', tone: 'coral' },
  { key: 'todayActiveUsers', label: '今日活跃', value: '0', badge: '今日', note: '今日有学习统计的用户', tone: 'teal' },
  { key: 'completionRate', label: '闯关完成率', value: '0%', badge: '近 7 天', note: '学习记录通过率', tone: 'gold' },
  { key: 'totalQuestions', label: '题目总量', value: '0', badge: '实时', note: '题库题目供给', tone: 'blue' },
  { key: 'totalOrders', label: '订单数量', value: '0', badge: '实时', note: '订阅和会员订单', tone: 'violet' },
  { key: 'pendingItems', label: '待处理', value: '0', badge: '需关注', note: '异常、审核与反馈合计', tone: 'red' },
])

const trendData = ref<TrendPoint[]>([])
const funnelData = ref<FunnelPoint[]>([])
const subjectPerformance = ref<ProgressPoint[]>([])
const contentHealth = ref<HealthPoint[]>([])
const todos = ref<TodoPoint[]>([])

const shortcuts = [
  { label: '新增题目', short: '题', path: '/question-bank' },
  { label: '关卡管理', short: '关', path: '/content' },
  { label: '用户管理', short: '人', path: '/system/user' },
  { label: '版本发布', short: '版', path: '/system/version' },
]

const recentActivities = ref<ActivityPoint[]>([])

const trendMax = computed(() => Math.max(
  1,
  ...trendData.value.flatMap(item => [Number(item.answers) || 0, Number(item.active) || 0])
))

function trendHeight(value: number) {
  return Math.max(8, Math.round((Number(value) || 0) / trendMax.value * 100))
}

function formatNumber(value: unknown) {
  if (typeof value === 'number') return value.toLocaleString('zh-CN')
  if (typeof value === 'string' && value.trim()) return value
  return null
}

function setMetricValue(key: string, value: unknown) {
  const formatted = formatNumber(value)
  if (!formatted) return

  const metric = summaryMetrics.value.find(item => item.key === key)
  if (metric) metric.value = formatted
}

function setPercentMetricValue(key: string, value: unknown) {
  const metric = summaryMetrics.value.find(item => item.key === key)
  if (metric && typeof value === 'number') metric.value = `${value}%`
}

function hydrateList<T>(value: unknown, fallback: T[] = []) {
  return Array.isArray(value) ? value as T[] : fallback
}

async function fetchStats() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getDashboardStats()
    if (res.code === 200 && res.data) {
      setMetricValue('totalUsers', res.data.totalUsers)
      setMetricValue('todayActiveUsers', res.data.todayActiveUsers)
      setPercentMetricValue('completionRate', res.data.completionRate)
      setMetricValue('totalQuestions', res.data.totalQuestions)
      setMetricValue('totalOrders', res.data.totalOrders)
      setMetricValue('pendingItems', res.data.pendingItems)
      trendData.value = hydrateList<TrendPoint>(res.data.trendData)
      funnelData.value = hydrateList<FunnelPoint>(res.data.funnelData)
      subjectPerformance.value = hydrateList<ProgressPoint>(res.data.subjectPerformance)
      contentHealth.value = hydrateList<HealthPoint>(res.data.contentHealth)
      todos.value = hydrateList<TodoPoint>(res.data.todos).map(item => ({
        ...item,
        path: item.path || (item.label.includes('审核') ? '/content/audit' : item.label.includes('题') ? '/question-bank' : item.label.includes('订单') ? '/order' : '/tasks'),
      }))
      recentActivities.value = hydrateList<ActivityPoint>(res.data.recentActivities)
      lastUpdated.value = new Date().toLocaleTimeString('zh-CN', { hour12: false, hour: '2-digit', minute: '2-digit' })
    } else {
      loadError.value = res.msg || '服务返回异常，页面保留最近一次成功数据。'
    }
  } catch {
    loadError.value = '网络或服务异常，页面保留最近一次成功数据。'
  } finally {
    loading.value = false
  }
}

// 监听时间范围变化
watch(timeRange, () => fetchStats())

onMounted(() => fetchStats())
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
}

.dashboard-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.eyebrow {
  color: var(--primary);
  font-size: 13px;
  font-weight: 800;
}

.dashboard-header h1 {
  margin-top: 4px;
  color: var(--admin-text);
  font-size: 28px;
  line-height: 1.2;
}

.header-copy {
  margin-top: 8px;
  color: var(--admin-muted);
  font-size: 14px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.last-updated { color: var(--admin-muted); font-size: var(--font-size-caption); }

.metric-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.metric-card,
.panel {
  background: var(--admin-surface);
  border: 1px solid var(--admin-border);
  border-radius: 8px;
  box-shadow: var(--admin-shadow);
}

.metric-card {
  position: relative;
  min-height: 132px;
  padding: 16px;
  overflow: hidden;
}

.metric-card::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  background: var(--metric-color);
}

.tone-coral { --metric-color: #FF6B6B; }
.tone-teal { --metric-color: #4ECDC4; }
.tone-gold { --metric-color: #F6C85F; }
.tone-blue { --metric-color: #6C8CFF; }
.tone-violet { --metric-color: #A78BFA; }
.tone-red { --metric-color: #F05252; }

.metric-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: var(--admin-muted);
  font-size: 13px;
}

.metric-topline b {
  color: var(--metric-color);
  font-size: 12px;
  white-space: nowrap;
}

.metric-value {
  margin-top: 14px;
  color: var(--admin-text);
  font-size: 28px;
  font-weight: 800;
  line-height: 1;
}

.metric-note {
  margin-top: 12px;
  color: var(--admin-muted);
  font-size: 12px;
  line-height: 1.4;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.86fr) 300px;
  gap: 16px;
  align-items: start;
}

.main-stack,
.insight-stack,
.ops-stack {
  display: grid;
  gap: 16px;
}

.panel {
  min-width: 0;
  padding: 18px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.panel-header.compact {
  margin-bottom: 14px;
}

.panel h2 {
  color: var(--admin-text);
  font-size: 17px;
  line-height: 1.3;
}

.panel p {
  margin-top: 4px;
  color: var(--admin-muted);
  font-size: 12px;
}

.trend-chart {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 12px;
  height: 226px;
  padding-top: 14px;
  border-bottom: 1px solid var(--admin-border);
}

.trend-day {
  min-width: 0;
  display: grid;
  grid-template-rows: minmax(0, 1fr) 24px;
  gap: 8px;
  align-items: end;
}

.bar-group {
  display: flex;
  align-items: end;
  justify-content: center;
  gap: 5px;
  height: 100%;
}

.bar {
  width: 12px;
  min-height: 16px;
  border-radius: 6px 6px 0 0;
}

.bar.answer,
.legend.answer {
  background: #FF6B6B;
}

.bar.active,
.legend.active {
  background: #4ECDC4;
}

.bar.finish,
.legend.finish {
  background: #F6C85F;
}

.day-label {
  color: var(--admin-muted);
  font-size: 12px;
  text-align: center;
}

.legend-row {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
  margin-top: 14px;
  color: var(--admin-muted);
  font-size: 12px;
}

.legend-row span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.legend {
  width: 10px;
  height: 10px;
  display: inline-block;
  border-radius: 3px;
}

.funnel-list,
.progress-list,
.health-list,
.todo-list,
.activity-list {
  display: grid;
  gap: 14px;
}

.funnel-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 7px;
}

.funnel-label,
.progress-meta,
.todo-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.todo-item {
  width: 100%;
  padding: var(--space-2);
  color: inherit;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: var(--radius-control);
}

.todo-item:hover,
.todo-item:focus-visible { background: var(--color-gray-50); }

.funnel-label,
.progress-meta {
  color: #4B5563;
  font-size: 13px;
}

.funnel-label b,
.progress-meta b {
  color: var(--admin-text);
}

.funnel-track,
.progress-track {
  height: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: #EEF2F7;
}

.funnel-track span,
.progress-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
}

.funnel-row small {
  color: var(--admin-muted);
  font-size: 12px;
}

.health-item {
  display: grid;
  grid-template-columns: minmax(96px, 1fr) minmax(80px, 0.8fr) 42px;
  gap: 10px;
  align-items: center;
}

.health-item span,
.todo-item span,
.activity-item b {
  color: var(--admin-text);
  font-size: 13px;
}

.health-item small,
.activity-item small,
.activity-item span {
  display: block;
  margin-top: 3px;
  color: var(--admin-muted);
  font-size: 12px;
}

.health-item b {
  color: var(--admin-text);
  font-size: 13px;
  text-align: right;
}

.todo-item {
  min-height: 38px;
  padding: 0 0 10px;
  border-bottom: 1px solid var(--admin-border);
}

.todo-item:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.todo-dot {
  width: 8px;
  height: 8px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: #4ECDC4;
}

.todo-dot.danger {
  background: #F05252;
}

.todo-dot + span {
  flex: 1;
}

.todo-item b {
  color: var(--admin-text);
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.shortcut-button {
  min-width: 0;
  min-height: 76px;
  padding: 12px 8px;
  border: 1px solid var(--admin-border);
  border-radius: 8px;
  background: var(--admin-surface-soft);
  color: var(--admin-text);
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.shortcut-button:hover {
  border-color: var(--primary);
  transform: translateY(-1px);
}

.shortcut-button span {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #fff;
  background: linear-gradient(135deg, var(--primary), var(--teal));
  font-weight: 800;
}

.shortcut-button b {
  display: block;
  margin-top: 9px;
  font-size: 13px;
}

.activity-item {
  position: relative;
  padding-left: 16px;
}

.activity-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 5px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--teal);
}

@media (max-width: 1280px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .dashboard-grid {
    grid-template-columns: minmax(0, 1.3fr) minmax(280px, 0.85fr);
  }

  .ops-stack {
    grid-column: 1 / -1;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1100px) {
  .dashboard-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-actions {
    justify-content: flex-start;
  }

  .metric-grid,
  .dashboard-grid,
  .ops-stack {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .trend-chart {
    gap: 8px;
  }

  .bar {
    width: 8px;
  }

  .health-item {
    grid-template-columns: 1fr;
  }

  .health-item b {
    text-align: left;
  }
}
</style>
