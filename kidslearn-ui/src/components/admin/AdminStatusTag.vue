<template>
  <span class="admin-status-tag" :class="`is-${tone}`">
    <el-icon v-if="icon"><component :is="icon" /></el-icon>
    <span>{{ label }}</span>
  </span>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

withDefaults(defineProps<{
  label: string
  tone?: 'neutral' | 'info' | 'success' | 'warning' | 'danger'
  icon?: Component
}>(), {
  tone: 'neutral',
  icon: undefined,
})
</script>

<style scoped>
.admin-status-tag {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  min-height: 24px;
  padding: 2px 8px;
  font-size: var(--font-size-caption);
  font-weight: 600;
  line-height: var(--line-height-caption);
  border: 1px solid transparent;
  border-radius: var(--radius-pill);
}

.is-neutral { color: var(--color-gray-600); background: var(--color-gray-100); border-color: var(--color-gray-200); }
.is-info { color: var(--color-info-700); background: var(--color-info-50); border-color: #b2ddff; }
.is-success { color: var(--color-success-600); background: var(--color-success-50); border-color: #abefc6; }
.is-warning { color: var(--color-warning-700); background: var(--color-warning-50); border-color: #fedf89; }
.is-danger { color: var(--color-danger-600); background: var(--color-danger-50); border-color: #fecdca; }
</style>

