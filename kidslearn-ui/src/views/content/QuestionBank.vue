<template>
  <el-card>
    <template #header>
      <AdminPageHeader class="question-page-header" title="题库管理" description="查找、维护并批量处理平台题目。" :count="total">
        <template #secondary><el-button plain @click="handleAiRateDifficulty"><el-icon><MagicStick /></el-icon>AI 评分难度</el-button></template>
        <template #primary><el-button type="primary" @click="openDialog()">新增题目</el-button></template>
      </AdminPageHeader>
    </template>

    <AdminFilterBar :active-count="activeFilterCount" :loading="loading" @search="applyFilters" @reset="resetFilters">
      <el-input v-model="filterKeyword" class="question-search" clearable placeholder="搜索题目内容或 ID" @keyup.enter="applyFilters" />
      <el-select v-model="filterSubject" placeholder="学科" clearable>
        <el-option v-for="sub in subjects" :key="sub.id" :label="sub.subjectName" :value="sub.id" />
      </el-select>
      <el-select v-model="filterGrade" placeholder="年级" clearable>
        <el-option v-for="g in grades" :key="g.id" :label="g.levelName" :value="g.id" />
      </el-select>
      <template #advanced>
        <el-select v-model="filterType" placeholder="题型" clearable>
          <el-option label="选择题" :value="1" /><el-option label="判断题" :value="2" /><el-option label="填空题" :value="3" /><el-option label="排序题" :value="4" /><el-option label="连线题" :value="5" />
        </el-select>
        <el-select v-model="filterAudio" placeholder="语音状态" clearable><el-option label="已生成" :value="true" /><el-option label="未生成" :value="false" /></el-select>
        <el-select v-model="filterAnalysis" placeholder="解析状态" clearable><el-option label="已有解析" :value="true" /><el-option label="缺少解析" :value="false" /></el-select>
        <el-select v-model="filterDifficulty" placeholder="难度" clearable><el-option v-for="value in 5" :key="value" :label="`${value} 星`" :value="value" /></el-select>
      </template>
    </AdminFilterBar>

    <AdminBatchBar :selected-count="selectedRows.length" @clear="clearSelection">
      <el-button type="primary" plain @click="handleBatchGenerateAudio"><el-icon><Headset /></el-icon>生成语音</el-button>
      <el-button type="primary" plain @click="handleBatchGenerateAnalysis"><el-icon><Document /></el-icon>生成解析</el-button>
    </AdminBatchBar>

    <div ref="tableBox">
    <el-table ref="questionTableRef" :data="tableData" stripe v-loading="loading" :max-height="tableMaxHeight" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" />
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
      <el-table-column label="解析" width="90">
        <template #default="{ row }">
          <el-tag v-if="hasAnalysis(row.analysis)" type="success" size="small">已有</el-tag>
          <el-tag v-else type="warning" size="small">缺失</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="score" label="分值" width="80" />
      <el-table-column prop="difficulty" label="难度" width="100">
        <template #default="{ row }">
          <el-rate v-if="row.difficulty" :model-value="row.difficulty" disabled size="small" />
          <el-tag v-else type="info" size="small">未评</el-tag>
        </template>
      </el-table-column>
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
      :page-sizes="[20, 50, 100, 200]"
      v-model:current-page="currentPage"
      layout="total, sizes, prev, pager, next"
      @current-change="fetchData"
      @size-change="handleSizeChange"
    />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? `编辑题目 #${editingId}` : '新增题目'"
      fullscreen
      destroy-on-close
      class="question-editor-dialog"
      @closed="handleEditorClosed"
    >
      <div class="editor-workspace">
        <nav class="editor-sections" aria-label="题目编辑分区">
          <a href="#question-basic">基本信息</a>
          <a href="#question-content">题目内容</a>
          <a href="#question-audio">题目语音</a>
          <a href="#question-options">选项配置</a>
          <a href="#question-analysis">答案解析</a>
          <a href="#question-settings">发布设置</a>
        </nav>
        <main class="editor-main">
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

        <el-form-item id="question-basic" label="所属学科">
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

        <el-form-item id="question-content" label="题目内容">
          <RichContentEditor v-model="form.questionContent" :context="questionEditorContext" />
          <div v-if="form.questionType === 3" style="font-size:12px;color:#999;margin-top:4px">
            提示：填空题可在上方内容中使用 ____ 或 [ ] 表示需要填空的位置。
          </div>
        </el-form-item>

        <el-form-item id="question-audio" label="题目语音">
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
        <el-form-item id="question-options" label="选项配置" class="dynamic-options-section">

          <!-- 1: 选择题 -->
          <div v-if="form.questionType === 1" class="option-list">
            <div v-for="(opt, index) in form.options" :key="index" class="option-row choice-row">
              <el-input v-model="opt.optionLabel" class="option-label" placeholder="标签(A/B/C)" />
              <RichContentEditor v-model="opt.optionContent" class="option-editor" compact :context="optionEditorContext" />
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
              <RichContentEditor v-model="opt.optionContent" class="option-editor" compact :context="optionEditorContext" />
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
                <RichContentEditor v-model="opt.optionContent" class="option-editor" compact :context="optionEditorContext" />
              </div>
              <el-button link type="danger" @click="removeOption(index)" style="margin-top:24px">删除</el-button>
            </div>
            <el-button @click="addOption(5)" style="align-self:flex-start">添加配对</el-button>
            <div style="font-size:12px;color:#999;margin-top:4px">前端会自动将左侧和右侧各自打乱显示。</div>
          </div>

        </el-form-item>

        <el-form-item id="question-analysis" label="解析">
          <RichContentEditor v-model="form.analysis" />
        </el-form-item>

        <el-form-item id="question-settings" label="分值">
          <el-input-number v-model="form.score" :min="1" />
        </el-form-item>

        <el-form-item label="限时(秒)">
          <el-input-number v-model="form.timeLimit" :min="0" />
        </el-form-item>
      </el-form>
        </main>

        <aside class="quality-panel">
          <h3>质量检查</h3>
          <ul>
            <li :class="{ passed: Boolean(form.subjectId && form.gradeLevelId) }">已选择学科和年级</li>
            <li :class="{ passed: Boolean(richContentSummary(form.questionContent, 20)) }">题目内容已填写</li>
            <li :class="{ passed: hasValidOptions }">答案和选项配置完整</li>
            <li :class="{ passed: hasAnalysis(form.analysis) }">答案解析已填写</li>
            <li :class="{ passed: !speechNeedsUpdate }">语音内容为最新版本</li>
          </ul>
          <div class="preview-card">
            <span>学生端预览</span>
            <p>{{ richContentSummary(form.questionContent, 120) || '填写题目后将在这里显示摘要。' }}</p>
            <small>{{ questionTypeLabel(form.questionType) }} · {{ form.score }} 分</small>
          </div>
        </aside>
      </div>

      <template #footer>
        <div class="editor-footer">
          <span :class="['save-state', { dirty: formDirty }]">{{ saveStateLabel }}</span>
          <div>
            <el-button @click="closeEditor">退出</el-button>
            <el-button type="primary" @click="handleSave" :loading="saving">保存题目</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="aiCandidateVisible" title="对比 AI 候选题目" width="880px" append-to-body>
      <div class="candidate-compare">
        <section>
          <span>当前内容</span>
          <h3>{{ richContentSummary(form.questionContent, 160) || '尚未填写题目内容' }}</h3>
          <p>{{ richContentSummary(form.analysis, 140) || '暂无解析' }}</p>
          <small>{{ form.options.length }} 个选项</small>
        </section>
        <section class="candidate-compare__new">
          <span>AI 候选</span>
          <h3>{{ richContentSummary(aiCandidate?.questionContent, 160) || '未生成题目内容' }}</h3>
          <p>{{ richContentSummary(aiCandidate?.analysis, 140) || '暂无解析' }}</p>
          <small>{{ aiCandidate?.options?.length || 0 }} 个选项</small>
        </section>
      </div>
      <template #footer>
        <el-button @click="aiCandidateVisible = false">保留当前内容</el-button>
        <el-button type="primary" @click="applyAiCandidate">应用候选内容</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="conflictVisible" title="检测到版本冲突" width="620px" append-to-body>
      <el-alert type="warning" title="这道题已被其他管理员更新" description="直接覆盖可能丢失他人的修改。建议先加载服务器版本，再重新合并你的草稿。" show-icon :closable="false" />
      <div class="conflict-compare"><section><span>你的版本</span><p>{{ richContentSummary(form.questionContent, 180) }}</p></section><section><span>服务器最新版本</span><p>{{ richContentSummary(conflictRemote?.questionContent, 180) }}</p></section></div>
      <template #footer><el-button @click="reloadRemoteVersion">加载服务器版本</el-button><el-button type="danger" plain :loading="saving" @click="forceSave">仍然覆盖保存</el-button></template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onBeforeUnmount, onMounted, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TableInstance } from 'element-plus'
