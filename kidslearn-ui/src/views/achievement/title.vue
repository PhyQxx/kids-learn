<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">称号管理</span>
        <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B" @click="openDialog()">新增称号</el-button>
      </div>
    </template>
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="titleCode" label="称号代码" />
      <el-table-column prop="titleName" label="称号名称" />
      <el-table-column prop="titleColor" label="颜色" width="100">
        <template #default="{ row }"><span :style="{ color: row.titleColor, fontWeight: 'bold' }">{{ row.titleName }}</span></template>
      </el-table-column>
      <el-table-column prop="obtainType" label="获取方式" width="100">
        <template #default="{ row }">{{ ['', '成就解锁', '活动奖励', '手动发放'][row.obtainType] }}</template>
      </el-table-column>
      <el-table-column prop="isTimed" label="限时" width="80">
        <template #default="{ row }"><el-tag :type="row.isTimed ? 'warning' : 'info'" size="small">{{ row.isTimed ? '是' : '否' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag></template>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑称号' : '新增称号'" width="500">
      <el-form :model="form" label-width="80px">
        <el-form-item label="称号代码"><el-input v-model="form.titleCode" /></el-form-item>
        <el-form-item label="称号名称"><el-input v-model="form.titleName" /></el-form-item>
        <el-form-item label="展示颜色"><el-color-picker v-model="form.titleColor" /></el-form-item>
        <el-form-item label="图标URL"><el-input v-model="form.titleIcon" /></el-form-item>
        <el-form-item label="获取方式">
          <el-select v-model="form.obtainType"><el-option label="成就解锁" :value="1" /><el-option label="活动奖励" :value="2" /><el-option label="手动发放" :value="3" /></el-select>
        </el-form-item>
        <el-form-item label="限时"><el-switch v-model="form.isTimed" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
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
import { getTitleList, saveTitle, deleteTitle } from '@/api/request'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  titleCode: '', titleName: '', titleColor: '#FF6B6B', titleIcon: '',
  obtainType: 1, isTimed: 0, status: 1
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getTitleList({ page: currentPage.value, pageSize: pageSize.value })
    if (res.code === 200) { tableData.value = res.data.list; total.value = res.data.total }
  } finally { loading.value = false }
}

function openDialog(row?: any) {
  if (row) { editingId.value = row.id; Object.assign(form, row) }
  else { editingId.value = null; Object.assign(form, { titleCode: '', titleName: '', titleColor: '#FF6B6B', titleIcon: '', obtainType: 1, isTimed: 0, status: 1 }) }
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    const res = await saveTitle({ ...form, id: editingId.value })
    if (res.code === 200) { ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() }
    else ElMessage.error(res.msg)
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  const res = await deleteTitle(id)
  if (res.code === 200) { ElMessage.success('删除成功'); fetchData() }
}

onMounted(() => fetchData())
</script>
