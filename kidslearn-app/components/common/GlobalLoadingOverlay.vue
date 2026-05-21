<template>
  <FunLoadingState
    v-if="loadingState.show"
    :title="loadingState.title"
    :mascot="loadingState.mascot"
    full-screen
  />
</template>

<script setup>
import { watch } from 'vue'
import { loadingState } from '@/utils/loading'
import FunLoadingState from './FunLoadingState.vue'
import { soundManager } from '@/utils/sound'

watch(() => loadingState.show, (show) => {
  if (show) {
    soundManager.play('popup')
  }
})
</script>
