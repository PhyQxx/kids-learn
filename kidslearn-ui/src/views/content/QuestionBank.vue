<template>
  <el-card>
    <template #header>
      <div style="display:flex;align-items:center;justify-content:space-between">
        <span style="font-size:16px;font-weight:600">题库管理</span>
        <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B" @click="openDialog()">新增题目</el-button>
      </div>
    </template>

    <div class="filter-row">
      <el-select v-model="filterSubject" placeholder="学科" clearable @change="fetchData" style="width: 140px">
        <el-option v-for="sub in subjects" :key="sub.id" :label="sub.subjectName" :value="sub.id" />
      </el-select>
      <el-select v-model="filterGrade" placeholder="年级" clearable @change="fetchData" style="width: 140px">
        <el-option v-for="g in grades" :key="g.id" :label="g.levelName" :value="g.id" />
      </el-select>
      <el-select v-model="filterType" placeholder="题型" clearable @change="fetchData" style="width: 140px">
        <el-option label="选择题" :value="1" />
        <el-option label="判断题" :value="2" />
        <el-option label="填空题" :value="3" />
        <el-option label="排序题" :value="4" />
        <el-option label="连线题" :value="5" />
      </el-select>
    </div>

    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="subjectId" label="学科" width="100">
        <template #default="{ row }">{{ subjectLabel(row.subjectId) }}</template>
      </el-table-column>
      <el-table-column prop="gradeLevelId" label="年级" width="120">
        <template #default="{ row }">{{ gradeLabel(row.gradeLevelId) }}</template>
      </el-table-column>
      <el-table-column prop="questionType" label="题型" width="90">
        <template #default="{ row }">
          <el-tag size="small">{{ questionTypeLabel(row.questionType) }}</el-tag>
        </template>
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
      <el-table-column prop="score" label="分值" width="80" />
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑题目' : '新增题目'" width="860px">
      <div class="ai-tools">
        <el-input
          v-model="aiKnowledgePoint"
          placeholder="输入知识点或出题要求，如：10以内加法、认识颜色"
          clearable
        />
        <el-button type="primary" plain :loading="aiGenerating" @click="handleAiGenerate">AI生成题目</el-button>
        <el-button plain :loading="aiAnalysisLoading" @click="handleAiAnalysis">AI补解析</el-button>
      </div>

      <el-form :model="form" label-width="90px">

        <el-form-item label="所属学科">
          <el-select v-model="form.subjectId" style="width: 100%">
            <el-option v-for="sub in subjects" :key="sub.id" :label="sub.subjectName" :value="sub.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="所属年级">
          <el-select v-model="form.gradeLevelId" style="width: 100%">
            <el-option v-for="g in grades" :key="g.id" :label="g.levelName" :value="g.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="题型">
          <el-radio-group v-model="form.questionType" @change="onQuestionTypeChange">
            <el-radio :value="1">选择题</el-radio>
            <el-radio :value="2">判断题</el-radio>
            <el-radio :value="3">填空题</el-radio>
            <el-radio :value="4">排序题</el-radio>
            <el-radio :value="5">连线题</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="题目内容">
          <RichContentEditor v-model="form.questionContent" />
          <div v-if="form.questionType === 3" style="font-size:12px;color:#999;margin-top:4px">
            提示：填空题可在上方内容中使用 ____ 或 [ ] 表示需要填空的位置。
          </div>
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

        <!-- 选项配置区域：根据题型动态变化 -->
        <el-form-item label="选项配置" class="dynamic-options-section">

          <!-- 1: 选择题 -->
          <div v-if="form.questionType === 1" class="option-list">
            <div v-for="(opt, index) in form.options" :key="index" class="option-row choice-row">
              <el-input v-model="opt.optionLabel" class="option-label" placeholder="标签(A/B/C)" />
              <RichContentEditor v-model="opt.optionContent" class="option-editor" compact />
              <el-switch v-model="opt.isCorrect" :active-value="1" :inactive-value="0" active-text="正确答案" />
              <el-button link type="danger" @click="removeOption(index)">删除</el-button>
            </div>
            <el-button @click="addOption(1)" style="align-self:flex-start">添加选项</el-button>
          </div>

          <!-- 2: 判断题 -->
          <div v-if="form.questionType === 2" class="option-list">
            <div v-for="(opt, index) in form.options" :key="index" class="option-row tf-row">
              <el-input v-model="opt.optionContent" readonly style="width: 120px;" />
              <el-radio :model-value="getTrueFalseCorrectIndex()" :label="index" @change="setTrueFalseCorrect(index)">正确答案</el-radio>
            </div>
            <div style="font-size:12px;color:#999;margin-top:4px">判断题固定为对/错两项，选择哪一项是正确答案即可。</div>
          </div>

          <!-- 3: 填空题 -->
          <div v-if="form.questionType === 3" class="option-list">
            <div v-for="(opt, index) in form.options" :key="index" class="option-row blank-row">
              <el-input v-model="opt.optionContent" placeholder="输入可接受的正确答案文本" />
              <el-button link type="danger" @click="removeOption(index)">删除</el-button>
            </div>
            <el-button @click="addOption(3)" style="align-self:flex-start">添加兼容答案</el-button>
            <div style="font-size:12px;color:#999;margin-top:4px">如果一个填空有多种回答（如“苹果”和“apple”），可添加多条。</div>
          </div>

          <!-- 4: 排序题 -->
          <div v-if="form.questionType === 4" class="option-list">
            <div v-for="(opt, index) in form.options" :key="index" class="option-row order-row">
              <div class="order-handle">顺序 {{ index + 1 }}</div>
              <RichContentEditor v-model="opt.optionContent" class="option-editor" compact />
              <div style="display:flex;flex-direction:column;gap:4px">
                <el-button size="small" :disabled="index === 0" @click="moveOption(index, -1)">上移</el-button>
                <el-button size="small" :disabled="index === form.options.length - 1" @click="moveOption(index, 1)">下移</el-button>
              </div>
              <el-button link type="danger" @click="removeOption(index)">删除</el-button>
            </div>
            <el-button @click="addOption(4)" style="align-self:flex-start">添加排序项</el-button>
            <div style="font-size:12px;color:#999;margin-top:4px">请按照【正确的最终顺序】录入列表，前端打乱后要求孩子还原此顺序。</div>
          </div>

          <!-- 5: 连线题 -->
          <div v-if="form.questionType === 5" class="option-list">
            <div v-for="(opt, index) in form.options" :key="index" class="option-row match-row">
              <div style="flex:1">
                <div style="margin-bottom:4px;font-size:12px;color:#666">左侧元素 (纯文本)</div>
                <el-input v-model="opt.optionLabel" placeholder="如：Apple" />
              </div>
              <div style="width: 40px; text-align:center; color:#999;">连线</div>
              <div style="flex:1">
                <div style="margin-bottom:4px;font-size:12px;color:#666">右侧匹配元素 (富文本)</div>
                <RichContentEditor v-model="opt.optionContent" class="option-editor" compact />
              </div>
              <el-button link type="danger" @click="removeOption(index)" style="margin-top:24px">删除</el-button>
            </div>
            <el-button @click="addOption(5)" style="align-self:flex-start">添加配对</el-button>
            <div style="font-size:12px;color:#999;margin-top:4px">前端会自动将左侧和右侧各自打乱显示。</div>
          </div>

        </el-form-item>

        <el-form-item label="解析">
          <RichContentEditor v-model="form.analysis" />
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
  </el-card>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import RichContentEditor from '@/components/RichContentEditor.vue'
