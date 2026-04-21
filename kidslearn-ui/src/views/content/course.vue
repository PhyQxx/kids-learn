<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">课程管理</span>
        <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B" @click="openDialog()">新增课程</el-button>
      </div>
    </template>
    <div style="margin-bottom:16px;display:flex;gap:12px">
      <el-select v-model="filterSubjectId" placeholder="筛选学科" clearable @change="fetchData" style="width:160px">
        <el-option v-for="s in subjects" :key="s.id" :label="s.subjectName" :value="s.id" />
      </el-select>
    </div>
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="courseName" label="课程名称" />
      <el-table-column prop="subjectId" label="学科" width="100">
        <template #default="{ row }">{{ getSubjectName(row.subjectId) }}</template>
      </el-table-column>
      <el-table-column prop="difficulty" label="难度" width="80">
        <template #default="{ row }">{{ ['', '简单', '普通', '困难'][row.difficulty] }}</template>
      </el-table-column>
      <el-table-column prop="totalLevels" label="关卡数" width="80" />
      <el-table-column prop="isElite" label="精英关" width="80">
        <template #default="{ row }"><el-tag :type="row.isElite ? 'warning' : 'info'" size="small">{{ row.isElite ? '是' : '否' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-if="total > 0" style="margin-top:16px;justify-content:flex-end" :total="total" :page-size="pageSize"
      v-model:current-page="currentPage" layout="total, prev, pager, next" @current-change="fetchData" />

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑课程' : '新增课程'" width="600">
      <el-form :model="form" label-width="80px">
        <el-form-item label="课程名称"><el-input v-model="form.courseName" /></el-form-item>
        <el-form-item label="学科">
          <el-select v-model="form.subjectId" style="width:100%">
            <el-option v-for="s in subjects" :key="s.id" :label="s.subjectName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程描述"><el-input v-model="form.courseDesc" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="封面图"><el-input v-model="form.coverUrl" placeholder="封面图URL" /></el-form-item>
        <el-form-item label="难度">
          <el-radio-group v-model="form.difficulty">
            <el-radio :value="1">简单</el-radio><el-radio :value="2">普通</el-radio><el-radio :value="3">困难</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="精英关"><el-switch v-model="form.isElite" :active-value="1" :inactive-value="0" /></el-form-item>
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
import { getCourseList, saveCourse, deleteCourse, getSubjectList } from '@/api/request'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const subjects = ref<any[]>([])
const filterSubjectId = ref<number | ''>('')
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  courseName: '', subjectId: null as number | null, courseDesc: '', coverUrl: '',
  difficulty: 1, isElite: 0, sortOrder: 0, status: 1
})

function getSubjectName(id: number) {
  const s = subjects.value.find(s => s.id === id)
  return s ? s.subjectName : '-'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getCourseList({ page: currentPage.value, pageSize: pageSize.value, subjectId: filterSubjectId.value || undefined })
    if (res.code === 200) { tableData.value = res.data.list; total.value = res.data.total }
  } finally { loading.value = false }
}

async function fetchSubjects() {
  const res = await getSubjectList({ page: 1, pageSize: 100 })
  if (res.code === 200) subjects.value = res.data.list
}

function openDialog(row?: any) {
  if (row) { editingId.value = row.id; Object.assign(form, row) }
  else { editingId.value = null; Object.assign(form, { courseName: '', subjectId: null, courseDesc: '', coverUrl: '', difficulty: 1, isElite: 0, sortOrder: 0, status: 1 }) }
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    const res = await saveCourse({ ...form, id: editingId.value })
    if (res.code === 200) { ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() }
    else ElMessage.error(res.msg)
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  const res = await deleteCourse(id)
  if (res.code === 200) { ElMessage.success('删除成功'); fetchData() }
  else ElMessage.error(res.msg)
}

onMounted(() => { fetchSubjects(); fetchData() })
</script>