import RichContentEditor, { type RichEditorContext } from '@/components/RichContentEditor.vue'
import AdminBatchBar from '@/components/admin/AdminBatchBar.vue'
import AdminFilterBar from '@/components/admin/AdminFilterBar.vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import { useTableHeight } from '@/composables/useTableHeight'
import { useAdminTaskStore } from '@/stores/adminTasks'

const { tableBox, tableMaxHeight } = useTableHeight()
const route = useRoute()
const router = useRouter()
const taskStore = useAdminTaskStore()
import {
  deleteQuestion,
  generateAiQuestionAnalysis,
  generateAiQuestionDraft,
  generateQuestionAudio,
  getQuestionDetail,
  getQuestionList,
  getQuestionOptions,
  saveQuestion,
  getSubjectList,
  getGradeLevelList,
  aiRateDifficulty,
  type QuestionSaveDTO,
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
const batchGenerating = ref(false)
const ratingDifficulty = ref(false)
const batchProgress = ref(0)
const batchDone = ref(0)
const batchTotal = ref(0)
const batchCancelled = ref(false)
const batchAnalysisGenerating = ref(false)
const batchAnalysisProgress = ref(0)
const batchAnalysisDone = ref(0)
const batchAnalysisTotal = ref(0)
const batchAnalysisCancelled = ref(false)
const batchAnalysisStatus = ref('')
const selectedRows = ref<any[]>([])
const aiGenerating = ref(false)
const aiAnalysisLoading = ref(false)
const aiCandidateVisible = ref(false)
const aiCandidate = ref<QuestionSaveDTO | null>(null)
const tableData = ref<any[]>([])
const subjects = ref<any[]>([])
const grades = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const questionTableRef = ref<TableInstance>()

const filterKeyword = ref('')
const filterSubject = ref<number | ''>('')
const filterGrade = ref<number | ''>('')
const filterType = ref<number | ''>('')
const filterAudio = ref<boolean | ''>('')
const filterAnalysis = ref<boolean | ''>('')
const filterDifficulty = ref<number | ''>('')

const activeFilterCount = computed(() => [filterKeyword.value, filterSubject.value, filterGrade.value, filterType.value, filterAudio.value, filterAnalysis.value, filterDifficulty.value]
  .filter(value => value !== '' && value !== null && value !== undefined).length)

function applyFilters() {
  currentPage.value = 1
  fetchData()
}

function resetFilters() {
  filterKeyword.value = ''
  filterSubject.value = ''
  filterGrade.value = ''
  filterType.value = ''
  filterAudio.value = ''
  filterAnalysis.value = ''
  filterDifficulty.value = ''
  applyFilters()
}

function clearSelection() {
  questionTableRef.value?.clearSelection()
}

function handleSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  fetchData()
}

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const aiKnowledgePoint = ref('')
const formDirty = ref(false)
const lastDraftSavedAt = ref<Date | null>(null)
const conflictVisible = ref(false)
const conflictRemote = ref<any>(null)
const sourceUpdateTime = ref('')
let formBaseline = ''
let draftTimer: number | null = null

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

