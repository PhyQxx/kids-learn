import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const usePetStore = defineStore('pet', () => {
  const petInfo = ref(null)

  const name = computed(() => petInfo.value?.name || '小星')
  const level = computed(() => petInfo.value?.currentLevel || 1)
  const hunger = computed(() => petInfo.value?.hunger || 80)
  const mood = computed(() => petInfo.value?.mood || 3)
  const moodText = computed(() => {
    const map = { 1: '难过', 2: '一般', 3: '开心', 4: '兴奋' }
    return map[petInfo.value?.mood] || '开心'
  })

  function setPetInfo(info) {
    petInfo.value = info
  }

  return { petInfo, name, level, hunger, mood, moodText, setPetInfo }
})
