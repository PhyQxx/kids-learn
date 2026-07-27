<template>
  <div class="audit-page">
    <AdminPageHeader title="内容审核" description="按风险和等待时间处理内容变更。" :count="total">
      <template #secondary><el-button :loading="loading" @click="fetchData"><el-icon><Refresh /></el-icon>刷新</el-button></template>
    </AdminPageHeader>

    <AdminFilterBar :collapsible="false" :loading="loading" @search="applyFilters" @reset="resetFilters">
      <el-select v-model="filterStatus" clearable placeholder="状态"><el-option label="待审核" :value="0" /><el-option label="已通过" :value="2" /><el-option label="已驳回" :value="3" /></el-select>
      <el-select v-model="filterTargetType" clearable placeholder="对象类型"><el-option label="题目" value="QUESTION" /><el-option label="关卡" value="LEVEL" /><el-option label="学科" value="SUBJECT" /></el-select>
    </AdminFilterBar>

    <section class="audit-workspace" v-loading="loading">
      <aside class="audit-queue">
        <button
          v-for="(row, index) in tableData"
          :key="row.id ?? `audit-${index}`"
          type="button"
          :class="['audit-queue__item', { active: selectedAudit?.id === row.id }]"
          @click="selectAudit(row)"
        >
          <div><b>{{ targetTypeLabel(row.targetType) }} #{{ row.targetId }}</b><AdminStatusTag :label="statusLabel(row.status)" :tone="statusTone(row.status)" /></div>
          <p>{{ actionLabel(row.action) }} · 提交人 {{ row.submitterId || '—' }}</p>
          <small>等待 {{ waitingTime(row.submitTime) }}</small>
        </button>
        <AdminEmptyState v-if="!loading && !tableData.length" :type="filterStatus !== '' || filterTargetType ? 'filtered' : 'empty'" />
        <el-pagination v-if="total > pageSize" v-model:current-page="currentPage" small background layout="prev, pager, next" :total="total" :page-size="pageSize" @current-change="fetchData" />
      </aside>

      <main class="audit-detail">
        <template v-if="selectedAudit">
          <header class="audit-detail__header">
            <div>
              <span>{{ targetTypeLabel(selectedAudit.targetType) }}</span>
              <h2>#{{ selectedAudit.targetId }} · {{ actionLabel(selectedAudit.action) }}</h2>
              <p>提交于 {{ selectedAudit.submitTime || '—' }}</p>
            </div>
            <AdminStatusTag :label="statusLabel(selectedAudit.status)" :tone="statusTone(selectedAudit.status)" />
          </header>

          <section class="audit-content-card">
            <h3>当前内容</h3>
            <pre v-if="auditDetail.currentContent">{{ auditDetail.currentContent }}</pre>
            <el-skeleton v-else-if="detailLoading" :rows="6" animated />
            <AdminEmptyState v-else title="内容不可用" description="审核对象可能已被删除或当前账号无权查看。" />
          </section>

          <section v-if="precheckResult.summary" class="inline-precheck">
            <div><AdminStatusTag :label="riskLabel(precheckResult.riskLevel)" :tone="riskTone(precheckResult.riskLevel)" /><b>{{ precheckResult.summary }}</b></div>
            <p v-if="precheckResult.issues?.length">问题：{{ precheckResult.issues.join('；') }}</p>
            <p v-if="precheckResult.suggestions?.length">建议：{{ precheckResult.suggestions.join('；') }}</p>
          </section>

          <section v-if="auditDetail.history.length" class="audit-history">
            <h3>历史记录</h3>
            <div v-for="(item, index) in auditDetail.history" :key="item.id ?? `history-${index}`"><span>{{ item.submitTime }}</span><b>{{ statusLabel(item.status) }}</b><small>{{ item.reviewComment || '无审核意见' }}</small></div>
          </section>

          <footer class="audit-actions">
            <el-button :loading="precheckingId === selectedAudit.id" @click="runAiPrecheck(selectedAudit)"><el-icon><MagicStick /></el-icon>AI 预审</el-button>
            <el-button type="danger" plain :disabled="selectedAudit.status !== 0" @click="openReview(selectedAudit, 3)">驳回</el-button>
            <el-button type="primary" :disabled="selectedAudit.status !== 0" @click="openReview(selectedAudit, 2)">通过</el-button>
          </footer>
        </template>
        <AdminEmptyState v-else title="选择一条审核任务" description="从左侧队列选择内容后，可在这里查看详情并作出审核决定。" />
      </main>
    </section>

    <div v-if="undoAuditId" class="audit-undo" role="status">
      <span>审核已提交，{{ undoSeconds }} 秒内可撤销。</span>
      <el-button link type="primary" :loading="undoing" @click="undoReview">撤销</el-button>
    </div>

    <el-dialog v-model="reviewDialogVisible" :title="reviewStatus === 2 ? '通过审核' : '驳回审核'" width="480px">
      <el-form label-width="80px">
        <el-form-item label="审核意见">
          <el-input v-model="reviewComment" type="textarea" :rows="4" placeholder="填写给运营和内容维护人员看的审核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewing" @click="submitReview">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="precheckDialogVisible" title="AI预审结果" width="560px">
      <div class="precheck-result">
        <div class="precheck-summary">
          <el-tag :type="riskTag(precheckResult.riskLevel)" effect="plain">
            {{ riskLabel(precheckResult.riskLevel) }}
          </el-tag>
          <span>{{ precheckResult.summary || '暂无结论' }}</span>
        </div>
        <div class="precheck-section">
          <div class="section-title">发现的问题</div>
          <el-empty v-if="!precheckResult.issues?.length" description="未发现明显问题" :image-size="60" />
          <ul v-else>
            <li v-for="item in precheckResult.issues" :key="item">{{ item }}</li>
          </ul>
        </div>
        <div class="precheck-section">
          <div class="section-title">修改建议</div>
          <el-empty v-if="!precheckResult.suggestions?.length" description="暂无修改建议" :image-size="60" />
          <ul v-else>
            <li v-for="item in precheckResult.suggestions" :key="item">{{ item }}</li>
          </ul>
        </div>
      </div>
      <template #footer>
        <el-button @click="precheckDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="usePrecheckAsComment">填入审核意见</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getContentAuditDetail,
  getContentAuditList,
  precheckContentAudit,
  reviewContentAudit,
  undoContentAuditReview,
  type ContentAiPrecheckResult,
  type ContentAuditRecord,
} from '@/api/request'
import AdminEmptyState from '@/components/admin/AdminEmptyState.vue'
import AdminFilterBar from '@/components/admin/AdminFilterBar.vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import AdminStatusTag from '@/components/admin/AdminStatusTag.vue'