const hasValidOptions = computed(() => {
  const populated = form.options.filter(option => richContentSummary(option.optionContent, 40) || option.optionLabel)
  if (form.questionType === 1) return populated.length >= 2 && populated.some(option => option.isCorrect === 1)
  if (form.questionType === 2) return form.options.length === 2 && form.options.filter(option => option.isCorrect === 1).length === 1
  return populated.length >= 1
})

const saveStateLabel = computed(() => {
  if (saving.value) return '正在保存…'
  if (formDirty.value) return lastDraftSavedAt.value
    ? `有未保存修改 · 草稿 ${lastDraftSavedAt.value.toLocaleTimeString('zh-CN', { hour12: false })}`
    : '有未保存修改'
  return editingId.value ? '所有修改已保存' : '新题目草稿'
})

const draftKey = computed(() => `admin_question_draft_${editingId.value || 'new'}`)

// AI图片生成上下文
const questionTypeNameMap: Record<number, string> = { 1: '选择题', 2: '判断题', 3: '填空题', 4: '排序题', 5: '连线题' }
const questionEditorContext = computed<RichEditorContext>(() => ({
  subjectName: subjects.value.find(s => s.id === form.subjectId)?.subjectName,
  gradeName: grades.value.find(g => g.id === form.gradeLevelId)?.levelName,
  questionType: questionTypeNameMap[form.questionType],
  knowledgePoint: aiKnowledgePoint.value,
  usage: 'question',
}))
const optionEditorContext = computed<RichEditorContext>(() => ({
  subjectName: subjects.value.find(s => s.id === form.subjectId)?.subjectName,
  gradeName: grades.value.find(g => g.id === form.gradeLevelId)?.levelName,
  questionType: questionTypeNameMap[form.questionType],
  questionContent: form.questionContent,
  knowledgePoint: aiKnowledgePoint.value,
  usage: 'option',
}))

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
      { optionLabel: 'A', optionContent: '步骤 1', isCorrect: 1, sortOrder: 0 },
      { optionLabel: 'B', optionContent: '步骤 2', isCorrect: 1, sortOrder: 1 },
      { optionLabel: 'C', optionContent: '步骤 3', isCorrect: 1, sortOrder: 2 },
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
      keyword: filterKeyword.value.trim() || undefined,
      hasAudio: filterAudio.value === '' ? undefined : filterAudio.value,
      hasAnalysis: filterAnalysis.value === '' ? undefined : filterAnalysis.value,
      difficulty: filterDifficulty.value || undefined,
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
  // 先重置表单，防止上次数据残留
  Object.assign(form, {
    subjectId: null,
    gradeLevelId: null,
    questionType: 1,
    questionContent: '',
    score: 10,
    timeLimit: 0,
    analysis: '',
    sortOrder: 0,
    speechText: '',
    audioUrl: '',
    savedSpeechText: '',
    options: [],
  })

  if (row) {
    editingId.value = row.id
    sourceUpdateTime.value = String(row.updateTime || row.updatedAt || '')
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
    sourceUpdateTime.value = ''
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
  formBaseline = JSON.stringify(form)
  formDirty.value = false
  restoreDraft()
  dialogVisible.value = true
  const targetPath = editingId.value ? `/question-bank/${editingId.value}/edit` : '/question-bank/new'
  if (route.path !== targetPath) router.push(targetPath)
}

