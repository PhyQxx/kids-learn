<template>
  <section class="admin-filter-bar" aria-label="筛选条件">
    <div class="admin-filter-bar__primary">
      <slot />
      <el-button
        v-if="collapsible && $slots.advanced"
        text
        class="admin-filter-bar__toggle"
        @click="expanded = !expanded"
      >
        更多筛选
        <span v-if="activeCount" class="admin-filter-bar__badge">{{ activeCount }}</span>
        <el-icon><ArrowUp v-if="expanded" /><ArrowDown v-else /></el-icon>
      </el-button>
    </div>

    <div class="admin-filter-bar__actions">
      <el-button @click="emit('reset')">重置</el-button>
      <el-button type="primary" :loading="loading" @click="emit('search')">查询</el-button>
    </div>

    <div v-if="expanded && $slots.advanced" class="admin-filter-bar__advanced">
      <slot name="advanced" />
    </div>

    <div v-if="$slots.chips" class="admin-filter-bar__chips">
      <slot name="chips" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'

withDefaults(defineProps<{
  collapsible?: boolean
  activeCount?: number
  loading?: boolean
}>(), {
  collapsible: true,
  activeCount: 0,
  loading: false,
})

const emit = defineEmits<{
  search: []
  reset: []
}>()

const expanded = ref(false)
</script>

<style scoped>
.admin-filter-bar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: var(--space-3) var(--space-4);
  padding: var(--space-4);
  margin-bottom: var(--space-4);
  background: var(--admin-surface);
  border: 1px solid var(--admin-border);
  border-radius: var(--radius-card);
}

.admin-filter-bar__primary,
.admin-filter-bar__actions,
.admin-filter-bar__advanced,
.admin-filter-bar__chips {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.admin-filter-bar__actions {
  justify-content: flex-end;
}

.admin-filter-bar__advanced,
.admin-filter-bar__chips {
  grid-column: 1 / -1;
  padding-top: var(--space-3);
  border-top: 1px solid var(--admin-border);
}

.admin-filter-bar__badge {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  color: var(--color-brand-700);
  font-size: var(--font-size-caption);
  line-height: 20px;
  background: var(--color-brand-100);
  border-radius: var(--radius-pill);
}

@media (max-width: 767px) {
  .admin-filter-bar {
    grid-template-columns: 1fr;
  }

  .admin-filter-bar__actions {
    justify-content: stretch;
  }

  .admin-filter-bar__actions :deep(.el-button) {
    flex: 1;
  }
}
</style>

