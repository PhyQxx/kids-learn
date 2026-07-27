<template>
  <div class="season-page admin-feature-page">
    <!-- 可视化时间轴 -->
    <el-card class="timeline-card">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <div>
            <span style="font-weight:700">排位赛赛季排期</span>
            <span class="timeline-sub">按日期可视化赛季开始与结束时间，点击条目可编辑</span>
          </div>
          <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap">
            <span class="legend-dot active" />进行中
            <span class="legend-dot settled" />已结算
            <span class="legend-dot draft" />草稿
            <span class="legend-dot today" />今天
            <el-divider direction="vertical" />
            <el-button size="small" :icon="MagicStick" @click="openBatchDialog">批量生成</el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading" class="timeline-body">
        <div v-if="seasons.length === 0 && !loading" class="empty-tip">
          暂无赛季排期，点击右上角「新增赛季」或「批量生成」开始配置
        </div>

        <div v-else class="timeline-scroll">
          <div class="timeline-track" :style="trackStyle">
            <!-- 月份分隔线 -->
            <div
              v-for="m in monthMarkers"
              :key="m.key"
              class="month-grid"
              :style="m.style"
            >
              <span class="month-label">{{ m.label }}</span>
            </div>

            <!-- 今天竖线 -->
            <div v-if="todayPos != null" class="today-line" :style="{ left: todayPos + '%' }">
              <span class="today-tag">{{ todayStr }}</span>
            </div>

            <!-- 赛季条 -->
            <div
              v-for="(s, i) in seasons"
              :key="s.id"
              class="season-bar"
              :class="[s.status, { current: isCurrent(s) }]"
              :style="barStyle(s, i)"
              @click="openDialog(s)"
            >
              <div class="bar-row">
                <span class="bar-name">{{ s.seasonName || s.seasonKey }}</span>
                <span class="bar-cycle">{{ cycleLabel(s) }}</span>
              </div>
              <span class="bar-date">{{ s.startDate }} → {{ s.endDate }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 列表管理 -->
    <el-card class="list-card">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span style="font-weight:700">赛季列表</span>
          <div style="display:flex;gap:12px;align-items:center">
            <el-input v-model="keyword" placeholder="搜索赛季 Key/名称" style="width:220px" @keyup.enter="fetchData" clearable @clear="fetchData" />
            <el-select v-model="filterStatus" placeholder="状态" clearable style="width:120px" @change="fetchData">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="进行中" value="ACTIVE" />
              <el-option label="已结算" value="SETTLED" />
            </el-select>
            <el-button :icon="Refresh" @click="fetchData" :loading="loading">刷新</el-button>
            <el-button type="primary" @click="openDialog()">新增赛季</el-button>
          </div>
        </div>
      </template>

      <div ref="tableBox">
        <el-table :data="seasons" stripe v-loading="loading" :max-height="tableMaxHeight">
          <el-table-column prop="seasonKey" label="赛季 Key" width="150" />
          <el-table-column prop="seasonName" label="赛季名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="startDate" label="开始日期" width="130">
            <template #default="{ row }"><span style="font-weight:600">{{ row.startDate }}</span></template>
          </el-table-column>
          <el-table-column prop="endDate" label="结束日期" width="130">
            <template #default="{ row }"><span style="font-weight:600">{{ row.endDate }}</span></template>
          </el-table-column>
          <el-table-column label="周期" width="90">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ cycleLabel(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="天数" width="80">
            <template #default="{ row }">{{ durationDays(row) }} 天</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
              <el-popconfirm title="确定删除此赛季？删除后当前日期将回退到代码兜底算法。" @confirm="handleDelete(row.id)">
                <template #reference>
                  <el-button link type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination v-if="total > 0" style="margin-top:16px;justify-content:flex-end" :total="total" :page-size="pageSize"
          v-model:current-page="currentPage" layout="total, prev, pager, next" @current-change="fetchData" />
      </div>
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑赛季' : '新增赛季'" width="520">
      <el-form :model="form" label-width="100px">
        <el-form-item label="赛季 Key" required>
          <el-input v-model="form.seasonKey" placeholder="如 S20260720" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="赛季名称" required>
          <el-input v-model="form.seasonName" placeholder="展示名，如 2026-07-20 至 2026-08-16" />
        </el-form-item>
        <el-form-item label="开始日期" required>
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="选择开始日期" style="width:100%" @change="onStartOrCycleChange" />
        </el-form-item>
        <el-form-item label="周期设置" required>
          <el-radio-group v-model="form.cyclePreset" @change="onStartOrCycleChange">
            <el-radio-button value="W1">1 周</el-radio-button>
            <el-radio-button value="W2">2 周</el-radio-button>
            <el-radio-button value="W4">4 周</el-radio-button>
            <el-radio-button value="CUSTOM">自定义</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="结束日期" required>
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" placeholder="选择结束日期" style="width:100%" :disabled="form.cyclePreset !== 'CUSTOM'" @change="onEndManualChange" />
        </el-form-item>
        <el-form-item v-if="form.cyclePreset !== 'CUSTOM'" label="周期预览">
          <el-text type="info" size="small">
            {{ form.startDate || '请选择开始日期' }} 起，共 {{ cycleDays(form.cyclePreset) }} 天（{{ cycleLabelByPreset(form.cyclePreset) }}），结束日期 {{ form.endDate || '—' }}
          </el-text>
        </el-form-item>
        <el-form-item label="状态" required>
          <el-radio-group v-model="form.status">
            <el-radio value="DRAFT">草稿</el-radio>
            <el-radio value="ACTIVE">进行中</el-radio>
            <el-radio value="SETTLED">已结算</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量生成弹窗 -->
    <el-dialog v-model="batchDialogVisible" title="批量生成赛季" width="640">
      <el-alert type="info" show-icon :closable="false" style="margin-bottom:16px">
        按所选周期，从「起始日期」连续生成多个赛季，相邻赛季首尾相接（下个赛季 = 上个赛季结束日 + 1 天）。
      </el-alert>
      <el-form :model="batch" label-width="100px">
        <el-form-item label="起始日期" required>
          <el-date-picker v-model="batch.startDate" type="date" value-format="YYYY-MM-DD" placeholder="第一个赛季的开始日期" style="width:100%" @change="regenBatchPreview" />
        </el-form-item>
        <el-form-item label="周期" required>
          <el-radio-group v-model="batch.cyclePreset" @change="regenBatchPreview">
            <el-radio-button value="W1">1 周</el-radio-button>
            <el-radio-button value="W2">2 周</el-radio-button>
            <el-radio-button value="W4">4 周</el-radio-button>
            <el-radio-button value="CUSTOM">自定义</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="batch.cyclePreset === 'CUSTOM'" label="周期天数" required>
          <el-input-number v-model="batch.customDays" :min="1" :max="365" style="width:100%" @change="regenBatchPreview" />
        </el-form-item>
        <el-form-item label="生成数量" required>
          <el-input-number v-model="batch.count" :min="1" :max="52" style="width:100%" @change="regenBatchPreview" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="batch.status" @change="regenBatchPreview">
            <el-radio value="DRAFT">草稿</el-radio>
            <el-radio value="ACTIVE">进行中</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <div v-if="batchPreview.length" class="batch-preview">
        <div class="batch-preview-head">
          <span>预览（{{ batchPreview.length }} 个赛季）</span>
          <span class="batch-warn" v-if="batchConflicts.length">⚠ {{ batchConflicts.length }} 个 Key 与现有赛季冲突</span>
        </div>
        <div class="batch-preview-list">
          <div v-for="(p, i) in batchPreview" :key="i" class="batch-row" :class="{ conflict: p.conflict }">
            <span class="br-key">{{ p.seasonKey }}</span>
            <span class="br-date">{{ p.startDate }} → {{ p.endDate }}</span>
            <el-tag v-if="p.conflict" size="small" type="danger">冲突</el-tag>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBatchSave" :loading="batchSaving" :disabled="batchConflicts.length > 0">
          生成 {{ batchPreview.length }} 个赛季
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Refresh, MagicStick } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getChallengeSeasonList, saveChallengeSeason, deleteChallengeSeason } from '@/api/request'
import { useTableHeight } from '@/composables/useTableHeight'

