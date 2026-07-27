<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <span style="font-size:15px;font-weight:600">专项练习管理 ({{ subject?.subjectName }})</span>
      <el-button type="primary" @click="openDialog()">新增练习</el-button>
    </div>

    <div ref="tableBox">
    <el-table :data="tableData" stripe v-loading="loading" :max-height="tableMaxHeight">
      <el-table-column prop="icon" label="图标" width="60">
        <template #default="{ row }">
          <span style="font-size: 24px;">{{ row.icon }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="练习名称" width="150" />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.type === 'TIMED' ? 'warning' : undefined">{{ row.type === 'TIMED' ? '限时挑战' : '无尽模式' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="timeLimitSeconds" label="限时(秒)" width="100" />
      <el-table-column prop="tags" label="标签" width="100" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '上线' : '下线' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-if="total > 0" style="margin-top:16px;justify-content:flex-end" :total="total" :page-size="pageSize"
      v-model:current-page="currentPage" layout="total, prev, pager, next" @current-change="fetchData" />
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑练习' : '新增练习'" width="500">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称"><el-input v-model="form.name" placeholder="如: 20以内加法" /></el-form-item>
        <el-form-item label="描述"><el-input type="textarea" v-model="form.description" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" placeholder="Emoji，如 ➕" /></el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio value="ENDLESS">无尽模式</el-radio>
            <el-radio value="TIMED">限时模式</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="限时(秒)" v-if="form.type === 'TIMED'">
          <el-input-number v-model="form.timeLimitSeconds" :min="0" />
        </el-form-item>
        <el-form-item label="标签"><el-input v-model="form.tags" placeholder="如 HOT" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPracticeModeList, savePracticeMode, deletePracticeMode } from '@/api/request'
import { useTableHeight } from '@/composables/useTableHeight'

const { tableBox, tableMaxHeight } = useTableHeight()

const props = defineProps<{
  subject: any
}>()

const loading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  name: '', description: '', icon: '', type: 'ENDLESS', timeLimitSeconds: 60, tags: '', sortOrder: 0, status: 1
})

watch(() => props.subject, () => {
  currentPage.value = 1
  fetchData()
})

async function fetchData() {
  if (!props.subject) return
  loading.value = true
  try {
    const res = await getPracticeModeList({
      page: currentPage.value,
      pageSize: pageSize.value,
      subjectId: props.subject.id
    })
    if (res.code === 200) {
      tableData.value = res.data.list
      total.value = res.data.total
    }
  } finally { loading.value = false }
}

function openDialog(row?: any) {
  if (row) {
    editingId.value = row.id
    Object.assign(form, row)
  } else {
    editingId.value = null
    Object.assign(form, { name: '', description: '', icon: '', type: 'ENDLESS', timeLimitSeconds: 60, tags: '', sortOrder: 0, status: 1 })
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!props.subject) return
  saving.value = true
  try {
    const data: any = { ...form, id: editingId.value, subjectId: props.subject.id }
    if (data.type !== 'TIMED') {
      data.timeLimitSeconds = null
    }
    const res = await savePracticeMode(data)
    if (res.code === 200) {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      fetchData()
    } else { ElMessage.error(res.msg) }
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除该练习模式？', '提示', { type: 'warning' })
  const res = await deletePracticeMode(id)
  if (res.code === 200) { ElMessage.success('删除成功'); fetchData() }
  else { ElMessage.error(res.msg) }
}

onMounted(() => { fetchData() })
</script>