async function openEditorFromRoute() {
  if (route.name === 'QuestionCreate') {
    await openDialog()
    return
  }
  if (route.name !== 'QuestionEdit') return
  const id = Number(route.params.id)
  if (!Number.isFinite(id) || id <= 0) {
    ElMessage.error('题目 ID 无效')
    router.replace('/question-bank')
    return
  }
  const res = await getQuestionDetail(id)
  if (res.code === 200 && res.data) await openDialog(res.data)
  else {
    ElMessage.error(res.msg || '题目不存在')
    router.replace('/question-bank')
  }
}

function saveDraft() {
  if (!dialogVisible.value || !formDirty.value) return
  localStorage.setItem(draftKey.value, JSON.stringify({ form, savedAt: new Date().toISOString() }))
  lastDraftSavedAt.value = new Date()
}

function restoreDraft() {
  try {
    const raw = localStorage.getItem(draftKey.value)
    if (!raw) return
    const draft = JSON.parse(raw)
    if (draft?.form && window.confirm('发现本地未提交草稿，是否恢复？')) {
      Object.assign(form, draft.form)
      lastDraftSavedAt.value = new Date(draft.savedAt)
      formDirty.value = true
    }
  } catch {
    localStorage.removeItem(draftKey.value)
  }
}

function closeEditor() {
  if (formDirty.value && !window.confirm('当前修改尚未保存，确定退出吗？')) return
  formDirty.value = false
  dialogVisible.value = false
}