const { tableBox, tableMaxHeight } = useTableHeight()

const loading = ref(false)
const saving = ref(false)
const seasons = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(50)
const keyword = ref('')
const filterStatus = ref('')
const dialogVisible = ref(false)

/* ===================== 周期预设 ===================== */
const CYCLE_DAYS: Record<string, number> = { W1: 7, W2: 14, W4: 28 }
function cycleDays(preset: string): number {
  return preset === 'CUSTOM' ? 0 : (CYCLE_DAYS[preset] || 0)
}
/** 按天数推断周期标签：用于历史数据展示 */
function cycleLabel(row: any): string {
  const d = durationDays(row)
  if (d === 7) return '1 周'
  if (d === 14) return '2 周'
  if (d === 28) return '4 周'
  if (d > 0 && d % 7 === 0) return `${d / 7} 周`
  return `${d} 天`
}
function cycleLabelByPreset(preset: string): string {
  return cycleLabelByDays(cycleDays(preset))
}
function cycleLabelByDays(d: number): string {
  if (d === 7) return '1 周'
  if (d === 14) return '2 周'
  if (d === 28) return '4 周'
  if (d > 0 && d % 7 === 0) return `${d / 7} 周`
  return `${d} 天`
}

/* 日期工具：把 YYYY-MM-DD 加/减 n 天，跨月跨年安全 */
function addDays(iso: string, n: number): string {
  const d = new Date(iso + 'T00:00:00')
  d.setDate(d.getDate() + n)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}
