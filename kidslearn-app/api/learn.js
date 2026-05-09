import { get, post } from './request'

// 获取今日任务
export const getDailyTasks = () => get('/learn/daily-tasks')

// 获取学科列表 (gradeLevelId: 年级ID，可选)
export const getSubjects = (gradeLevelId) => get('/learn/subjects', { gradeLevelId })

// 获取关卡列表
export const getLevels = (subjectId, gradeLevelId) => get('/learn/levels', { subjectId, gradeLevelId })

export const getCourseVideos = (courseId) => get('/learn/videos', { courseId })

export const submitVideoProgress = (data) => post('/learn/video-progress', data)

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

// --- Phase 12: 专项练习与智能错题本 ---

// 获取专项练习模式列表
export const getPracticeModes = (subjectId) => get('/learn/practice/modes', { subjectId })

// 开始专项练习
export const startPractice = (practiceModeId) => post(`/learn/practice/start?practiceModeId=${practiceModeId}`)

// 提交专项练习答案
export const submitPracticeAnswer = (practiceSessionId, data) => 
  post(`/learn/practice/submit?practiceSessionId=${practiceSessionId}`, data)

// 获取智能复习组卷
export const getSmartReviewQuiz = (subjectId, questionCount = 15) => 
  get('/learn/review/smart-quiz', { subjectId, questionCount })

// 更新错题掌握度
export const updateWrongTopicMastery = (wrongTopicId, isCorrect) => 
  post(`/learn/review/mastery?wrongTopicId=${wrongTopicId}&isCorrect=${isCorrect}`)
