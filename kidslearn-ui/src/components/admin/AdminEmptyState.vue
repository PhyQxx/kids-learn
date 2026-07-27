<template>
  <section class="admin-empty-state" :class="`is-${type}`" role="status">
    <div class="admin-empty-state__icon" aria-hidden="true">
      <el-icon><component :is="iconByType" /></el-icon>
    </div>
    <h3>{{ title || copy.title }}</h3>
    <p>{{ description || copy.description }}</p>
    <div v-if="$slots.action" class="admin-empty-state__action"><slot name="action" /></div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Box, Filter, RefreshRight, Lock } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  type?: 'empty' | 'filtered' | 'error' | 'forbidden'
  title?: string
  description?: string
}>(), {
  type: 'empty',
  title: '',
  description: '',
})

const copies = {
  empty: { title: '暂无数据', description: '这里还没有内容，可以从创建第一条数据开始。' },
  filtered: { title: '没有匹配结果', description: '尝试调整或清除当前筛选条件。' },
  error: { title: '加载失败', description: '数据暂时无法加载，请检查网络后重试。' },
  forbidden: { title: '暂无访问权限', description: '当前账号缺少访问此内容所需的权限。' },
}

const iconByType = computed(() => ({ empty: Box, filtered: Filter, error: RefreshRight, forbidden: Lock })[props.type])
const copy = computed(() => copies[props.type])
</script>

<style scoped>
.admin-empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  min-height: 280px;
  padding: var(--space-8);
  text-align: center;
}

.admin-empty-state__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  margin-bottom: var(--space-4);
  color: var(--color-gray-500);
  font-size: 24px;
  background: var(--color-gray-100);
  border-radius: 50%;
}

h3 { margin: 0; font-size: var(--font-size-heading-2); line-height: var(--line-height-heading-2); }
p { max-width: 420px; margin: var(--space-2) 0 0; color: var(--admin-muted); }
.admin-empty-state__action { margin-top: var(--space-4); }
.is-error .admin-empty-state__icon { color: var(--color-danger-600); background: var(--color-danger-50); }
.is-forbidden .admin-empty-state__icon { color: var(--color-warning-700); background: var(--color-warning-50); }
</style>

