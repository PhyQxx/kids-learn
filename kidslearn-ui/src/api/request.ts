import request from '@/utils/request'
import type { AxiosRequestConfig } from 'axios'

export type ApiEnvelope<T = unknown> = {
  code: number
  msg?: string
  data: T
}

export type PageQuery = {
  page?: number
  pageSize?: number
  keyword?: string
} & Record<string, unknown>

export type PageResult<T> = {
  list: T[]
  total: number
  page?: number
  pageSize?: number
}

export type IdRecord = {
  id?: number | null
}

export type AdminRecord = IdRecord & Record<string, unknown>

export type DashboardStats = Record<string, unknown>

export type LoginPayload = {
  username: string
  password: string
}

export type TokenPayload = {
  accessToken: string
  refreshToken: string
  userInfo?: Record<string, unknown>
}

export type QuestionOptionDTO = AdminRecord & {
  questionId?: number | null
  optionLabel: string
  optionContent: string
  isCorrect: number
  sortOrder: number
}

export type QuestionSaveDTO = AdminRecord & {
  subjectId?: number | null
  gradeLevelId?: number | null
  courseLevelId?: number | null
  questionType?: number
  questionContent?: string
  score?: number
  timeLimit?: number
  analysis?: string
  sortOrder?: number
  options?: QuestionOptionDTO[]
}

export type QuestionAudioDTO = {
  speechText?: string
}

export type QuestionAudioResult = {
  speechText?: string
  audioUrl?: string
}

export type ContentAuditRecord = AdminRecord & {
  targetType?: string
  targetId?: number
  action?: string
  status?: number
  submitterId?: number
  reviewerId?: number
  reviewComment?: string
  submitTime?: string
  reviewTime?: string
}

export type ContentAiPrecheckResult = {
  riskLevel?: string
  summary?: string
  issues?: string[]
  suggestions?: string[]
}

export type AiProviderConfig = {
  provider: string
  name?: string
  enabled: boolean
  baseUrl: string
  model: string
  apiKey: string
  apiKeyConfigured?: boolean
}

export type AiConfigPayload = {
  provider: string
  timeout: number
  providers: AiProviderConfig[]
}

export type AiQuestionDraftRequest = {
  subjectName?: string
  gradeName?: string
  questionType?: number
  knowledgePoint?: string
}

export type AiQuestionAnalysisRequest = {
  questionContent?: string
  correctAnswer?: string
  options?: string[]
  existingAnalysis?: string
}

// Helper: axios interceptor unwraps response to { code, msg, data }.
const get = <T = unknown>(url: string, params?: Record<string, unknown>): Promise<ApiEnvelope<T>> =>
  request.get(url, { params })
const post = <T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ApiEnvelope<T>> =>
  request.post(url, data, config)
const del = <T = unknown>(url: string, config?: AxiosRequestConfig): Promise<ApiEnvelope<T>> =>
  request.delete(url, config)

// 登录
export function login(data: LoginPayload) {
  return post<TokenPayload>('/auth/login', data)
}

// 获取统计数据
export function getDashboardStats() {
  return get<DashboardStats>('/admin/dashboard/stats')
}

// ==================== 学科管理 ====================
export function getSubjectList(params: PageQuery) { return get<PageResult<AdminRecord>>('/admin/subject/list', params) }
export function saveSubject(data: AdminRecord) { return post<void>('/admin/subject/save', data) }
export function deleteSubject(id: number) { return del(`/admin/subject/${id}`) }

// ==================== 课程管理 ====================
export function getCourseList(params: PageQuery) { return get<PageResult<AdminRecord>>('/admin/course/list', params) }
export function saveCourse(data: AdminRecord) { return post<number | void>('/admin/course/save', data) }
export function deleteCourse(id: number) { return del(`/admin/course/${id}`) }

