import { get, post } from './request'

// 获取今日任务
export const getDailyTasks = () => get('/learn/daily-tasks')

// 获取学科列表 (gradeLevelId: 年级ID，可选)
export const getSubjects = (gradeLevelId) => get('/learn/subjects', { gradeLevelId })

// 获取课程列表 (分页)
export const getCourses = (subjectId, gradeLevelId, page = 1, pageSize = 20) =>
  get('/learn/courses', { subjectId, gradeLevelId, page, pageSize })

// 获取关卡列表
export const getLevels = (courseId, gradeLevelId) => get('/learn/levels', { courseId, gradeLevelId })

// 获取题目列表
export const getQuestions = (levelId, gradeLevelId) => get('/learn/questions', { levelId, gradeLevelId })

// 提交答案
export const submitAnswer = (data) => post('/learn/submit-answer', data)

// 完成关卡 (query params)
export const completeLevel = (levelId, totalScore, totalTime, wrongCount) =>
  post(`/learn/complete-level?levelId=${levelId}&totalScore=${totalScore}&totalTime=${totalTime}&wrongCount=${wrongCount}`)

// 获取学习记录 (date: 可选日期筛选)
export const getRecords = (date) => get('/learn/records', { date })

// 获取错题本
export const getWrongTopics = () => get('/learn/wrong-topics')

// 每日签到
export const postCheckin = () => post('/learn/checkin')

// 获取签到状态
export const getCheckinStatus = () => get('/learn/checkin/status')

// 宠物提示技能 (排除2个错误选项)
export const getHint = (questionId) => post(`/learn/hint?questionId=${questionId}`)

// 获取薄弱点推荐
export const getWeakPoints = () => get('/learn/weak-points')

// 获取自适应题目 (subjectId: 可选学科ID)
export const getAdaptiveQuestions = (subjectId) => get('/learn/adaptive-questions', { subjectId })

// 错题重做
export const retryWrong = (questionId, answer) =>
  post(`/learn/retry-wrong?questionId=${questionId}&answer=${encodeURIComponent(answer)}`)

// 错题AI讲解
export const getExplainWrong = (questionId) => get('/learn/explain-wrong', { questionId })

// 获取新手测评题目
export const getAssessmentQuestions = () => get('/learn/assessment')