function daysBetweenIso(a: string, b: string): number {
  const da = new Date(a + 'T00:00:00').getTime()
  const db = new Date(b + 'T00:00:00').getTime()
  return Math.round((db - da) / 86400000)
}

const form = reactive({
  id: null as number | null,
  seasonKey: '',
  seasonName: '',
  startDate: '',
  endDate: '',
  status: 'DRAFT',
  cyclePreset: 'W4' as string,
})

function inferPreset(row: any): string {
  const d = durationDays(row)
  if (d === 7) return 'W1'
  if (d === 14) return 'W2'
  if (d === 28) return 'W4'
  return 'CUSTOM'
}

/** 周期/开始日期变化时，自动重算结束日期（预设模式下） */
function onStartOrCycleChange() {
  if (form.cyclePreset === 'CUSTOM') return
  if (!form.startDate) return
  form.endDate = addDays(form.startDate, cycleDays(form.cyclePreset) - 1)
  form.seasonName = `${form.startDate} 至 ${form.endDate}`
}
function onEndManualChange() {
  // 用户切到自定义后改结束日期，回写 seasonName
  if (form.startDate && form.endDate) {
    form.seasonName = `${form.startDate} 至 ${form.endDate}`
  }
}

/* ===================== 数据加载 ===================== */
async function fetchData() {
  loading.value = true
  try {
    const res = await getChallengeSeasonList({
      page: currentPage.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      status: filterStatus.value || undefined,
    })
    if (res.code === 200) {
      seasons.value = res.data?.list || []
      total.value = res.data?.total || 0
    }
  } finally {
    loading.value = false
  }
}

