<template>
  <div>
    <div class="panel-header">
      <span class="panel-title">{{ courseLevel.levelName }} - 题目列表</span>
      <div class="header-actions">
        <el-button
          v-if="selectedRows.length > 0"
          type="success"
          :loading="batchGenerating"
          @click="handleBatchGenerateAudio"
        >
          🗣️ 批量生成语音 ({{ selectedRows.length }})
        </el-button>
        <el-button type="primary" class="primary-btn" @click="openDialog()">新增题目</el-button>
      </div>
    </div>

    <div ref="tableBox">
    <div class="filter-row">
      <el-select v-model="filterType" placeholder="题型" clearable @change="fetchData" style="width: 140px">
        <el-option label="选择题" :value="1" />
        <el-option label="判断题" :value="2" />
        <el-option label="填空题" :value="3" />
      </el-select>
    </div>

    <el-table :data="tableData" stripe v-loading="loading" :max-height="tableMaxHeight" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="questionType" label="题型" width="90">
        <template #default="{ row }">{{ questionTypeLabel(row.questionType) }}</template>
      </el-table-column>
      <el-table-column label="题目内容" min-width="260" show-overflow-tooltip>
        <template #default="{ row }">{{ richContentSummary(row.questionContent, 60) }}</template>
      </el-table-column>
      <el-table-column label="语音" width="90">
        <template #default="{ row }">
          <el-tag v-if="hasQuestionAudio(row.questionContent)" type="success" size="small">已生成</el-tag>
          <el-tag v-else type="info" size="small">未生成</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="difficulty" label="难度" width="90">
        <template #default="{ row }">{{ difficultyLabel(row.difficulty) }}</template>
      </el-table-column>
      <el-table-column prop="score" label="分值" width="80" />
      <el-table-column prop="timeLimit" label="限时(秒)" width="100" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑题目' : '新增题目'" top="1vh" width="60vw">
      <el-form :model="form" label-width="90px">
        <el-form-item label="所属关卡">
          <el-select v-model="form.courseLevelId" style="width: 100%" disabled>
            <el-option :label="courseLevel.levelName" :value="courseLevel.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="题型">
          <el-radio-group v-model="form.questionType">
            <el-radio :value="1">选择题</el-radio>
            <el-radio :value="2">判断题</el-radio>
            <el-radio :value="3">填空题</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="题目内容">
          <RichContentEditor v-model="form.questionContent" />
        </el-form-item>

        <el-form-item label="题目语音">
          <div class="audio-panel">
            <el-input
              v-model="form.speechText"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 4 }"
              placeholder="用于生成语音的朗读文本"
              @input="syncSpeechToContent"
            />
            <div class="audio-actions">
              <el-button @click="syncSpeechTextFromContent">同步题目文字</el-button>
              <el-button
                type="primary"
                :disabled="!editingId || !form.speechText.trim()"
                :loading="generatingAudio"
                @click="handleGenerateAudio"
              >
                生成/更新音频
              </el-button>
              <el-tag v-if="speechNeedsUpdate" type="warning">文字已变更，建议重新生成</el-tag>
            </div>
            <audio v-if="form.audioUrl" :src="form.audioUrl" controls class="audio-player" />
            <el-input v-if="form.audioUrl" v-model="form.audioUrl" readonly />
            <el-text v-if="!editingId" type="info" size="small">新题目请先保存后再生成音频</el-text>
          </div>
        </el-form-item>

        <el-form-item label="选项" v-if="form.questionType === 1">
          <div class="option-list">
            <div v-for="(opt, index) in form.options" :key="index" class="option-row">
              <el-input v-model="opt.optionLabel" class="option-label" placeholder="A" />
              <RichContentEditor v-model="opt.optionContent" class="option-editor" compact />
              <el-switch
                v-model="opt.isCorrect"
                :active-value="1"
                :inactive-value="0"
                active-text="正确"
              />
              <el-button link type="danger" @click="removeOption(index)">删除</el-button>
            </div>
            <el-button @click="addOption">添加选项</el-button>
          </div>
        </el-form-item>

        <el-form-item label="正确答案" v-if="form.questionType === 3">
          <el-input v-model="form.correctAnswer" />
        </el-form-item>

        <el-form-item label="解析">
          <RichContentEditor v-model="form.analysis" />
        </el-form-item>

        <el-form-item label="难度">
          <el-radio-group v-model="form.difficulty">
            <el-radio :value="1">简单</el-radio>
            <el-radio :value="2">普通</el-radio>
            <el-radio :value="3">困难</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="分值">
          <el-input-number v-model="form.score" :min="1" />
        </el-form-item>

        <el-form-item label="限时(秒)">
          <el-input-number v-model="form.timeLimit" :min="0" />
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
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import RichContentEditor from '@/components/RichContentEditor.vue'
import { deleteQuestion, generateQuestionAudio, getQuestionList, getQuestionOptions, saveQuestion } from '@/api/request'
import { useTableHeight } from '@/composables/useTableHeight'

