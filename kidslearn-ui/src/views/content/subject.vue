<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">学科管理</span>
        <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B" @click="openDialog()">新增学科</el-button>
      </div>
    </template>
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="subjectCode" label="学科代码" />
      <el-table-column prop="subjectName" label="学科名称" />
      <el-table-column prop="iconUrl" label="图标URL" show-overflow-tooltip />
      <el-table-column prop="color" label="主题色">
        <template #default="{ row }">
          <span v-if="row.color" :style="{ color: row.color, fontWeight: 'bold' }">{{ row.color }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-if="total > 0" style="margin-top:16px;justify-content:flex-end" :total="total" :page-size="pageSize"
      v-model:current-page="currentPage" layout="total, prev, pager, next" @current-change="fetchData" />

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑学科' : '新增学科'" width="500">
      <el-form :model="form" label-width="80px">
        <el-form-item label="学科代码"><el-input v-model="form.subjectCode" placeholder="如 CHINESE" /></el-form-item>
        <el-form-item label="学科名称"><el-input v-model="form.subjectName" placeholder="如 语文" /></el-form-item>
        <el-form-item label="图标URL"><el-input v-model="form.iconUrl" /></el-form-item>
        <el-form-item label="主题色">
          <el-color-picker v-model="form.color" />
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSubjectList, saveSubject, deleteSubject } from '@/api/request'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  subjectCode: '', subjectName: '', iconUrl: '', color: '#FF6B6B', sortOrder: 0, status: 1
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getSubjectList({ page: currentPage.value, pageSize: pageSize.value })
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
    Object.assign(form, { subjectCode: '', subjectName: '', iconUrl: '', color: '#FF6B6B', sortOrder: 0, status: 1 })
  }
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    const data = { ...form, id: editingId.value }
    const res = await saveSubject(data)
    if (res.code === 200) {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      fetchData()
    } else { ElMessage.error(res.msg) }
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除该学科？', '提示', { type: 'warning' })
  const res = await deleteSubject(id)
  if (res.code === 200) { ElMessage.success('删除成功'); fetchData() }
  else { ElMessage.error(res.msg) }
}

onMounted(() => fetchData())
</script>