import {
  deleteQuestion,
  generateAiQuestionAnalysis,
  generateAiQuestionDraft,
  generateQuestionAudio,
  getQuestionList,
  getQuestionOptions,
  saveQuestion,
  getSubjectList,
  getGradeLevelList,
} from '@/api/request'
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

const loading = ref(false)
const saving = ref(false)
const generatingAudio = ref(false)
const aiGenerating = ref(false)
const aiAnalysisLoading = ref(false)
const tableData = ref<any[]>([])
const subjects = ref<any[]>([])
const grades = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

const filterSubject = ref<number | ''>('')
const filterGrade = ref<number | ''>('')
const filterType = ref<number | ''>('')

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const aiKnowledgePoint = ref('')

const form = reactive({
  subjectId: null as number | null,
  gradeLevelId: null as number | null,
  questionType: 1,
  questionContent: '',
  score: 10,
  timeLimit: 0,
  analysis: '',
  sortOrder: 0,
  speechText: '',
  audioUrl: '',
  savedSpeechText: '',
  options: [] as QuestionOptionForm[],
})

const speechNeedsUpdate = computed(() => (
  Boolean(form.audioUrl && form.speechText && form.savedSpeechText && form.speechText !== form.savedSpeechText)
))

function defaultOptionsForType(type: number): QuestionOptionForm[] {
  if (type === 1) {
    return [
      { optionLabel: 'A', optionContent: '', isCorrect: 1, sortOrder: 0 },
      { optionLabel: 'B', optionContent: '', isCorrect: 0, sortOrder: 1 },
      { optionLabel: 'C', optionContent: '', isCorrect: 0, sortOrder: 2 },
      { optionLabel: 'D', optionContent: '', isCorrect: 0, sortOrder: 3 },
    ]
  } else if (type === 2) {
    return [
      { optionLabel: '', optionContent: '正确', isCorrect: 1, sortOrder: 0 },
      { optionLabel: '', optionContent: '错误', isCorrect: 0, sortOrder: 1 },
    ]
  } else if (type === 3) {
    return [
      { optionLabel: '', optionContent: '', isCorrect: 1, sortOrder: 0 }
    ]
  } else if (type === 4) {
    return [
      { optionLabel: '', optionContent: '步骤 1', isCorrect: 1, sortOrder: 0 },
      { optionLabel: '', optionContent: '步骤 2', isCorrect: 1, sortOrder: 1 },
      { optionLabel: '', optionContent: '步骤 3', isCorrect: 1, sortOrder: 2 },
    ]
  } else if (type === 5) {
    return [
      { optionLabel: '左侧1', optionContent: '右侧1', isCorrect: 1, sortOrder: 0 },
      { optionLabel: '左侧2', optionContent: '右侧2', isCorrect: 1, sortOrder: 1 },
    ]
  }
  return []
}

