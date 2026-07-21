<template>
  <AppLayout theme="learn" title="学习中心" active-nav="learn">
    <LearnContent />
  </AppLayout>
</template>

<script setup>
import AppLayout from '@/components/AppLayout.vue'
import LearnContent from '@/components/learn/LearnContent.vue'
</script>
