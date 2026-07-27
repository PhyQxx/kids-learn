<template>
  <transition name="batch-bar">
    <section v-if="selectedCount > 0" class="admin-batch-bar" aria-live="polite">
      <div class="admin-batch-bar__summary">
        <el-icon><CircleCheckFilled /></el-icon>
        <span>已选择 <b>{{ selectedCount }}</b> 条{{ scope === 'all-results' ? '结果' : '本页数据' }}</span>
        <slot name="scope" />
      </div>
      <div class="admin-batch-bar__actions">
        <slot />
        <el-button link @click="emit('clear')">取消选择</el-button>
      </div>
    </section>
  </transition>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  selectedCount: number
  scope?: 'page' | 'all-results'
}>(), {
  scope: 'page',
})

const emit = defineEmits<{ clear: [] }>()
</script>

<style scoped>
.admin-batch-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-3) var(--space-4);
  margin-bottom: var(--space-3);
  color: var(--color-info-700);
  background: var(--color-info-50);
  border: 1px solid #b2ddff;
  border-radius: var(--radius-control);
}

.admin-batch-bar__summary,
.admin-batch-bar__actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.batch-bar-enter-active,
.batch-bar-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.batch-bar-enter-from,
.batch-bar-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

@media (max-width: 767px) {
  .admin-batch-bar {
    align-items: flex-start;
    flex-direction: column;
  }

  .admin-batch-bar__actions {
    width: 100%;
    overflow-x: auto;
  }
}
</style>

