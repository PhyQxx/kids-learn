import { get, post } from './request'

// 获取今日任务
export const getDailyTasks = () => get('/learn/daily-tasks')

// 获取学科列表
export const getSubjects = () => get('/learn/subjects')

// 获取课程列表
export const getCourses = (subjectId, gradeLevelId) =>
  get('/learn/courses', { subjectId, gradeLevelId })

// 获取关卡列表
export const getLevels = (courseId) => get('/learn/levels', { courseId })

// 获取题目列表
export const getQuestions = (levelId) => get('/learn/questions', { levelId })

// 提交答案
export const submitAnswer = (data) => post('/learn/submit-answer', data)

// 完成关卡
export const completeLevel = (data) => post('/learn/complete-level', data)

// 获取学习记录
export const getRecords = (params) => get('/learn/records', params)

// 获取错题本
export const getWrongTopics = (params) => get('/learn/wrong-topics', params)
