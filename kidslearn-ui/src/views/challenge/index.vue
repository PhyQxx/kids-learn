<template>
  <el-card class="admin-crud-page">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">挑战赛管理</span>
        <div style="display:flex;gap:12px">
          <el-select v-model="filterType" placeholder="挑战类型" clearable @change="fetchData" style="width:140px">
            <el-option label="限时挑战" value="timed" />
            <el-option label="无限挑战" value="endless" />
            <el-option label="每日挑战" value="daily" />
          </el-select>
          <el-button :icon="Refresh" @click="fetchData" :loading="loading">刷新</el-button>
          <el-button type="primary" @click="openDialog()">新增挑战</el-button>
        </div>
      </div>
    </template>

    <div ref="tableBox">
      <el-table :data="tableData" stripe v-loading="loading" :max-height="tableMaxHeight">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="challengeName" label="挑战名称" min-width="150" />
        <el-table-column prop="challengeType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ typeText(row.challengeType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="subjectName" label="学科" width="100" />
        <el-table-column prop="timeLimitSeconds" label="限时(秒)" width="100" />
        <el-table-column prop="questionCount" label="题目数" width="90" />
        <el-table-column prop="rewardGold" label="奖励金币" width="100">
          <template #default="{ row }">
            <span style="color:#E6A23C">🪙 {{ row.rewardGold }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="rewardExp" label="奖励经验" width="100">
          <template #default="{ row }">
            <span style="color:#67C23A">⚡ {{ row.rewardExp }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-if="total > 0" style="margin-top:16px;justify-content:flex-end" :total="total" :page-size="pageSize"
        v-model:current-page="currentPage" layout="total, prev, pager, next" @current-change="fetchData" />
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑挑战' : '新增挑战'" width="600">
      <el-form :model="form" label-width="100px">
        <el-form-item label="挑战名称"><el-input v-model="form.challengeName" /></el-form-item>
        <el-form-item label="挑战类型">
          <el-select v-model="form.challengeType" style="width:100%">
            <el-option label="限时挑战" value="timed" />
            <el-option label="无限挑战" value="endless" />
            <el-option label="每日挑战" value="daily" />
          </el-select>
        </el-form-item>
        <el-form-item label="学科">
          <el-select v-model="form.subjectId" style="width:100%" clearable>
            <el-option v-for="s in subjects" :key="s.id" :label="s.subjectName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="限时(秒)"><el-input-number v-model="form.timeLimitSeconds" :min="0" :max="600" /></el-form-item>
        <el-form-item label="题目数"><el-input-number v-model="form.questionCount" :min="5" :max="50" /></el-form-item>
        <el-form-item label="奖励金币"><el-input-number v-model="form.rewardGold" :min="0" /></el-form-item>
        <el-form-item label="奖励经验"><el-input-number v-model="form.rewardExp" :min="0" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getChallengeList, saveChallenge, deleteChallenge, getSubjectList } from '@/api/request'
import { useTableHeight } from '@/composables/useTableHeight'

const { tableBox, tableMaxHeight } = useTableHeight()

const loading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const subjects = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const filterType = ref('')
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  challengeName: '',
  challengeType: 'timed',
  subjectId: null as number | null,
  timeLimitSeconds: 60,
  questionCount: 10,
  rewardGold: 20,
  rewardExp: 15,
  description: '',
  status: 1,
})

function typeText(type: string) {
  return { timed: '限时', endless: '无限', daily: '每日' }[type] || type
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getChallengeList({ page: currentPage.value, pageSize: pageSize.value, type: filterType.value || undefined })
    if (res.code === 200) {
      tableData.value = res.data?.list || []
      total.value = res.data?.total || 0
    }
  } finally {
    loading.value = false
  }
}

async function fetchSubjects() {
  const res = await getSubjectList({})
  if (res.code === 200) subjects.value = res.data?.list || []
}

function openDialog(row?: any) {
  if (row) {
    editingId.value = row.id
    Object.assign(form, row)
  } else {
    editingId.value = null
    Object.assign(form, { challengeName: '', challengeType: 'timed', subjectId: null, timeLimitSeconds: 60, questionCount: 10, rewardGold: 20, rewardExp: 15, description: '', status: 1 })
  }
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    const res = await saveChallenge({ ...form, id: editingId.value })
    if (res.code === 200) { ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() }
    else ElMessage.error(res.msg)
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除此挑战？', '提示', { type: 'warning' })
  const res = await deleteChallenge(id)
  if (res.code === 200) { ElMessage.success('删除成功'); fetchData() }
}

async function toggleStatus(row: any) {
  const newStatus = row.status === 1 ? 0 : 1
  const res = await saveChallenge({ id: row.id, status: newStatus })
  if (res.code === 200) { ElMessage.success('操作成功'); fetchData() }
}

onMounted(() => { fetchData(); fetchSubjects() })
</script>
