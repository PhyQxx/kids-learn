import request from '@/utils/request'

// Helper: axios interceptor unwraps response to { code, msg, data }
const get = (url: string, params?: any): Promise<any> => request.get(url, { params })
const post = (url: string, data?: any, config?: any): Promise<any> => request.post(url, data, config)
const del = (url: string, config?: any): Promise<any> => request.delete(url, config)

// 登录
export function login(data: { username: string; password: string }) {
  return post('/auth/login', data)
}

// 获取统计数据
export function getDashboardStats() {
  return get('/admin/dashboard/stats')
}

// ==================== 学科管理 ====================
export function getSubjectList(params: any) { return get('/admin/subject/list', params) }
export function saveSubject(data: any) { return post('/admin/subject/save', data) }
export function deleteSubject(id: number) { return del(`/admin/subject/${id}`) }

// ==================== 课程管理 ====================
export function getCourseList(params: any) { return get('/admin/course/list', params) }
export function saveCourse(data: any) { return post('/admin/course/save', data) }
export function deleteCourse(id: number) { return del(`/admin/course/${id}`) }

// ==================== 关卡管理 ====================
export function getLevelList(params: any) { return get('/admin/level/list', params) }
export function saveLevel(data: any) { return post('/admin/level/save', data) }
export function deleteLevel(id: number) { return del(`/admin/level/${id}`) }

// ==================== 题目管理 ====================
export function getQuestionList(params: any) { return get('/admin/question/list', params) }
export function saveQuestion(data: any) { return post('/admin/question/save', data) }
export function deleteQuestion(id: number) { return del(`/admin/question/${id}`) }
export function getQuestionOptions(id: number) { return get(`/admin/question/${id}/options`) }

// ==================== 宠物管理 ====================
export function getPetList(params: any) { return get('/admin/pet/list', params) }
export function savePet(data: any) { return post('/admin/pet/save', data) }
export function deletePet(id: number) { return del(`/admin/pet/${id}`) }
export function getPetEvolutions(petId: number) { return get(`/admin/pet/${petId}/evolutions`) }
export function savePetEvolution(data: any) { return post('/admin/pet/evolution/save', data) }

// ==================== 道具管理 ====================
export function getPetItemList(params: any) { return get('/admin/pet-item/list', params) }
export function savePetItem(data: any) { return post('/admin/pet-item/save', data) }
export function deletePetItem(id: number) { return del(`/admin/pet-item/${id}`) }

// ==================== 装饰管理 ====================
export function getDecorationList(params: any) { return get('/admin/decoration/list', params) }
export function saveDecoration(data: any) { return post('/admin/decoration/save', data) }
export function deleteDecoration(id: number) { return del(`/admin/decoration/${id}`) }

// ==================== 成就管理 ====================
export function getAchievementList(params: any) { return get('/admin/achievement/list', params) }
export function saveAchievement(data: any) { return post('/admin/achievement/save', data) }
export function deleteAchievement(id: number) { return del(`/admin/achievement/${id}`) }

// ==================== 贴纸管理 ====================
export function getStickerList(params: any) { return get('/admin/sticker/list', params) }
export function saveSticker(data: any) { return post('/admin/sticker/save', data) }
export function deleteSticker(id: number) { return del(`/admin/sticker/${id}`) }
export function getStickerSeriesList() { return get('/admin/sticker-series/list') }

// ==================== 称号管理 ====================
export function getTitleList(params: any) { return get('/admin/title/list', params) }
export function saveTitle(data: any) { return post('/admin/title/save', data) }
export function deleteTitle(id: number) { return del(`/admin/title/${id}`) }

// ==================== 用户管理 ====================
export function getUserList(params: any) { return get('/admin/user/list', params) }
export function saveUser(data: any) { return post('/admin/user/save', data) }
export function updateUserStatus(id: number, status: number) {
  return post(`/admin/user/${id}/status`, null, { params: { status } })
}

// ==================== 角色管理 ====================
export function getRoleList() { return get('/admin/role/list') }
export function saveRole(data: any) { return post('/admin/role/save', data) }
export function deleteRole(id: number) { return del(`/admin/role/${id}`) }

// ==================== 系统配置 ====================
export function getConfigList(params: any) { return get('/admin/config/list', params) }
export function saveConfig(data: any) { return post('/admin/config/save', data) }

// ==================== 操作日志 ====================
export function getLogList(params: any) { return get('/admin/log/list', params) }

// ==================== 字典查询 ====================
export function getDictDataByType(dictType: string) { return get(`/admin/dict/data/${dictType}`) }

// ==================== 字典管理 ====================
export function getDictTypeList(params: any) { return get('/admin/dict/type/list', params) }
export function saveDictType(data: any) { return post('/admin/dict/type/save', data) }
export function deleteDictType(id: number) { return del(`/admin/dict/type/${id}`) }
export function getDictDataList(params: any) { return get('/admin/dict/data/list', params) }
export function saveDictData(data: any) { return post('/admin/dict/data/save', data) }
export function deleteDictData(id: number) { return del(`/admin/dict/data/${id}`) }