// ==================== 年级管理 ====================
export function getGradeLevelList(params?: PageQuery & { ageGroup?: number }) { return get<AdminRecord[]>('/admin/grade-level/list', params) }
export function saveGradeLevel(data: AdminRecord) { return post<void>('/admin/grade-level/save', data) }
export function deleteGradeLevel(id: number) { return del(`/admin/grade-level/${id}`) }
export function bindCourseGrades(data: { courseId: number; gradeLevelIds: number[] }) { return post('/admin/grade-level/course-bind', data) }
export function getCourseGrades(courseId: number) { return get<number[]>('/admin/grade-level/course-grades', { courseId }) }

// ==================== 关卡管理 ====================
export function getLevelList(params: PageQuery & { subjectId?: number }) { return get<PageResult<AdminRecord>>('/admin/level/list', params) }
export function saveLevel(data: AdminRecord) { return post<void>('/admin/level/save', data) }
export function deleteLevel(id: number) { return del(`/admin/level/${id}`) }

// ==================== 题目管理 ====================
export function getQuestionList(params: PageQuery & {
  subjectId?: number
  gradeLevelId?: number
  courseLevelId?: number
  questionType?: number
}) { return get<PageResult<AdminRecord>>('/admin/question/list', params) }
export function saveQuestion(data: QuestionSaveDTO) { return post<void>('/admin/question/save', data) }
export function deleteQuestion(id: number) { return del(`/admin/question/${id}`) }
export function getQuestionOptions(id: number) { return get<QuestionOptionDTO[]>(`/admin/question/${id}/options`) }
export function generateQuestionAudio(id: number, data: QuestionAudioDTO) {
  return post<QuestionAudioResult>(`/admin/question/${id}/audio`, data)
}
export function generateAiQuestionDraft(data: AiQuestionDraftRequest) {
  return post<QuestionSaveDTO>('/admin/question/ai-generate', data)
}
export function generateAiQuestionAnalysis(data: AiQuestionAnalysisRequest) {
  return post<{ analysis: string }>('/admin/question/ai-analysis', data)
}
export function uploadQuestionImage(file: File) {
  const form = new FormData()
  form.append('file', file)
  return post<{ url: string }>('/admin/file/upload-image', form, { headers: { 'Content-Type': 'multipart/form-data' } })
}

// ==================== 内容审核 ====================
export function getContentAuditList(params: PageQuery & { status?: number; targetType?: string }) {
  return get<PageResult<ContentAuditRecord>>('/admin/content-audit/list', params)
}
export function submitContentAudit(data: ContentAuditRecord) {
  return post<void>('/admin/content-audit/submit', data)
}
export function reviewContentAudit(id: number, status: number, comment?: string) {
  return post<void>(`/admin/content-audit/${id}/review`, null, { params: { status, comment } })
}
export function precheckContentAudit(id: number) {
  return post<ContentAiPrecheckResult>(`/admin/content-audit/${id}/ai-precheck`)
}

// ==================== 专项练习模式管理 (Phase 12) ====================
export function getPracticeModeList(params: PageQuery) { return get<PageResult<AdminRecord>>('/admin/practice/list', params) }
export function savePracticeMode(data: AdminRecord) { return post<void>('/admin/practice/save', data) }
export function deletePracticeMode(id: number) { return del(`/admin/practice/${id}`) }

// ==================== 宠物管理 ====================
export function getPetList(params: PageQuery) { return get<PageResult<AdminRecord>>('/admin/pet/list', params) }
export function savePet(data: AdminRecord) { return post<void>('/admin/pet/save', data) }
export function deletePet(id: number) { return del(`/admin/pet/${id}`) }
export function getPetEvolutions(petId: number) { return get<AdminRecord[]>(`/admin/pet/${petId}/evolutions`) }
export function savePetEvolution(data: AdminRecord) { return post<void>('/admin/pet/evolution/save', data) }

// ==================== 道具管理 ====================
export function getPetItemList(params: PageQuery) { return get<PageResult<AdminRecord>>('/admin/pet-item/list', params) }
export function savePetItem(data: AdminRecord) { return post<void>('/admin/pet-item/save', data) }
export function deletePetItem(id: number) { return del(`/admin/pet-item/${id}`) }

