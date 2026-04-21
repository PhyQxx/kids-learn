<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">题目管理</span>
        <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B" @click="openDialog()">新增题目</el-button>
      </div>
    </template>
    <div style="margin-bottom:16px;display:flex;gap:12px">
      <el-select v-model="filterLevelId" placeholder="筛选关卡" clearable @change="fetchData" style="width:200px">
        <el-option v-for="l in levels" :key="l.id" :label="l.levelName" :value="l.id" />
      </el-select>
      <el-select v-model="filterType" placeholder="题型" clearable @change="fetchData" style="width:120px">
        <el-option label="选择题" :value="1" /><el-option label="判断题" :value="2" /><el-option label="填空题" :value="3" />
      </el-select>
    </div>
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="questionType" label="题型" width="80">
        <template #default="{ row }">{{ ['', '选择', '判断', '填空', '连线', '拖拽'][row.questionType] }}</template>
      </el-table-column>
      <el-table-column prop="questionContent" label="题目内容" show-overflow-tooltip />
      <el-table-column prop="difficulty" label="难度" width="80">
        <template #default="{ row }">{{ ['', '简单', '普通', '困难'][row.difficulty] }}</template>
      </el-table-column>
      <el-table-column prop="score" label="分值" width="70" />
      <el-table-column prop="timeLimit" label="限时(秒)" width="90" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-if="total > 0" style="margin-top:16px;justify-content:flex-end" :total="total" :page-size="pageSize"
      v-model:current-page="currentPage" layout="total, prev, pager, next" @current-change="fetchData" />

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑题目' : '新增题目'" width="700">
      <el-form :model="form" label-width="80px">
        <el-form-item label="所属关卡">
          <el-select v-model="form.courseLevelId" style="width:100%">
            <el-option v-for="l in levels" :key="l.id" :label="l.levelName" :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型">
          <el-radio-group v-model="form.questionType">
            <el-radio :value="1">选择</el-radio><el-radio :value="2">判断</el-radio><el-radio :value="3">填空</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题目内容"><el-input v-model="form.questionContent" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="选项" v-if="form.questionType === 1">
          <div v-for="(opt, i) in form.options" :key="i" style="display:flex;gap:8px;margin-bottom:8px">
            <el-input v-model="opt.optionLabel" style="width:60px" placeholder="A" />
            <el-input v-model="opt.optionContent" placeholder="选项内容" />
            <el-switch v-model="opt.isCorrect" :active-value="1" :inactive-value="0" active-text="正确" />
            <el-button link type="danger" @click="form.options.splice(i, 1)">删除</el-button>
          </div>
          <el-button @click="form.options.push({ optionLabel: String.fromCharCode(65 + form.options.length), optionContent: '', isCorrect: 0, sortOrder: form.options.length })">+ 添加选项</el-button>
        </el-form-item>
        <el-form-item label="正确答案" v-if="form.questionType === 3"><el-input v-model="form.correctAnswer" /></el-form-item>
        <el-form-item label="解析"><el-input v-model="form.analysis" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="难度">
          <el-radio-group v-model="form.difficulty">
            <el-radio :value="1">简单</el-radio><el-radio :value="2">普通</el-radio><el-radio :value="3">困难</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分值"><el-input-number v-model="form.score" :min="1" /></el-form-item>
        <el-form-item label="限时(秒)"><el-input-number v-model="form.timeLimit" :min="0" /></el-form-item>
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
import { getQuestionList, saveQuestion, deleteQuestion, getLevelList } from '@/api/request'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const levels = ref<any[]>([])
const filterLevelId = ref<number | ''>('')
const filterType = ref<number | ''>('')
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const defaultOptions = [
  { optionLabel: 'A', optionContent: '', isCorrect: 1, sortOrder: 0 },
  { optionLabel: 'B', optionContent: '', isCorrect: 0, sortOrder: 1 },
  { optionLabel: 'C', optionContent: '', isCorrect: 0, sortOrder: 2 },
  { optionLabel: 'D', optionContent: '', isCorrect: 0, sortOrder: 3 },
]

const form = reactive({
  courseLevelId: null as number | null, questionType: 1, questionContent: '',
  difficulty: 1, score: 10, timeLimit: 0, analysis: '', sortOrder: 0,
  correctAnswer: '', options: JSON.parse(JSON.stringify(defaultOptions))
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getQuestionList({
      page: currentPage.value, pageSize: pageSize.value,
      courseLevelId: filterLevelId.value || undefined,
      questionType: filterType.value || undefined
    })
    if (res.code === 200) { tableData.value = res.data.list; total.value = res.data.total }
  } finally { loading.value = false }
}

async function fetchLevels() {
  const res = await getLevelList({ page: 1, pageSize: 200 })
  if (res.code === 200) levels.value = res.data.list
}

function openDialog(row?: any) {
  if (row) {
    editingId.value = row.id
    Object.assign(form, { ...row, options: JSON.parse(JSON.stringify(defaultOptions)) })
  } else {
    editingId.value = null
    Object.assign(form, { courseLevelId: null, questionType: 1, questionContent: '', difficulty: 1, score: 10, timeLimit: 0, analysis: '', sortOrder: 0, correctAnswer: '', options: JSON.parse(JSON.stringify(defaultOptions)) })
  }
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    const res = await saveQuestion({ ...form, id: editingId.value })
    if (res.code === 200) { ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() }
    else ElMessage.error(res.msg)
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  const res = await deleteQuestion(id)
  if (res.code === 200) { ElMessage.success('删除成功'); fetchData() }
}

onMounted(() => { fetchLevels(); fetchData() })
</script>