function openDialog(row?: any) {
  if (row) {
    const preset = inferPreset(row)
    Object.assign(form, {
      id: row.id,
      seasonKey: row.seasonKey,
      seasonName: row.seasonName,
      startDate: row.startDate,
      endDate: row.endDate,
      status: row.status,
      cyclePreset: preset,
    })
  } else {
    Object.assign(form, {
      id: null,
      seasonKey: '',
      seasonName: '',
      startDate: '',
      endDate: '',
      status: 'DRAFT',
      cyclePreset: 'W4',
    })
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.seasonKey || !form.seasonName || !form.startDate || !form.endDate) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (form.endDate < form.startDate) {
    ElMessage.warning('结束日期不能早于开始日期')
    return
  }
  saving.value = true
  try {
    const res = await saveChallengeSeason({ ...form })
    if (res.code === 200) { ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() }
    else ElMessage.error(res.msg)
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  const res = await deleteChallengeSeason(id)
  if (res.code === 200) { ElMessage.success('删除成功'); fetchData() }
}

/* ===================== 批量生成 ===================== */
const batchDialogVisible = ref(false)
const batchSaving = ref(false)
const batch = reactive({
  startDate: '',
  cyclePreset: 'W4',
  customDays: 28,
  count: 4,
  status: 'DRAFT' as 'DRAFT' | 'ACTIVE',
})
const batchPreview = ref<any[]>([])

function openBatchDialog() {
  // 默认从现有最大结束日期 +1 天开始
  if (seasons.value.length > 0) {
    const maxEnd = seasons.value.reduce((m, s) => (s.endDate > m ? s.endDate : m), seasons.value[0].endDate)
    batch.startDate = addDays(maxEnd, 1)
  } else {
    batch.startDate = todayStr
  }
  batch.cyclePreset = 'W4'
  batch.customDays = 28
  batch.count = 4
  batch.status = 'DRAFT'
  regenBatchPreview()
  batchDialogVisible.value = true
}

function regenBatchPreview() {
  if (!batch.startDate) { batchPreview.value = []; return }
  const days = batch.cyclePreset === 'CUSTOM' ? batch.customDays : cycleDays(batch.cyclePreset)
  const list: any[] = []
  let cursor = batch.startDate
  const existingKeys = new Set(seasons.value.map(s => s.seasonKey))
  for (let i = 0; i < batch.count; i++) {
    const start = cursor
    const end = addDays(start, days - 1)
    const key = 'S' + start.replace(/-/g, '')
    list.push({
      seasonKey: key,
      seasonName: `${start} 至 ${end}`,
      startDate: start,
      endDate: end,
      status: batch.status,
      conflict: existingKeys.has(key),
    })
    cursor = addDays(end, 1)
  }
  batchPreview.value = list
}

const batchConflicts = computed(() => batchPreview.value.filter(p => p.conflict))

async function handleBatchSave() {
  if (batchConflicts.value.length > 0) {
    ElMessage.warning('存在 Key 冲突，请调整起始日期或数量')
    return
  }
  batchSaving.value = true
  let ok = 0
  let fail = 0
  try {
    for (const p of batchPreview.value) {
      try {
        const res = await saveChallengeSeason({
          seasonKey: p.seasonKey,
          seasonName: p.seasonName,
          startDate: p.startDate,
          endDate: p.endDate,
          status: p.status,
        })
        if (res.code === 200) ok++
        else fail++
      } catch {
        fail++
      }
    }
    if (ok > 0) ElMessage.success(`成功生成 ${ok} 个赛季${fail ? `，失败 ${fail} 个` : ''}`)
    else if (fail > 0) ElMessage.error(`全部失败（${fail} 个）`)
    if (ok > 0) { batchDialogVisible.value = false; fetchData() }
  } finally {
    batchSaving.value = false
  }
}

/* ===================== 时间轴可视化 ===================== */
const today = new Date()
const todayStr = today.toISOString().slice(0, 10)

const timelineRange = computed(() => {
  if (seasons.value.length === 0) return null
  let min = seasons.value[0].startDate
  let max = seasons.value[0].endDate
  for (const s of seasons.value) {
    if (s.startDate < min) min = s.startDate
    if (s.endDate > max) max = s.endDate
  }
  // 左右各留 7 天 padding
  return { start: addDays(min, -7), end: addDays(max, 7) }
})

function isCurrent(s: any): boolean {
  return s.status === 'ACTIVE' && s.startDate <= todayStr && s.endDate >= todayStr
}

const trackStyle = computed(() => {
  const rows = Math.max(seasons.value.length, 1)
  return { height: rows * 44 + 12 + 'px' }
})

function barStyle(s: any, i: number) {
  if (!timelineRange.value) return {}
  const range = timelineRange.value
  const total = daysBetweenIso(range.start, range.end)
  if (total <= 0) return {}
  const left = (daysBetweenIso(range.start, s.startDate) / total) * 100
  const width = Math.max((durationDays(s) / total) * 100, 3)
  return {
    left: left + '%',
    width: width + '%',
    top: i * 44 + 8 + 'px',
  }
}

const todayPos = computed(() => {
  if (!timelineRange.value) return null
  const range = timelineRange.value
  if (todayStr < range.start || todayStr > range.end) return null
  const total = daysBetweenIso(range.start, range.end)
  return (daysBetweenIso(range.start, todayStr) / total) * 100
})

const monthMarkers = computed(() => {
  if (!timelineRange.value) return []
  const range = timelineRange.value
  const total = daysBetweenIso(range.start, range.end)
  if (total <= 0) return []
  const markers: { key: string; label: string; style: Record<string, string> }[] = []
  const start = new Date(range.start + 'T00:00:00')
  const end = new Date(range.end + 'T00:00:00')
  const cur = new Date(start.getFullYear(), start.getMonth(), 1)
  while (cur <= end) {
    const monthEnd = new Date(cur.getFullYear(), cur.getMonth() + 1, 0)
    const segStart = cur < start ? start : cur
    const segEnd = monthEnd > end ? end : monthEnd
    const left = (daysBetweenIso(range.start, isoOf(segStart)) / total) * 100
    const width = ((daysBetweenIso(isoOf(segStart), isoOf(segEnd)) + 1) / total) * 100
    markers.push({
      key: `${cur.getFullYear()}-${cur.getMonth()}`,
      label: `${cur.getMonth() + 1}月`,
      style: { left: left + '%', width: width + '%' },
    })
    cur.setMonth(cur.getMonth() + 1)
    cur.setDate(1)
  }
  return markers
})

function isoOf(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function statusText(status: string) {
  return { DRAFT: '草稿', ACTIVE: '进行中', SETTLED: '已结算' }[status] || status
}
function statusType(status: string): 'success' | 'warning' | 'info' {
  return ({ ACTIVE: 'success', DRAFT: 'warning', SETTLED: 'info' } as const)[status] || 'info'
}
function durationDays(row: any) {
  if (!row.startDate || !row.endDate) return 0
  return daysBetweenIso(row.startDate, row.endDate) + 1
}

onMounted(() => fetchData())
</script>

<style scoped>
.season-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  overflow: hidden;
}

.timeline-card :deep(.el-card__body),
.list-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
}