// ==================== 装饰管理 ====================
export function getDecorationList(params: PageQuery) { return get<PageResult<AdminRecord>>('/admin/decoration/list', params) }
export function saveDecoration(data: AdminRecord) { return post<void>('/admin/decoration/save', data) }
export function deleteDecoration(id: number) { return del(`/admin/decoration/${id}`) }

// ==================== 成就管理 ====================
export function getAchievementList(params: PageQuery) { return get<PageResult<AdminRecord>>('/admin/achievement/list', params) }
export function saveAchievement(data: AdminRecord) { return post<void>('/admin/achievement/save', data) }
export function deleteAchievement(id: number) { return del(`/admin/achievement/${id}`) }

// ==================== 贴纸管理 ====================
export function getStickerList(params: PageQuery) { return get<PageResult<AdminRecord>>('/admin/sticker/list', params) }
export function saveSticker(data: AdminRecord) { return post<void>('/admin/sticker/save', data) }
export function deleteSticker(id: number) { return del(`/admin/sticker/${id}`) }
export function getStickerSeriesList() { return get<AdminRecord[]>('/admin/sticker-series/list') }

// ==================== 称号管理 ====================
export function getTitleList(params: PageQuery) { return get<PageResult<AdminRecord>>('/admin/title/list', params) }
export function saveTitle(data: AdminRecord) { return post<void>('/admin/title/save', data) }
export function deleteTitle(id: number) { return del(`/admin/title/${id}`) }

// ==================== 用户管理 ====================
export function getUserList(params: PageQuery) { return get<PageResult<AdminRecord>>('/admin/user/list', params) }
export function saveUser(data: AdminRecord) { return post<void>('/admin/user/save', data) }
export function updateUserStatus(id: number, status: number) {
  return post<void>(`/admin/user/${id}/status`, null, { params: { status } })
}

// ==================== 角色管理 ====================
export function getRoleList() { return get<AdminRecord[]>('/admin/role/list') }
export function saveRole(data: AdminRecord) { return post<void>('/admin/role/save', data) }
export function deleteRole(id: number) { return del(`/admin/role/${id}`) }

// ==================== 系统配置 ====================
export function getConfigList(params: PageQuery) { return get<PageResult<AdminRecord>>('/admin/config/list', params) }
export function saveConfig(data: AdminRecord) { return post<void>('/admin/config/save', data) }
export function getAiConfig() { return get<AiConfigPayload>('/admin/ai/config') }
export function saveAiConfig(data: AiConfigPayload) { return post<void>('/admin/ai/config', data) }

// ==================== 操作日志 ====================
export function getLogList(params: PageQuery & { module?: string }) { return get<PageResult<AdminRecord>>('/admin/log/list', params) }

// ==================== 字典查询 ====================
export function getDictDataByType(dictType: string) { return get<AdminRecord[]>(`/admin/dict/data/${dictType}`) }

// ==================== 字典管理 ====================
export function getDictTypeList(params: PageQuery) { return get<PageResult<AdminRecord>>('/admin/dict/type/list', params) }
export function saveDictType(data: AdminRecord) { return post<void>('/admin/dict/type/save', data) }
export function deleteDictType(id: number) { return del(`/admin/dict/type/${id}`) }
export function getDictDataList(params: PageQuery & { typeId?: number }) { return get<PageResult<AdminRecord>>('/admin/dict/data/list', params) }
export function saveDictData(data: AdminRecord) { return post<void>('/admin/dict/data/save', data) }
export function deleteDictData(id: number) { return del(`/admin/dict/data/${id}`) }

// ==================== 版本管理 ====================
export function getVersionList(params: PageQuery) { return get<PageResult<AdminRecord>>('/admin/version/list', params) }
export function saveVersion(data: AdminRecord) { return post<void>('/admin/version/save', data) }
export function deleteVersion(id: number) { return del(`/admin/version/${id}`) }