const { tableBox, tableMaxHeight } = useTableHeight()
import {
  richContentSpeech,
  richContentSummary,
  richContentToSpeechText,
  withRichContentSpeech,
} from '@/utils/richContent'

type QuestionOptionForm = {
  optionLabel: string
  optionContent: string
  isCorrect: number
  sortOrder: number
}

const props = defineProps<{
  courseLevel: any
}>()

const loading = ref(false)
const saving = ref(false)
const generatingAudio = ref(false)
const batchGenerating = ref(false)
const selectedRows = ref<any[]>([])
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const filterType = ref<number | ''>('')
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  courseLevelId: null as number | null,
  questionType: 1,
  questionContent: '',
  difficulty: 1,
  score: 10,
  timeLimit: 0,
  analysis: '',
  sortOrder: 0,
  correctAnswer: '',
  speechText: '',
  audioUrl: '',
  savedSpeechText: '',
  options: defaultOptions(),
})

const speechNeedsUpdate = computed(() => (
  Boolean(form.audioUrl && form.speechText && form.savedSpeechText && form.speechText !== form.savedSpeechText)
))

function defaultOptions(): QuestionOptionForm[] {
  return [
    { optionLabel: 'A', optionContent: '', isCorrect: 1, sortOrder: 0 },
    { optionLabel: 'B', optionContent: '', isCorrect: 0, sortOrder: 1 },
    { optionLabel: 'C', optionContent: '', isCorrect: 0, sortOrder: 2 },
    { optionLabel: 'D', optionContent: '', isCorrect: 0, sortOrder: 3 },
  ]
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getQuestionList({
      page: currentPage.value,
      pageSize: pageSize.value,
      courseLevelId: props.courseLevel.id,
      questionType: filterType.value || undefined,
    })
    if (res.code === 200) {
      tableData.value = res.data.list
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

async function openDialog(row?: any) {
  if (row) {
    editingId.value = row.id
    let existingOptions = defaultOptions()
    try {
      const optRes = await getQuestionOptions(row.id)
      if (optRes.code === 200 && Array.isArray(optRes.data) && optRes.data.length > 0) {
        existingOptions = optRes.data
      }
    } catch {
      existingOptions = defaultOptions()
    }
    Object.assign(form, { ...row, options: existingOptions })
    loadSpeechFields(row.questionContent)
  } else {
    editingId.value = null
    Object.assign(form, {
      courseLevelId: props.courseLevel.id,
      questionType: 1,
      questionContent: '',
      difficulty: 1,
      score: 10,
      timeLimit: 0,
      analysis: '',
      sortOrder: 0,
      correctAnswer: '',
      speechText: '',
      audioUrl: '',
      savedSpeechText: '',
      options: defaultOptions(),
    })
  }
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    syncSpeechToContent()
    form.options.forEach((option, index) => {
      option.sortOrder = index
    })
    const res = await saveQuestion({ ...form, id: editingId.value })
    if (res.code === 200) {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res.msg)
    }
  } finally {
    saving.value = false
  }
}