function handleEditorClosed() {
  if (route.path !== '/question-bank') router.push('/question-bank')
}

async function handleSave() {
  await saveWithConflict(false)
}

async function forceSave() {
  conflictVisible.value = false
  await saveWithConflict(true)
}

async function reloadRemoteVersion() {
  if (!conflictRemote.value) return
  conflictVisible.value = false
  await openDialog(conflictRemote.value)
  ElMessage.info('已加载服务器最新版本，本地草稿仍保留在浏览器中')
}

async function saveWithConflict(force: boolean) {
  if (!form.subjectId || !form.gradeLevelId) {
    ElMessage.warning('请选择学科和年级')
    return
  }
  saving.value = true
  try {
    if (!force && editingId.value && sourceUpdateTime.value) {
      const latest = await getQuestionDetail(editingId.value)
      const latestUpdateTime = String(latest.data?.updateTime || latest.data?.updatedAt || '')
      if (latest.code === 200 && latestUpdateTime && latestUpdateTime !== sourceUpdateTime.value) {
        conflictRemote.value = latest.data
        conflictVisible.value = true
        return
      }
    }
    syncSpeechToContent()
    form.options.forEach((option, index) => {
      option.sortOrder = index
      // Fill-in, Order, Match types implicitly have isCorrect = 1 for their entries
      if ([3, 4, 5].includes(form.questionType)) {
        option.isCorrect = 1
      }
      // 排序题自动生成 optionLabel（A/B/C/...），确保后端比对基准一致
      if (form.questionType === 4 && !option.optionLabel) {
        option.optionLabel = String.fromCharCode(65 + index)
      }
    })
    const res = await saveQuestion({ ...form, id: editingId.value })
    if (res.code === 200) {
      ElMessage.success('保存成功')
      formDirty.value = false
      localStorage.removeItem(draftKey.value)
      dialogVisible.value = false
      sourceUpdateTime.value = ''
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
      const candidate: QuestionSaveDTO = { ...res.data }
      if (Array.isArray(candidate.options) && candidate.options.length > 0) {
        candidate.options = candidate.options.map((option, index) => {
          let label = option.optionLabel || ''
          // 连线题：如果 optionLabel 为空或是单个字母(A/B/C)，使用"左侧N"格式
          if (form.questionType === 5 && (!label || /^[A-Z]$/.test(label))) {
            label = `左侧${index + 1}`
          }
          // 其他题型：如果 optionLabel 为空，使用 A/B/C
          if (!label) {
            label = String.fromCharCode(65 + index)
          }
          return {
            ...option,
            optionLabel: label,
            optionContent: option.optionContent || '',
            isCorrect: option.isCorrect ?? (index === 0 ? 1 : 0),
            sortOrder: option.sortOrder ?? index,
          }
        })
      }
      aiCandidate.value = candidate
      aiCandidateVisible.value = true
    } else {
      ElMessage.error(res.msg || 'AI题目生成失败')
    }
  } finally {
    aiGenerating.value = false
  }
}

