<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <span style="font-size:15px;font-weight:600">年级管理</span>
      <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B" @click="openDialog()">新增年级</el-button>
    </div>

    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="levelCode" label="年级编码" width="100" />
      <el-table-column prop="levelName" label="年级名称" />
      <el-table-column prop="ageGroup" label="年龄段" width="120">
        <template #default="{ row }">
          <el-tag :type="ageGroupTag(row.ageGroup)" size="small">{{ ageGroupLabel(row.ageGroup) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="年龄范围" width="120">
        <template #default="{ row }">{{ row.minAge }}-{{ row.maxAge }}岁</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑年级' : '新增年级'" width="500">
      <el-form :model="form" label-width="80px">
        <el-form-item label="年级编码">
          <el-input-number v-model="form.levelCode" :min="1" :max="12" style="width:100%" />
        </el-form-item>
        <el-form-item label="年级名称">
          <el-input v-model="form.levelName" placeholder="如：小班、一年级" />
        </el-form-item>
        <el-form-item label="年龄段">
          <el-select v-model="form.ageGroup" style="width:100%">
            <el-option :value="1" label="幼幼组 (3-6岁)" />
            <el-option :value="2" label="低龄组 (6-9岁)" />
            <el-option :value="3" label="高龄组 (9-12岁)" />
          </el-select>
        </el-form-item>
        <el-form-item label="最小年龄">
          <el-input-number v-model="form.minAge" :min="2" :max="15" style="width:100%" />
        </el-form-item>
        <el-form-item label="最大年龄">
          <el-input-number v-model="form.maxAge" :min="2" :max="18" style="width:100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getGradeLevelList, saveGradeLevel, deleteGradeLevel } from '@/api/request'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  levelCode: 1, levelName: '', ageGroup: 1, minAge: 3, maxAge: 6, status: 1
})

function ageGroupLabel(ag: number) {
  return { 1: '幼幼组', 2: '低龄组', 3: '高龄组' }[ag] || '未知'
}
function ageGroupTag(ag: number) {
  return ({ 1: 'success', 2: 'warning', 3: 'danger' } as any)[ag] || 'info'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getGradeLevelList()
    if (res.code === 200) tableData.value = res.data
  } finally { loading.value = false }
}

function openDialog(row?: any) {
  if (row) {
    editingId.value = row.id
    Object.assign(form, {
      levelCode: row.levelCode, levelName: row.levelName,
      ageGroup: row.ageGroup, minAge: row.minAge, maxAge: row.maxAge, status: row.status
    })
  } else {
    editingId.value = null
    Object.assign(form, { levelCode: 1, levelName: '', ageGroup: 1, minAge: 3, maxAge: 6, status: 1 })
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.levelName) { ElMessage.warning('请输入年级名称'); return }
  saving.value = true
  try {
    const res = await saveGradeLevel({ ...form, id: editingId.value })
    if (res.code === 200) { ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() }
    else ElMessage.error(res.msg)
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('删除年级将解除与课程的关联，确认删除？', '提示', { type: 'warning' })
  const res = await deleteGradeLevel(id)
  if (res.code === 200) { ElMessage.success('删除成功'); fetchData() }
  else ElMessage.error(res.msg)
}

onMounted(() => fetchData())
</script>
