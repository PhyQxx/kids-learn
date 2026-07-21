<template>
  <AppLayout theme="kids" title="我的宠物" active-nav="pet">
    <PetContent />
  </AppLayout>
</template>

<script setup>
import AppLayout from '@/components/AppLayout.vue'
import PetContent from '@/components/pet/PetContent.vue'
</script>
