import { get, post } from './request'

// 获取今日任务
export const getDailyTasks = () => get('/learn/daily-tasks')

// 获取学科列表
export const getSubjects = () => get('/learn/subjects')

// 获取课程列表 (分页)
export const getCourses = (subjectId, page = 1, pageSize = 20) =>
  get('/learn/courses', { subjectId, page, pageSize })

// 获取关卡列表
export const getLevels = (courseId) => get('/learn/levels', { courseId })

// 获取题目列表
export const getQuestions = (levelId) => get('/learn/questions', { levelId })

// 提交答案
export const submitAnswer = (data) => post('/learn/submit-answer', data)

// 完成关卡 (query params)
export const completeLevel = (levelId, totalScore, totalTime, wrongCount) =>
  post(`/learn/complete-level?levelId=${levelId}&totalScore=${totalScore}&totalTime=${totalTime}&wrongCount=${wrongCount}`)

// 获取学习记录 (date: 可选日期筛选)
export const getRecords = (date) => get('/learn/records', { date })

// 获取错题本
export const getWrongTopics = () => get('/learn/wrong-topics')
