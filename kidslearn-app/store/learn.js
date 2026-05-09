import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useLearnStore = defineStore('learn', () => {
  const currentSubject = ref(null)
  const currentLevel = ref(null)
  const quizQuestions = ref([])
  const quizResults = ref(null)
  const dailyTasks = ref([])

  function setSubject(subject) { currentSubject.value = subject }
  function setLevel(level) { currentLevel.value = level }
  function setQuestions(questions) { quizQuestions.value = questions }
  function setResults(results) { quizResults.value = results }
  function setDailyTasks(tasks) { dailyTasks.value = tasks }

  function clearLearningContext() {
    currentSubject.value = null
    currentLevel.value = null
    quizQuestions.value = []
    quizResults.value = null
  }

  function clearQuiz() {
    quizQuestions.value = []
    quizResults.value = null
  }

  return {
    currentSubject, currentLevel,
    quizQuestions, quizResults, dailyTasks,
    setSubject, setLevel, setQuestions, setResults, setDailyTasks,
    clearLearningContext, clearQuiz
  }
})
