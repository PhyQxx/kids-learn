<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">关卡管理</span>
        <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B" @click="openDialog()">新增关卡</el-button>
      </div>
    </template>
    <div style="margin-bottom:16px">
      <el-select v-model="filterCourseId" placeholder="筛选课程" clearable @change="fetchData" style="width:200px">
        <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id" />
      </el-select>
    </div>
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="levelNum" label="序号" width="80" />
      <el-table-column prop="levelName" label="关卡名称" />
      <el-table-column prop="totalQuestions" label="题目数" width="80" />
      <el-table-column prop="passScore" label="及格分" width="80" />
      <el-table-column prop="starThresholds" label="星级门槛" />
      <el-table-column prop="expReward" label="经验" width="70" />
      <el-table-column prop="goldReward" label="金币" width="70" />
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑关卡' : '新增关卡'" width="600">
      <el-form :model="form" label-width="80px">
        <el-form-item label="所属课程">
          <el-select v-model="form.courseId" style="width:100%">
            <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关卡序号"><el-input-number v-model="form.levelNum" :min="1" /></el-form-item>
        <el-form-item label="关卡名称"><el-input v-model="form.levelName" /></el-form-item>
        <el-form-item label="关卡描述"><el-input v-model="form.levelDesc" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="及格分"><el-input-number v-model="form.passScore" :min="0" :max="100" /></el-form-item>
        <el-form-item label="星级门槛"><el-input v-model="form.starThresholds" placeholder="如 60,80,100" /></el-form-item>
        <el-form-item label="经验奖励"><el-input-number v-model="form.expReward" :min="0" /></el-form-item>
        <el-form-item label="金币奖励"><el-input-number v-model="form.goldReward" :min="0" /></el-form-item>
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
import { getLevelList, saveLevel, deleteLevel, getCourseList } from '@/api/request'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const courses = ref<any[]>([])
const filterCourseId = ref<number | ''>('')
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  courseId: null as number | null, levelNum: 1, levelName: '', levelDesc: '',
  passScore: 60, starThresholds: '60,80,100', expReward: 10, goldReward: 10, status: 1
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getLevelList({ page: currentPage.value, pageSize: pageSize.value, courseId: filterCourseId.value || undefined })
    if (res.code === 200) { tableData.value = res.data.list; total.value = res.data.total }
  } finally { loading.value = false }
}

async function fetchCourses() {
  const res = await getCourseList({ page: 1, pageSize: 200 })
  if (res.code === 200) courses.value = res.data.list
}

function openDialog(row?: any) {
  if (row) { editingId.value = row.id; Object.assign(form, row) }
  else { editingId.value = null; Object.assign(form, { courseId: null, levelNum: 1, levelName: '', levelDesc: '', passScore: 60, starThresholds: '60,80,100', expReward: 10, goldReward: 10, status: 1 }) }
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    const res = await saveLevel({ ...form, id: editingId.value })
    if (res.code === 200) { ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() }
    else ElMessage.error(res.msg)
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  const res = await deleteLevel(id)
  if (res.code === 200) { ElMessage.success('删除成功'); fetchData() }
}

onMounted(() => { fetchCourses(); fetchData() })
</script>