const loading = ref(false)
const reviewing = ref(false)
const tableData = ref<ContentAuditRecord[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const filterStatus = ref<number | ''>(0)
const filterTargetType = ref('')
const reviewDialogVisible = ref(false)
const activeReviewId = ref<number | null>(null)
const reviewStatus = ref<2 | 3>(2)
const reviewComment = ref('')
const precheckingId = ref<number | null>(null)
const precheckDialogVisible = ref(false)
const precheckResult = ref<ContentAiPrecheckResult>({})
const selectedAudit = ref<ContentAuditRecord | null>(null)
const detailLoading = ref(false)
const auditDetail = ref<{ currentContent: string; history: ContentAuditRecord[] }>({ currentContent: '', history: [] })
const undoAuditId = ref<number | null>(null)
const undoSeconds = ref(5)
const undoing = ref(false)
let undoTimer: number | undefined

async function fetchData() {
  loading.value = true
  try {
    const res = await getContentAuditList({
      page: currentPage.value,
      pageSize: pageSize.value,
      status: filterStatus.value === '' ? undefined : filterStatus.value,
      targetType: filterTargetType.value || undefined,
    })
    if (res.code === 200) {
      tableData.value = res.data.list
      total.value = res.data.total
      if (!selectedAudit.value || !tableData.value.some(item => item.id === selectedAudit.value?.id)) {
        if (tableData.value[0]) await selectAudit(tableData.value[0])
        else selectedAudit.value = null
      }
    }
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  currentPage.value = 1
  selectedAudit.value = null
  fetchData()
}

function resetFilters() {
  filterStatus.value = 0
  filterTargetType.value = ''
  applyFilters()
}

async function selectAudit(row: ContentAuditRecord) {
  selectedAudit.value = row
  precheckResult.value = {}
  detailLoading.value = true
  auditDetail.value = { currentContent: '', history: [] }
  try {
    const id = Number(row.id)
    if (!id) return
    const res = await getContentAuditDetail(id)
    if (res.code === 200) {
      auditDetail.value = { currentContent: res.data?.currentContent || '', history: res.data?.history || [] }
    }
  } finally {
    detailLoading.value = false
  }
}

function openReview(row: ContentAuditRecord, status: 2 | 3) {
  activeReviewId.value = Number(row.id)
  reviewStatus.value = status
  reviewComment.value = status === 2 ? '审核通过' : ''
  reviewDialogVisible.value = true
}

async function submitReview() {
  if (!activeReviewId.value) return
  if (reviewStatus.value === 3 && reviewComment.value.trim().length < 10) {
    ElMessage.warning('驳回原因至少填写 10 个字，便于内容维护人员处理。')
    return
  }
  reviewing.value = true
  try {
    const res = await reviewContentAudit(activeReviewId.value, reviewStatus.value, reviewComment.value)
    if (res.code === 200) {
      ElMessage.success('审核已提交')
      startUndoWindow(activeReviewId.value)
      reviewDialogVisible.value = false
      selectedAudit.value = null
      await fetchData()
    } else {
      ElMessage.error(res.msg || '审核失败')
    }
  } finally {
    reviewing.value = false
  }
}

function startUndoWindow(id: number) {
  window.clearInterval(undoTimer)
  undoAuditId.value = id
  undoSeconds.value = 5
  undoTimer = window.setInterval(() => {
    undoSeconds.value -= 1
    if (undoSeconds.value <= 0) {
      window.clearInterval(undoTimer)
      undoAuditId.value = null
    }
  }, 1000)
}

async function undoReview() {
  if (!undoAuditId.value) return
  undoing.value = true
  try {
    const res = await undoContentAuditReview(undoAuditId.value)
    if (res.code === 200) {
      ElMessage.success('已撤销审核，任务已回到待审核队列')
      undoAuditId.value = null
      window.clearInterval(undoTimer)
      await fetchData()
    } else ElMessage.error(res.msg || '撤销失败')
  } finally {
    undoing.value = false
  }
}

async function runAiPrecheck(row: ContentAuditRecord) {
  const id = Number(row.id)
  if (!id) return
  precheckingId.value = id
  try {
    const res = await precheckContentAudit(id)
    if (res.code === 200) {
      precheckResult.value = res.data || {}
      activeReviewId.value = id
      reviewStatus.value = String(res.data?.riskLevel || '').toUpperCase() === 'HIGH' ? 3 : 2
      precheckDialogVisible.value = false
    } else {
      ElMessage.error(res.msg || 'AI预审失败')
    }
  } finally {
    precheckingId.value = null
  }
}

function usePrecheckAsComment() {
  reviewComment.value = buildPrecheckComment()
  precheckDialogVisible.value = false
  reviewDialogVisible.value = true
}

function statusLabel(status?: number) {
  return ({ 0: '待审核', 2: '已通过', 3: '已驳回' } as Record<number, string>)[Number(status)] || '未知'
}

function statusTag(status?: number): 'success' | 'warning' | 'danger' | 'info' {
  return ({ 0: 'warning', 2: 'success', 3: 'danger' } as Record<number, 'success' | 'warning' | 'danger'>)[Number(status)] || 'info'
}

function statusTone(status?: number): 'neutral' | 'info' | 'success' | 'warning' | 'danger' {
  return ({ 0: 'info', 2: 'success', 3: 'danger' } as Record<number, 'info' | 'success' | 'danger'>)[Number(status)] || 'neutral'
}

function riskTone(level?: string): 'neutral' | 'info' | 'success' | 'warning' | 'danger' {
  return ({ LOW: 'success', MEDIUM: 'warning', HIGH: 'danger' } as Record<string, 'success' | 'warning' | 'danger'>)[String(level || '').toUpperCase()] || 'neutral'
}

function targetTypeLabel(type?: string) {
  return ({ QUESTION: '题目', LEVEL: '关卡', SUBJECT: '学科' } as Record<string, string>)[String(type || '').toUpperCase()] || type || '内容'
}

function actionLabel(action?: string) {
  return ({ create: '新增', update: '修改', delete: '删除', CREATE: '新增', UPDATE: '修改', DELETE: '删除' } as Record<string, string>)[String(action || '')] || action || '内容变更'
}

function waitingTime(value?: string) {
  if (!value) return '未知'
  const minutes = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 60_000))
  if (minutes < 60) return `${minutes} 分钟`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时`
  return `${Math.floor(hours / 24)} 天`
}

function riskLabel(level?: string) {
  return ({ LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险' } as Record<string, string>)[String(level || '').toUpperCase()] || '未评级'
}

function riskTag(level?: string): 'success' | 'warning' | 'danger' | 'info' {
  return ({ LOW: 'success', MEDIUM: 'warning', HIGH: 'danger' } as Record<string, 'success' | 'warning' | 'danger'>)[String(level || '').toUpperCase()] || 'info'
}

function buildPrecheckComment() {
  const result = precheckResult.value
  const lines = [`AI预审：${riskLabel(result.riskLevel)}，${result.summary || '暂无结论'}`]
  if (result.issues?.length) lines.push(`问题：${result.issues.join('；')}`)
  if (result.suggestions?.length) lines.push(`建议：${result.suggestions.join('；')}`)
  return lines.join('\n')
}

onMounted(fetchData)
onBeforeUnmount(() => window.clearInterval(undoTimer))
</script>

<style scoped>
.audit-page {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.audit-workspace {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  overflow: hidden;
  background: var(--admin-surface);
  border: 1px solid var(--admin-border);
  border-radius: var(--radius-card);
}

.audit-queue {
  min-height: 0;
  padding: var(--space-2);
  overflow-y: auto;
  border-right: 1px solid var(--admin-border);
}

.audit-queue__item {
  width: 100%;
  padding: var(--space-3);
  color: var(--admin-text);
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-control);
}

.audit-queue__item:hover,
.audit-queue__item:focus-visible {
  background: var(--color-gray-50);
}

.audit-queue__item.active {
  background: var(--color-brand-50);
  border-color: var(--color-brand-100);
}

.audit-queue__item > div,
.audit-detail__header,
.inline-precheck > div,
.audit-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.audit-undo {
  position: fixed;
  right: var(--space-5);
  bottom: var(--space-5);
  z-index: 30;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  color: white;
  background: var(--color-gray-900);
  border-radius: var(--radius-control);
  box-shadow: var(--admin-shadow-lg);
}

.audit-queue__item p {
  margin: var(--space-2) 0 var(--space-1);
  color: var(--color-gray-600);
}

.audit-queue__item small {
  color: var(--admin-muted);
}

.audit-detail {
  min-height: 0;
  padding: var(--space-5);
  overflow-y: auto;
}

.audit-detail__header span,
.audit-detail__header p {
  color: var(--admin-muted);
  font-size: var(--font-size-caption);
}

.audit-detail__header h2 {
  margin: var(--space-1) 0;
  font-size: var(--font-size-heading-1);
}

.audit-content-card,
.inline-precheck,
.audit-history {
  padding: var(--space-4);
  margin-top: var(--space-4);
  border: 1px solid var(--admin-border);
  border-radius: var(--radius-card);
}

.audit-content-card h3,
.audit-history h3 {
  margin: 0 0 var(--space-3);
  font-size: var(--font-size-heading-2);
}

.audit-content-card pre {
  margin: 0;
  color: var(--color-gray-700);
  font: inherit;
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.inline-precheck {
  background: var(--color-info-50);
  border-color: #b2ddff;
}

.inline-precheck p {
  margin: var(--space-2) 0 0;
  color: var(--color-gray-700);
}

.audit-history > div {
  display: grid;
  grid-template-columns: 160px 80px minmax(0, 1fr);
  gap: var(--space-3);
  padding: var(--space-2) 0;
  border-top: 1px solid var(--admin-border);
}

.audit-history span,
.audit-history small {
  color: var(--admin-muted);
}

.audit-actions {
  position: sticky;
  bottom: calc(-1 * var(--space-5));
  justify-content: flex-end;
  padding: var(--space-4) 0;
  margin-top: var(--space-4);
  background: var(--admin-surface);
  border-top: 1px solid var(--admin-border);
}

.audit-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.audit-title {
  font-weight: 700;
}

.audit-filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.precheck-result {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.precheck-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--admin-border);
  border-radius: 8px;
  background: #f7fbff;
}

.precheck-section {
  padding: 12px;
  border: 1px solid var(--admin-border);
  border-radius: 8px;
}

.section-title {
  margin-bottom: 8px;
  color: var(--admin-text);
  font-weight: 700;
}

.precheck-section ul {
  margin: 0;
  padding-left: 18px;
  color: var(--admin-text);
  line-height: 1.7;
}

@media (max-width: 1023px) {
  .audit-workspace { grid-template-columns: 300px minmax(0, 1fr); }
}

@media (max-width: 767px) {
  .audit-page { height: auto; }
  .audit-workspace { display: block; overflow: visible; }
  .audit-queue { max-height: 360px; border-right: 0; border-bottom: 1px solid var(--admin-border); }
  .audit-detail { overflow: visible; }
  .audit-history > div { grid-template-columns: 1fr; }
}
</style>
