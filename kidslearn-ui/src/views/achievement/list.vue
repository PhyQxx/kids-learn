<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">成就管理</span>
        <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B" @click="openDialog()">新增成就</el-button>
      </div>
    </template>
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="achieveCode" label="成就代码" />
      <el-table-column prop="achieveName" label="成就名称" />
      <el-table-column prop="achieveType" label="类型" width="80">
        <template #default="{ row }">{{ ['', '学习', '收集', '社交', '时长', '特殊'][row.achieveType] }}</template>
      </el-table-column>
      <el-table-column prop="isTiered" label="分级" width="80">
        <template #default="{ row }"><el-tag :type="row.isTiered ? 'success' : 'info'" size="small">{{ row.isTiered ? '是' : '否' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="70" />
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑成就' : '新增成就'" width="600">
      <el-form :model="form" label-width="80px">
        <el-form-item label="成就代码"><el-input v-model="form.achieveCode" /></el-form-item>
        <el-form-item label="成就名称"><el-input v-model="form.achieveName" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.achieveDesc" /></el-form-item>
        <el-form-item label="图标URL"><el-input v-model="form.achieveIcon" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.achieveType">
            <el-option label="学习" :value="1" /><el-option label="收集" :value="2" /><el-option label="社交" :value="3" /><el-option label="时长" :value="4" /><el-option label="特殊" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="分级"><el-switch v-model="form.isTiered" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
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
import { getAchievementList, saveAchievement, deleteAchievement } from '@/api/request'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  achieveCode: '', achieveName: '', achieveDesc: '', achieveIcon: '',
  achieveType: 1, isTiered: 0, sortOrder: 0, status: 1
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getAchievementList({ page: currentPage.value, pageSize: pageSize.value })
    if (res.code === 200) { tableData.value = res.data.list; total.value = res.data.total }
  } finally { loading.value = false }
}

function openDialog(row?: any) {
  if (row) { editingId.value = row.id; Object.assign(form, row) }
  else { editingId.value = null; Object.assign(form, { achieveCode: '', achieveName: '', achieveDesc: '', achieveIcon: '', achieveType: 1, isTiered: 0, sortOrder: 0, status: 1 }) }
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    const res = await saveAchievement({ ...form, id: editingId.value })
    if (res.code === 200) { ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() }
    else ElMessage.error(res.msg)
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  const res = await deleteAchievement(id)
  if (res.code === 200) { ElMessage.success('删除成功'); fetchData() }
}

onMounted(() => fetchData())
</script>