function applyAiCandidate() {
  const candidate = aiCandidate.value
  if (!candidate) return
  if (candidate.questionContent) form.questionContent = candidate.questionContent
  if (candidate.analysis) form.analysis = candidate.analysis
  if (Array.isArray(candidate.options) && candidate.options.length > 0) {
    form.options = candidate.options.map(option => ({
      optionLabel: option.optionLabel || '',
      optionContent: option.optionContent || '',
      isCorrect: option.isCorrect ?? 0,
      sortOrder: option.sortOrder ?? 0,
    }))
  }
  syncSpeechTextFromContent()
  aiCandidateVisible.value = false
  ElMessage.success('已应用 AI 候选内容，请继续审核后保存')
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

  const toGenerate = targets.filter(row => !hasQuestionAudio(row.questionContent))
  if (!toGenerate.length) {
    ElMessage.info('所有选中题目都已有语音')
    return
  }

  void taskStore.runTask({
    type: 'question-audio',
    title: `批量生成题目语音（${toGenerate.length}）`,
    total: toGenerate.length,
    runner: async (reporter) => {
      let success = 0
      let failed = 0
      const concurrency = 3
      for (let i = 0; i < toGenerate.length; i += concurrency) {
        if (reporter.isCancelled()) break
        const batch = toGenerate.slice(i, i + concurrency)
        const results = await Promise.allSettled(batch.map(row => generateQuestionAudio(row.id, {})))
        results.forEach((result, index) => {
          const row = batch[index]
          if (result.status === 'fulfilled' && result.value.code === 200) success++
          else {
            failed++
            const reason = result.status === 'rejected' ? String(result.reason?.message || '请求失败') : result.value.msg || '生成失败'
            reporter.addFailure(`题目 #${row.id}`, reason)
          }
        })
        reporter.progress({ completed: Math.min(i + concurrency, toGenerate.length), success, failed, message: `已处理 ${Math.min(i + concurrency, toGenerate.length)}/${toGenerate.length}` })
      }
      await fetchData()
    },
  })
  ElMessage.success('任务已创建，可在任务中心查看进度')
  router.push('/tasks')
}

async function handleBatchGenerateAnalysis() {
  const targets = selectedRows.value.filter(row => row.id)
  if (targets.length === 0) {
    ElMessage.warning('请先选择题目')
    return
  }

  // 统计已有解析的数量
  const alreadyHasAnalysis = targets.filter(row => hasAnalysis(row.analysis))
  const toGenerate = targets.filter(row => !hasAnalysis(row.analysis))

  if (toGenerate.length === 0) {
    ElMessage.info('所有选中的题目已有解析，无需生成')
    return
  }

  let confirmMsg = `共选择 ${targets.length} 道题目，其中 ${toGenerate.length} 道缺少解析。`
  if (alreadyHasAnalysis.length > 0) {
    confirmMsg += `\n${alreadyHasAnalysis.length} 道已有解析将跳过。`
  }
  confirmMsg += '\n确认开始批量生成？'

  await ElMessageBox.confirm(confirmMsg, '批量生成解析', {
    type: 'info',
    confirmButtonText: '开始生成',
  })

  void taskStore.runTask({
    type: 'question-analysis',
    title: `批量生成题目解析（${toGenerate.length}）`,
    total: toGenerate.length,
    runner: async (reporter) => {
      let success = 0
      let failed = 0
      for (let i = 0; i < toGenerate.length; i++) {
        if (reporter.isCancelled()) break
        const row = toGenerate[i]
        try {
          const optRes = await getQuestionOptions(row.id)
          const optionRows = optRes.code === 200 && Array.isArray(optRes.data) ? optRes.data : []
          const options = optionRows.map((opt: any) => richContentSummary(opt.optionContent, 120)).filter(Boolean)
          const correctAnswer = optionRows.filter((opt: any) => opt.isCorrect === 1).map((opt: any) => opt.optionLabel || richContentSummary(opt.optionContent, 120)).filter(Boolean).join('；')
          const res = await generateAiQuestionAnalysis({ questionContent: richContentSummary(row.questionContent, 300), correctAnswer, options, existingAnalysis: '' })
          if (res.code !== 200 || !res.data?.analysis) throw new Error(res.msg || 'AI 未返回解析')
          const saveRes = await saveQuestion({ id: row.id, analysis: res.data.analysis })
          if (saveRes.code !== 200) throw new Error(saveRes.msg || '保存失败')
          success++
        } catch (error: any) {
          failed++
          reporter.addFailure(`题目 #${row.id}`, error?.message || '生成失败')
        }
        reporter.progress({ completed: i + 1, success, failed, message: `已处理 ${i + 1}/${toGenerate.length}` })
      }
      await fetchData()
    },
  })
  ElMessage.success('任务已创建，可在任务中心查看进度')
  router.push('/tasks')
}

