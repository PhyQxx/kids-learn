import request from '@/utils/request'

// Helper: axios interceptor unwraps response to { code, msg, data }
const get = (url: string, params?: any): Promise<any> => request.get(url, { params })
const post = (url: string, data?: any, config?: any): Promise<any> => request.post(url, data, config)
const del = (url: string, config?: any): Promise<any> => request.delete(url, config)

// 登录
export function login(data: { username: string; password: string }) {
  return post('/api/v1/auth/login', data)
}

// 获取统计数据
export function getDashboardStats() {
  return get('/api/v1/dashboard/stats')
}

// ==================== 学科管理 ====================
export function getSubjectList(params: any) { return get('/api/v1/subject/list', params) }
export function saveSubject(data: any) { return post('/api/v1/subject/save', data) }
export function deleteSubject(id: number) { return del(`/api/v1/subject/${id}`) }

// ==================== 课程管理 ====================
export function getCourseList(params: any) { return get('/api/v1/course/list', params) }
export function saveCourse(data: any) { return post('/api/v1/course/save', data) }
export function deleteCourse(id: number) { return del(`/api/v1/course/${id}`) }

// ==================== 关卡管理 ====================
export function getLevelList(params: any) { return get('/api/v1/level/list', params) }
export function saveLevel(data: any) { return post('/api/v1/level/save', data) }
export function deleteLevel(id: number) { return del(`/api/v1/level/${id}`) }

// ==================== 题目管理 ====================
export function getQuestionList(params: any) { return get('/api/v1/question/list', params) }
export function saveQuestion(data: any) { return post('/api/v1/question/save', data) }
export function deleteQuestion(id: number) { return del(`/api/v1/question/${id}`) }
export function getQuestionOptions(id: number) { return get(`/api/v1/question/${id}/options`) }

// ==================== 宠物管理 ====================
export function getPetList(params: any) { return get('/api/v1/pet/list', params) }
export function savePet(data: any) { return post('/api/v1/pet/save', data) }
export function deletePet(id: number) { return del(`/api/v1/pet/${id}`) }
export function getPetEvolutions(petId: number) { return get(`/api/v1/pet/${petId}/evolutions`) }
export function savePetEvolution(data: any) { return post('/api/v1/pet/evolution/save', data) }

// ==================== 道具管理 ====================
export function getPetItemList(params: any) { return get('/api/v1/pet-item/list', params) }
export function savePetItem(data: any) { return post('/api/v1/pet-item/save', data) }
export function deletePetItem(id: number) { return del(`/api/v1/pet-item/${id}`) }

// ==================== 装饰管理 ====================
export function getDecorationList(params: any) { return get('/api/v1/decoration/list', params) }
export function saveDecoration(data: any) { return post('/api/v1/decoration/save', data) }
export function deleteDecoration(id: number) { return del(`/api/v1/decoration/${id}`) }

// ==================== 成就管理 ====================
export function getAchievementList(params: any) { return get('/api/v1/achievement/list', params) }
export function saveAchievement(data: any) { return post('/api/v1/achievement/save', data) }
export function deleteAchievement(id: number) { return del(`/api/v1/achievement/${id}`) }

// ==================== 贴纸管理 ====================
export function getStickerList(params: any) { return get('/api/v1/sticker/list', params) }
export function saveSticker(data: any) { return post('/api/v1/sticker/save', data) }
export function deleteSticker(id: number) { return del(`/api/v1/sticker/${id}`) }
export function getStickerSeriesList() { return get('/api/v1/sticker-series/list') }

// ==================== 称号管理 ====================
export function getTitleList(params: any) { return get('/api/v1/title/list', params) }
export function saveTitle(data: any) { return post('/api/v1/title/save', data) }
export function deleteTitle(id: number) { return del(`/api/v1/title/${id}`) }

// ==================== 用户管理 ====================
export function getUserList(params: any) { return get('/api/v1/user/list', params) }
export function updateUserStatus(id: number, status: number) {
  return post(`/api/v1/user/${id}/status`, null, { params: { status } })
}

// ==================== 角色管理 ====================
export function getRoleList() { return get('/api/v1/role/list') }
export function saveRole(data: any) { return post('/api/v1/role/save', data) }
export function deleteRole(id: number) { return del(`/api/v1/role/${id}`) }

// ==================== 系统配置 ====================
export function getConfigList(params: any) { return get('/api/v1/config/list', params) }
export function saveConfig(data: any) { return post('/api/v1/config/save', data) }

// ==================== 操作日志 ====================
export function getLogList(params: any) { return get('/api/v1/log/list', params) }
