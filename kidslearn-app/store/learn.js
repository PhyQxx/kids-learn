import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useLearnStore = defineStore('learn', () => {
  const currentSubject = ref(null)
  const currentCourse = ref(null)
  const currentLevel = ref(null)
  const quizQuestions = ref([])
  const quizResults = ref(null)
  const dailyTasks = ref([])

  function setSubject(subject) { currentSubject.value = subject }
  function setCourse(course) { currentCourse.value = course }
  function setLevel(level) { currentLevel.value = level }
  function setQuestions(questions) { quizQuestions.value = questions }
  function setResults(results) { quizResults.value = results }
  function setDailyTasks(tasks) { dailyTasks.value = tasks }

  function clearQuiz() {
    quizQuestions.value = []
    quizResults.value = null
  }

  return {
    currentSubject, currentCourse, currentLevel,
    quizQuestions, quizResults, dailyTasks,
    setSubject, setCourse, setLevel, setQuestions, setResults, setDailyTasks,
    clearQuiz
  }
})