async function handleGenerateAudio() {
  if (!editingId.value) {
    ElMessage.warning('请先保存题目后再生成音频')
    return
  }

  const speechText = (form.speechText || richContentToSpeechText(form.questionContent)).trim()
  if (!speechText) {
    ElMessage.warning('朗读文本不能为空')
    return
  }

  generatingAudio.value = true
  try {
    const res = await generateQuestionAudio(editingId.value, { speechText })
    if (res.code === 200) {
      form.speechText = res.data?.speechText || speechText
      form.audioUrl = res.data?.audioUrl || ''
      form.savedSpeechText = form.speechText
      syncSpeechToContent()
      ElMessage.success('音频生成成功')
      fetchData()
    } else {
      ElMessage.error(res.msg)
    }
  } finally {
    generatingAudio.value = false
  }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  const res = await deleteQuestion(id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    fetchData()
  }
}

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

async function handleBatchGenerateAudio() {
  const targets = selectedRows.value.filter(row => row.id)
  if (targets.length === 0) {
    ElMessage.warning('请先选择题目')
    return
  }

  await ElMessageBox.confirm(
    `确认为 ${targets.length} 道题目生成语音？已有语音的题目将跳过。`,
    '批量生成语音',
    { type: 'info', confirmButtonText: '开始生成' }
  )

  batchGenerating.value = true
  let success = 0
  let skipped = 0
  let failed = 0

  try {
    for (let i = 0; i < targets.length; i++) {
      const row = targets[i]
      // 已有语音的跳过
      if (hasQuestionAudio(row.questionContent)) {
        skipped++
        continue
      }
      try {
        const res = await generateQuestionAudio(row.id, {})
        if (res.code === 200) {
          success++
          // 更新本地数据
          row.questionContent = withRichContentSpeech(row.questionContent, {
            text: res.data?.speechText || '',
            audioUrl: res.data?.audioUrl || '',
          })
        } else {
          failed++
        }
      } catch {
        failed++
      }
    }

    const msg = [`完成！成功 ${success}`]
    if (skipped > 0) msg.push(`跳过 ${skipped}`)
    if (failed > 0) msg.push(`失败 ${failed}`)
    ElMessage.success(msg.join('，'))
    fetchData()
  } finally {
    batchGenerating.value = false
  }
}

function addOption() {
  form.options.push({
    optionLabel: String.fromCharCode(65 + form.options.length),
    optionContent: '',
    isCorrect: 0,
    sortOrder: form.options.length,
  })
}

function removeOption(index: number) {
  form.options.splice(index, 1)
}

function loadSpeechFields(questionContent?: string) {
  const speech = richContentSpeech(questionContent)
  form.speechText = speech.text || richContentToSpeechText(questionContent)
  form.audioUrl = speech.audioUrl
  form.savedSpeechText = form.speechText
  syncSpeechToContent()
}

function syncSpeechTextFromContent() {
  form.speechText = richContentToSpeechText(form.questionContent)
  syncSpeechToContent()
}

function syncSpeechToContent() {
  form.questionContent = withRichContentSpeech(form.questionContent, {
    text: form.speechText,
    audioUrl: form.audioUrl,
  })
}

function hasQuestionAudio(questionContent?: string) {
  return Boolean(richContentSpeech(questionContent).audioUrl)
}

function questionTypeLabel(type: number) {
  return ['', '选择题', '判断题', '填空题', '连线题', '拖拽题'][type] || '未知'
}

function difficultyLabel(difficulty: number) {
  return ['', '简单', '普通', '困难'][difficulty] || '未知'
}

onMounted(() => fetchData())
</script>

<style scoped>
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.primary-btn {
  background: #ff6b6b;
  border-color: #ff6b6b;
}

.filter-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.option-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.option-row {
  display: grid;
  grid-template-columns: 60px minmax(0, 1fr) 120px 48px;
  gap: 8px;
  align-items: flex-start;
}

.option-label {
  width: 60px;
}

.option-editor {
  min-width: 0;
}

.audio-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.audio-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.audio-player {
  width: 100%;
}
</style>