.timeline-card {
  flex-shrink: 0;
}

.list-card {
  flex: 1;
  min-height: 0;
}

.timeline-sub {
  margin-left: 10px;
  font-size: 12px;
  color: var(--admin-muted);
  font-weight: 400;
}

.timeline-body {
  min-height: 160px;
}

.empty-tip {
  text-align: center;
  padding: 40px;
  color: var(--admin-muted);
}

.timeline-scroll {
  overflow-x: auto;
  padding-bottom: 8px;
}

.timeline-track {
  position: relative;
  min-width: 640px;
  padding-top: 24px;
  margin-top: 4px;
}

.month-grid {
  position: absolute;
  top: 0;
  height: 100%;
  border-left: 1px dashed var(--admin-border);
  padding-left: 4px;
}

.month-grid:first-child {
  border-left: none;
}

.month-label {
  position: absolute;
  top: 0;
  left: 4px;
  font-size: 11px;
  color: var(--admin-muted);
  background: var(--admin-surface);
  padding: 0 4px;
}

.today-line {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 2px;
  background: var(--primary);
  z-index: 3;
}

.today-tag {
  position: absolute;
  top: -2px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--primary);
  color: #fff;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 8px;
  white-space: nowrap;
}

.season-bar {
  position: absolute;
  height: 36px;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 10px;
  color: #fff;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
  box-shadow: 0 2px 6px rgba(31, 41, 55, 0.1);
  overflow: hidden;
  z-index: 2;
}

.season-bar:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 14px rgba(31, 41, 55, 0.18);
  z-index: 4;
}

/* 当前进行中的赛季：高亮描边 */
.season-bar.current {
  outline: 2px solid #fff;
  outline-offset: -2px;
  box-shadow: 0 0 0 2px var(--primary), 0 6px 14px rgba(255, 107, 107, 0.28);
  z-index: 5;
}

.season-bar.ACTIVE {
  background: linear-gradient(135deg, #4ECDC4, #3bb4ab);
}

.season-bar.SETTLED {
  background: linear-gradient(135deg, #9aa5b5, #7b8494);
}

.season-bar.DRAFT {
  background: linear-gradient(135deg, #E6A23C, #d68f2c);
}

.bar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.bar-name {
  font-weight: 700;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bar-cycle {
  font-size: 10px;
  padding: 0 5px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.25);
  white-space: nowrap;
  flex-shrink: 0;
}

.bar-date {
  font-size: 10px;
  opacity: 0.92;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.legend-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 3px;
  margin-right: 2px;
}

.legend-dot.active { background: #4ECDC4; }
.legend-dot.settled { background: #9aa5b5; }
.legend-dot.draft { background: #E6A23C; }
.legend-dot.today {
  width: 2px;
  height: 12px;
  background: var(--primary);
  border-radius: 1px;
}

/* 批量生成预览 */
.batch-preview {
  margin-top: 8px;
  border: 1px solid var(--admin-border);
  border-radius: 8px;
  overflow: hidden;
}

.batch-preview-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: var(--admin-surface-soft);
  font-size: 13px;
  font-weight: 600;
  color: var(--admin-text);
}

.batch-warn {
  font-size: 12px;
  color: var(--primary);
  font-weight: 400;
}

.batch-preview-list {
  max-height: 240px;
  overflow-y: auto;
}

.batch-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 12px;
  border-top: 1px solid var(--admin-border);
  font-size: 12px;
}

.batch-row:first-child {
  border-top: none;
}

.batch-row.conflict {
  background: rgba(255, 107, 107, 0.06);
}

.br-key {
  font-weight: 600;
  color: var(--admin-text);
  min-width: 120px;
}

.br-date {
  color: var(--admin-muted);
  flex: 1;
}
</style>