function onQuestionTypeChange(newType: string | number | boolean | undefined) {
  if (typeof newType !== 'number') return
  // If switching types while creating/editing, we reset options to default for that type.
  // Warning: this overwrites current options.
  ElMessageBox.confirm('切换题型会重置下方的选项列表，确认切换吗？', '提示', { type: 'warning' })
    .then(() => {
      form.options = defaultOptionsForType(newType)
    })
    .catch(() => {
      // Revert type change if canceled. This requires knowing previous type, but simplified for now:
      // Actually el-radio-group v-model is already updated. It's tricky to revert cleanly without extra state.
      // So we just re-assign if they confirm.
    })
}

async function loadDictionaries() {
  getSubjectList({ page: 1, pageSize: 1000 }).then(res => {
    if (res.code === 200) subjects.value = res.data.list
  })
  getGradeLevelList().then(res => {
    if (res.code === 200) grades.value = res.data
  })
}

function subjectLabel(id: number) {
  const sub = subjects.value.find(s => s.id === id)
  return sub ? sub.subjectName : '未知'
}

function gradeLabel(id: number) {
  const g = grades.value.find(g => g.id === id)
  return g ? g.levelName : '未知'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getQuestionList({
      page: currentPage.value,
      pageSize: pageSize.value,
      subjectId: filterSubject.value || undefined,
      gradeLevelId: filterGrade.value || undefined,
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
    let existingOptions: QuestionOptionForm[] = []
    try {
      const optRes = await getQuestionOptions(row.id)
      if (optRes.code === 200 && Array.isArray(optRes.data) && optRes.data.length > 0) {
        existingOptions = optRes.data
      } else {
        existingOptions = defaultOptionsForType(row.questionType)
      }
    } catch {
      existingOptions = defaultOptionsForType(row.questionType)
    }
    Object.assign(form, { ...row, options: existingOptions })
    loadSpeechFields(row.questionContent)
  } else {
    editingId.value = null
    Object.assign(form, {
      subjectId: filterSubject.value || null,
      gradeLevelId: filterGrade.value || null,
      questionType: 1,
      questionContent: '',
      score: 10,
      timeLimit: 0,
      analysis: '',
      sortOrder: 0,
      speechText: '',
      audioUrl: '',
      savedSpeechText: '',
      options: defaultOptionsForType(1),
    })
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.subjectId || !form.gradeLevelId) {
    ElMessage.warning('请选择学科和年级')
    return
  }
  saving.value = true
  try {
    syncSpeechToContent()
    form.options.forEach((option, index) => {
      option.sortOrder = index
      // Fill-in, Order, Match types implicitly have isCorrect = 1 for their entries
      if ([3, 4, 5].includes(form.questionType)) {
        option.isCorrect = 1
      }
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

async function handleAiGenerate() {
  const subjectName = subjectLabel(Number(form.subjectId))
  const gradeName = gradeLabel(Number(form.gradeLevelId))
  if (!form.subjectId || !form.gradeLevelId) {
    ElMessage.warning('请先选择学科和年级')
    return
  }

  aiGenerating.value = true
  try {
    const res = await generateAiQuestionDraft({
      subjectName,
      gradeName,
      questionType: form.questionType,
      knowledgePoint: aiKnowledgePoint.value,
    })
    if (res.code === 200 && res.data) {
      if (res.data.questionContent) form.questionContent = res.data.questionContent
      if (res.data.analysis) form.analysis = res.data.analysis
      if (Array.isArray(res.data.options) && res.data.options.length > 0) {
        form.options = res.data.options.map((option, index) => ({
          optionLabel: option.optionLabel || String.fromCharCode(65 + index),
          optionContent: option.optionContent || '',
          isCorrect: option.isCorrect ?? (index === 0 ? 1 : 0),
          sortOrder: option.sortOrder ?? index,
        }))
      }
      syncSpeechTextFromContent()
      ElMessage.success('AI题目已生成，请审核后保存')
    } else {
      ElMessage.error(res.msg || 'AI题目生成失败')
    }
  } finally {
    aiGenerating.value = false
  }
}

async function handleAiAnalysis() {
  if (!form.questionContent) {
    ElMessage.warning('请先填写题目内容')
    return
  }
  const correctAnswer = collectCorrectAnswer()
  if (!correctAnswer) {
    ElMessage.warning('请先配置正确答案')
    return
  }

  aiAnalysisLoading.value = true
  try {
    const res = await generateAiQuestionAnalysis({
      questionContent: richContentSummary(form.questionContent, 300),
      correctAnswer,
      options: form.options.map(option => richContentSummary(option.optionContent, 120)).filter(Boolean),
      existingAnalysis: richContentSummary(form.analysis, 300),
    })
    if (res.code === 200 && res.data?.analysis) {
      form.analysis = res.data.analysis
      ElMessage.success('AI解析已生成，请审核后保存')
    } else {
      ElMessage.error(res.msg || 'AI解析生成失败')
    }
  } finally {
    aiAnalysisLoading.value = false
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

function addOption(type: number) {
  if (type === 1) {
    form.options.push({ optionLabel: String.fromCharCode(65 + form.options.length), optionContent: '', isCorrect: 0, sortOrder: form.options.length })
  } else if (type === 3) {
    form.options.push({ optionLabel: '', optionContent: '', isCorrect: 1, sortOrder: form.options.length })
  } else if (type === 4) {
    form.options.push({ optionLabel: '', optionContent: `步骤 ${form.options.length + 1}`, isCorrect: 1, sortOrder: form.options.length })
  } else if (type === 5) {
    form.options.push({ optionLabel: `左侧${form.options.length + 1}`, optionContent: `右侧${form.options.length + 1}`, isCorrect: 1, sortOrder: form.options.length })
  }
}

function removeOption(index: number) {
  form.options.splice(index, 1)
}

function moveOption(index: number, direction: number) {
  const targetIndex = index + direction
  if (targetIndex < 0 || targetIndex >= form.options.length) return
  const temp = form.options[index]
  form.options[index] = form.options[targetIndex]
  form.options[targetIndex] = temp
}

function getTrueFalseCorrectIndex() {
  return form.options.findIndex(o => o.isCorrect === 1)
}

function setTrueFalseCorrect(index: number) {
  form.options.forEach((o, i) => o.isCorrect = (i === index ? 1 : 0))
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

function collectCorrectAnswer() {
  const correctOptions = form.options
    .filter(option => option.isCorrect === 1)
    .map(option => option.optionLabel || richContentSummary(option.optionContent, 120))
    .filter(Boolean)
  return correctOptions.join('；')
}

function questionTypeLabel(type: number) {
  return ['', '选择题', '判断题', '填空题', '排序题', '连线题'][type] || '未知'
}

onMounted(() => {
  loadDictionaries()
  fetchData()
})
</script>

<style scoped>
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
  background: #fcfcfc;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.option-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.choice-row {
  display: grid;
  grid-template-columns: 60px minmax(0, 1fr) 100px 48px;
}

.tf-row {
  justify-content: flex-start;
}

.blank-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 48px;
}

.order-row {
  display: grid;
  grid-template-columns: 80px minmax(0, 1fr) 60px 48px;
  background: #fff;
  padding: 8px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
}

.order-handle {
  font-weight: bold;
  color: #4A90D9;
}

.match-row {
  display: flex;
  align-items: center;
  background: #fff;
  padding: 8px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
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

.ai-tools {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 10px;
  margin-bottom: 16px;
  padding: 12px;
  border: 1px solid #e8f0fe;
  border-radius: 8px;
  background: #f7fbff;
}

@media (max-width: 760px) {
  .ai-tools {
    grid-template-columns: 1fr;
  }
}
</style>