function addOption(type: number) {
  if (type === 1) {
    form.options.push({ optionLabel: String.fromCharCode(65 + form.options.length), optionContent: '', isCorrect: 0, sortOrder: form.options.length })
  } else if (type === 3) {
    form.options.push({ optionLabel: '', optionContent: '', isCorrect: 1, sortOrder: form.options.length })
  } else if (type === 4) {
    form.options.push({ optionLabel: String.fromCharCode(65 + form.options.length), optionContent: `步骤 ${form.options.length + 1}`, isCorrect: 1, sortOrder: form.options.length })
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

function hasAnalysis(analysis?: string) {
  if (!analysis) return false
  const text = richContentSummary(analysis, 200)
  return text.trim().length > 0
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

// AI 批量评分难度（循环调用直到全部完成）
async function handleAiRateDifficulty() {
  await ElMessageBox.confirm(
    'AI 将对所有未评分的题目逐批评估难度（1-5星），每批50题。\n这个过程可能需要几分钟，确定开始？',
    'AI 难度评分',
    { type: 'info', confirmButtonText: '开始评分', cancelButtonText: '取消' }
  )

  void taskStore.runTask({
    type: 'question-difficulty',
    title: 'AI 批量评分题目难度',
    total: Math.max(1, total.value),
    runner: async (reporter) => {
      let completed = 0
      let remaining = total.value
      while (!reporter.isCancelled()) {
        const res = await aiRateDifficulty(50)
        if (res.code !== 200) throw new Error(res.msg || '评分失败')
        completed += res.data.rated
        remaining = res.data.remaining
        reporter.progress({ completed: Math.min(completed, total.value), success: completed, message: `已评分 ${completed}，剩余 ${remaining}` })
        if (remaining === 0 || res.data.rated === 0) break
      }
      await fetchData()
    },
  })
  ElMessage.success('难度评分任务已创建')
  router.push('/tasks')
}

onMounted(() => {
  loadDictionaries()
  fetchData()
  openEditorFromRoute()
  draftTimer = window.setInterval(saveDraft, 30_000)
})

watch(form, () => {
  if (!dialogVisible.value || !formBaseline) return
  formDirty.value = JSON.stringify(form) !== formBaseline
}, { deep: true })

onBeforeRouteLeave((_to, _from, next) => {
  if (formDirty.value && !window.confirm('当前题目有未保存修改，确定离开吗？')) next(false)
  else next()
})

onBeforeUnmount(() => {
  if (draftTimer) window.clearInterval(draftTimer)
})
</script>

<style scoped>
.filter-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.question-page-header {
  margin-bottom: 0;
}

.question-search {
  width: min(320px, 100%);
}

:deep(.admin-filter-bar .el-select) {
  width: 150px;
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

.editor-workspace {
  display: grid;
  grid-template-columns: 168px minmax(520px, 760px) minmax(260px, 320px);
  justify-content: center;
  gap: var(--space-6);
  min-height: 100%;
}

.editor-sections,
.quality-panel {
  position: sticky;
  top: 0;
  align-self: start;
}

.editor-sections {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.editor-sections a {
  padding: var(--space-2) var(--space-3);
  color: var(--color-gray-600);
  text-decoration: none;
  border-radius: var(--radius-control);
}

.editor-sections a:hover,
.editor-sections a:focus-visible {
  color: var(--color-brand-700);
  background: var(--color-brand-50);
}

.editor-main {
  min-width: 0;
}

.quality-panel {
  padding: var(--space-4);
  background: var(--color-gray-50);
  border: 1px solid var(--admin-border);
  border-radius: var(--radius-card);
}

.quality-panel h3 {
  margin: 0 0 var(--space-3);
  font-size: var(--font-size-heading-2);
}

.quality-panel ul {
  display: grid;
  gap: var(--space-2);
  padding: 0;
  margin: 0;
  list-style: none;
}

.quality-panel li {
  color: var(--color-warning-700);
  font-size: var(--font-size-caption);
}

.quality-panel li::before {
  content: '○';
  margin-right: var(--space-2);
}

.quality-panel li.passed {
  color: var(--color-success-600);
}

.quality-panel li.passed::before {
  content: '✓';
}

.preview-card {
  padding: var(--space-3);
  margin-top: var(--space-4);
  background: var(--admin-surface);
  border: 1px solid var(--admin-border);
  border-radius: var(--radius-control);
}

.preview-card span {
  color: var(--admin-muted);
  font-size: var(--font-size-caption);
}

.preview-card p {
  margin: var(--space-2) 0;
  color: var(--admin-text);
}

.preview-card small {
  color: var(--admin-muted);
}

.editor-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.save-state {
  color: var(--color-success-600);
  font-size: var(--font-size-caption);
}

.save-state.dirty {
  color: var(--color-warning-700);
}

.candidate-compare {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
}

.candidate-compare section {
  min-width: 0;
  padding: var(--space-4);
  border: 1px solid var(--admin-border);
  border-radius: var(--radius-card);
}

.conflict-compare {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.conflict-compare section {
  padding: var(--space-3);
  background: var(--color-gray-50);
  border: 1px solid var(--admin-border);
  border-radius: var(--radius-control);
}

.conflict-compare span { color: var(--admin-muted); font-size: var(--font-size-caption); }
.conflict-compare p { margin-top: var(--space-2); overflow-wrap: anywhere; }

.candidate-compare__new {
  background: var(--color-info-50);
  border-color: #b2ddff !important;
}

.candidate-compare span,
.candidate-compare small {
  color: var(--admin-muted);
  font-size: var(--font-size-caption);
}

.candidate-compare h3 {
  margin: var(--space-2) 0;
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
}

.candidate-compare p {
  min-height: 48px;
  color: var(--color-gray-600);
}

@media (max-width: 760px) {
  .ai-tools {
    grid-template-columns: 1fr;
  }

  .editor-workspace {
    display: block;
    min-width: 0;
  }

  .editor-sections,
  .quality-panel {
    position: static;
  }

  .editor-sections {
    flex-direction: row;
    margin-bottom: var(--space-4);
    overflow-x: auto;
  }

  .editor-sections a {
    flex: 0 0 auto;
  }

  .quality-panel {
    margin-top: var(--space-4);
  }

  .candidate-compare {
    grid-template-columns: 1fr;
  }
}
</style>

<style>
.question-editor-dialog .el-dialog__body {
  height: calc(100vh - 132px);
  overflow-y: auto;
}

.question-editor-dialog .el-dialog__footer {
  position: sticky;
  bottom: 0;
  background: var(--admin-surface);
}
</style>
