<template>
  <header class="admin-page-header">
    <div class="admin-page-header__main">
      <el-button
        v-if="backTo"
        class="admin-page-header__back"
        text
        circle
        aria-label="返回上一页"
        @click="router.push(backTo)"
      >
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <div class="admin-page-header__copy">
        <div class="admin-page-header__title-row">
          <h1>{{ title }}</h1>
          <span v-if="count !== undefined" class="admin-page-header__count">{{ count }}</span>
          <slot name="meta" />
        </div>
        <p v-if="description">{{ description }}</p>
      </div>
    </div>

    <div v-if="$slots.primary || $slots.secondary" class="admin-page-header__actions">
      <div v-if="$slots.secondary" class="admin-page-header__secondary">
        <slot name="secondary" />
      </div>
      <slot name="primary" />
    </div>
  </header>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

defineProps<{
  title: string
  description?: string
  count?: number | string
  backTo?: string
}>()

const router = useRouter()
</script>

<style scoped>
.admin-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-6);
  margin-bottom: var(--space-6);
}

.admin-page-header__main,
.admin-page-header__title-row,
.admin-page-header__actions,
.admin-page-header__secondary {
  display: flex;
  align-items: center;
}

.admin-page-header__main {
  gap: var(--space-3);
  min-width: 0;
}

.admin-page-header__copy {
  min-width: 0;
}

.admin-page-header__title-row {
  gap: var(--space-2);
  flex-wrap: wrap;
}

h1 {
  margin: 0;
  color: var(--admin-text);
  font-size: var(--font-size-heading-1);
  font-weight: 600;
  line-height: var(--line-height-heading-1);
}

p {
  margin: var(--space-1) 0 0;
  color: var(--admin-muted);
  font-size: var(--font-size-body);
  line-height: var(--line-height-body);
}

.admin-page-header__count {
  min-width: 24px;
  padding: 1px 8px;
  color: var(--color-gray-600);
  font-size: var(--font-size-caption);
  line-height: 20px;
  text-align: center;
  background: var(--color-gray-100);
  border-radius: var(--radius-pill);
}

.admin-page-header__actions,
.admin-page-header__secondary {
  gap: var(--space-2);
  flex-shrink: 0;
}

@media (max-width: 767px) {
  .admin-page-header {
    flex-direction: column;
    gap: var(--space-4);
  }

  .admin-page-header__actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>

