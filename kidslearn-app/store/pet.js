import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const usePetStore = defineStore('pet', () => {
  const petInfo = ref(null)

  const name = computed(() => petInfo.value?.petName || '小星')
  const level = computed(() => petInfo.value?.currentLevel || 1)
  const hunger = computed(() => petInfo.value?.hunger ?? 80)
  const mood = computed(() => petInfo.value?.mood ?? 3)
  const energy = computed(() => petInfo.value?.energy ?? 100)
  const expInLevel = computed(() => petInfo.value?.expInCurrentLevel ?? 0)
  const nextLevelExp = computed(() => petInfo.value?.nextLevelExp ?? 50)
  const moodText = computed(() => {
    const map = { 1: '难过', 2: '一般', 3: '开心', 4: '兴奋' }
    return map[petInfo.value?.mood] || '开心'
  })
  const moodPercent = computed(() => ((petInfo.value?.mood ?? 3) / 4) * 100)
  const wearDecorationIds = computed(() => petInfo.value?.wearDecorationIds || [])
  const evolutionName = computed(() => petInfo.value?.evolutionName || '')
  const currentImageUrl = computed(() => petInfo.value?.currentImageUrl || '🐱')

  function setPetInfo(info) {
    petInfo.value = info
  }

  return {
    petInfo, name, level, hunger, mood, energy,
    expInLevel, nextLevelExp, moodText, moodPercent,
    wearDecorationIds, evolutionName, currentImageUrl,
    setPetInfo
  }
})
