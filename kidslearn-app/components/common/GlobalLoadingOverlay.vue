<template>
  <FunLoadingState
    v-if="loadingState.show"
    :title="loadingState.title"
    :mascot="loadingState.mascot"
    full-screen
  />
</template>

<script setup>
import { loadingState } from '@/utils/loading'
import FunLoadingState from './FunLoadingState.vue'
</script>
