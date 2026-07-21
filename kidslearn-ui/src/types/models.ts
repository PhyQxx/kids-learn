/** 通用分页查询参数 */
export interface PageQuery {
  page?: number
  pageSize?: number
}

/** 通用分页结果 */
export interface PageResult<T> {
  list: T[]
  total: number
}

/** 通用API响应 */
export interface ApiEnvelope<T = unknown> {
  code: number
  msg: string
  data: T
}

/** 学科 */
export interface Subject {
  id: number
  subjectName: string
  subjectIcon: string
  subjectCode: string
  description: string
  sortOrder: number
  status: number
}

/** 年级 */
export interface GradeLevel {
  id: number
  levelName: string
  levelCode: string
  ageGroup: number
  sortOrder: number
  status: number
}

/** 关卡 */
export interface CourseLevel {
  id: number
  subjectId: number
  levelNum: number
  levelName: string
  levelDesc: string
  baseQuestionCount: number
  advancedQuestionCount: number
  passScore: number
  starThresholds: string
  expReward: number
  goldReward: number
  status: number
}

/** 题目 */
export interface Question {
  id: number
  subjectId: number
  gradeLevelId: number
  questionType: number
  questionContent: string
  difficulty: number
  score: number
  analysis: string
  status: number
}

/** 题目选项 */
export interface QuestionOption {
  id: number
  questionId: number
  optionLabel: string
  optionContent: string
  isCorrect: number
  sortOrder: number
}

/** 用户 */
export interface User {
  id: number
  username: string
  nickname: string
  avatar: string
  userType: number
  level: number
  gold: number
  diamond: number
  totalExp: number
  status: number
  realName: string
  phone: string
  createTime: string
}

/** 宠物 */
export interface Pet {
  id: number
  petName: string
  petCode: string
  description: string
  baseImageUrl: string
  isDefault: number
  status: number
}

/** 宠物进化 */
export interface PetEvolution {
  id: number
  petId: number
  evolveLevel: number
  imageUrl: string
  description: string
}

/** 成就 */
export interface Achievement {
  id: number
  achieveCode: string
  achieveName: string
  achieveDesc: string
  achieveIcon: string
  achieveType: number
  isTiered: number
  sortOrder: number
  status: number
}

/** 贴纸 */
export interface Sticker {
  id: number
  stickerName: string
  seriesId: number
  rarity: number
  imageUrl: string
  description: string
  status: number
}

/** 称号 */
export interface Title {
  id: number
  titleName: string
  titleIcon: string
  description: string
  conditionType: string
  conditionValue: number
  status: number
}

/** 系统配置 */
export interface SystemConfig {
  id: number
  configKey: string
  configValue: string
  configType: number
  description: string
}

/** 字典类型 */
export interface DictType {
  id: number
  dictType: string
  dictName: string
  description: string
  status: number
}

/** 字典数据 */
export interface DictData {
  id: number
  typeId: number
  dictLabel: string
  dictValue: string
  sortOrder: number
  status: number
}

/** 角色 */
export interface Role {
  id: number
  roleName: string
  roleCode: string
  permissions: string
  description: string
  status: number
}

/** 挑战赛 */
export interface Challenge {
  id: number
  challengeName: string
  challengeType: string
  subjectId: number
  subjectName: string
  timeLimitSeconds: number
  questionCount: number
  rewardGold: number
  rewardExp: number
  description: string
  status: number
}

/** 订单 */
export interface Order {
  id: number
  orderNo: string
  userId: number
  username: string
  planName: string
  amount: number
  status: number
  payChannel: string
  payTime: string
  createTime: string
  remark: string
}

/** 排行榜条目 */
export interface RankingItem {
  id: number
  nickname: string
  avatar: string
  score: number
  rank: number
  isMe: boolean
}

/** 通知 */
export interface Notification {
  id: number
  type: string
  title: string
  content: string
  isRead: number
  createTime: string
}
