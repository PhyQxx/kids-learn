<template>
  <AppLayout theme="kids" title="成就中心" :show-back="true" active-nav="achievement">
    <AchievementContent />
  </AppLayout>
</template>

<script setup>
import AppLayout from '@/components/AppLayout.vue'
import AchievementContent from '@/components/achievement/AchievementContent.vue'
</script>
