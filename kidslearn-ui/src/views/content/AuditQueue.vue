<template>
  <el-card>
    <template #header>
      <div class="audit-header">
        <span class="audit-title">内容审核</span>
        <div class="audit-filters">
          <el-select v-model="filterStatus" clearable placeholder="状态" style="width: 140px" @change="fetchData">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="2" />
            <el-option label="已驳回" :value="3" />
          </el-select>
          <el-select v-model="filterTargetType" clearable placeholder="对象类型" style="width: 150px" @change="fetchData">
            <el-option label="题目" value="QUESTION" />
            <el-option label="关卡" value="LEVEL" />
            <el-option label="学科" value="SUBJECT" />
          </el-select>
          <el-button type="primary" plain @click="fetchData">刷新</el-button>
        </div>
      </div>
    </template>

    <div ref="tableBox">
    <el-table :data="tableData" stripe v-loading="loading" :max-height="tableMaxHeight">
      <el-table-column prop="targetType" label="对象类型" width="120" />
      <el-table-column prop="targetId" label="对象 ID" width="100" />
      <el-table-column prop="action" label="动作" width="120" />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="submitterId" label="提交人" width="100" />
      <el-table-column prop="submitTime" label="提交时间" width="180" />
      <el-table-column prop="reviewComment" label="审核意见" show-overflow-tooltip />
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :loading="precheckingId === row.id" @click="runAiPrecheck(row)">AI预审</el-button>
          <el-button link type="success" :disabled="row.status !== 0" @click="openReview(row, 2)">通过</el-button>
          <el-button link type="danger" :disabled="row.status !== 0" @click="openReview(row, 3)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0"
      class="pagination"
      :total="total"
      :page-size="pageSize"
      v-model:current-page="currentPage"
      layout="total, prev, pager, next"
      @current-change="fetchData"
    />
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
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getContentAuditList,
  precheckContentAudit,
  reviewContentAudit,
  type ContentAiPrecheckResult,
  type ContentAuditRecord,
} from '@/api/request'
import { useTableHeight } from '@/composables/useTableHeight'

const { tableBox, tableMaxHeight } = useTableHeight()

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
    }
  } finally {
    loading.value = false
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
  reviewing.value = true
  try {
    const res = await reviewContentAudit(activeReviewId.value, reviewStatus.value, reviewComment.value)
    if (res.code === 200) {
      ElMessage.success('审核已提交')
      reviewDialogVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res.msg || '审核失败')
    }
  } finally {
    reviewing.value = false
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
      precheckDialogVisible.value = true
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
</script>

<style scoped>
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
</style>
