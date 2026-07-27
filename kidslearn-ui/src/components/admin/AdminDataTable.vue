<template>
  <section class="admin-data-table" :class="`is-${density}`">
    <div v-if="error" class="admin-data-table__state">
      <AdminEmptyState type="error" :description="error">
        <template #action><el-button type="primary" plain @click="emit('retry')">重新加载</el-button></template>
      </AdminEmptyState>
    </div>

    <el-table
      v-else
      :data="rows"
      :row-key="rowKey"
      :loading="loading"
      stripe
      @selection-change="emit('selectionChange', $event)"
      @sort-change="emit('sortChange', $event)"
    >
      <el-table-column v-if="selectable" type="selection" width="48" reserve-selection />
      <el-table-column
        v-for="column in visibleColumns"
        :key="column.key"
        :prop="column.prop || column.key"
        :label="column.label"
        :width="column.width"
        :min-width="column.minWidth"
        :fixed="column.fixed"
        :align="column.align"
        :sortable="column.sortable"
        :show-overflow-tooltip="column.overflowTooltip"
      >
        <template #default="scope">
          <slot
            v-if="column.slot"
            :name="column.slot"
            :row="scope.row"
            :column="column"
            :index="scope.$index"
          />
          <template v-else>{{ displayValue(scope.row, column.prop || column.key) }}</template>
        </template>
      </el-table-column>
      <el-table-column v-if="$slots.actions" label="操作" :width="actionsWidth" fixed="right">
        <template #default="scope"><slot name="actions" :row="scope.row" :index="scope.$index" /></template>
      </el-table-column>
      <template #empty>
        <AdminEmptyState :type="filtered ? 'filtered' : 'empty'">
          <template v-if="$slots.emptyAction" #action><slot name="emptyAction" /></template>
        </AdminEmptyState>
      </template>
    </el-table>

    <footer v-if="!error && total > 0" class="admin-data-table__footer">
      <span class="admin-data-table__summary">共 {{ total }} 条</span>
      <el-pagination
        :current-page="page"
        :page-size="pageSize"
        :page-sizes="pageSizes"
        :total="total"
        layout="sizes, prev, pager, next"
        @update:current-page="emit('update:page', $event)"
        @update:page-size="emit('update:pageSize', $event)"
      />
    </footer>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AdminEmptyState from './AdminEmptyState.vue'

export type AdminTableColumn = {
  key: string
  label: string
  prop?: string
  width?: number | string
  minWidth?: number | string
  fixed?: boolean | 'left' | 'right'
  align?: 'left' | 'center' | 'right'
  sortable?: boolean | 'custom'
  overflowTooltip?: boolean
  visible?: boolean
  slot?: string
}

const props = withDefaults(defineProps<{
  columns: AdminTableColumn[]
  rows: Record<string, any>[]
  rowKey?: string | ((row: Record<string, any>) => string)
  loading?: boolean
  error?: string
  filtered?: boolean
  selectable?: boolean
  density?: 'comfortable' | 'compact'
  actionsWidth?: number | string
  total?: number
  page?: number
  pageSize?: number
  pageSizes?: number[]
}>(), {
  rowKey: 'id',
  loading: false,
  error: '',
  filtered: false,
  selectable: false,
  density: 'comfortable',
  actionsWidth: 140,
  total: 0,
  page: 1,
  pageSize: 20,
  pageSizes: () => [20, 50, 100],
})

const emit = defineEmits<{
  retry: []
  selectionChange: [rows: Record<string, any>[]]
  sortChange: [payload: Record<string, any>]
  'update:page': [page: number]
  'update:pageSize': [size: number]
}>()

const visibleColumns = computed(() => props.columns.filter(column => column.visible !== false))

function displayValue(row: Record<string, any>, path: string) {
  const value = path.split('.').reduce<any>((current, key) => current?.[key], row)
  return value === null || value === undefined || value === '' ? '—' : value
}
</script>

<style scoped>
.admin-data-table { min-width: 0; }
.admin-data-table__state { background: var(--admin-surface); border: 1px solid var(--admin-border); border-radius: var(--radius-card); }
.admin-data-table__footer { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); }
.admin-data-table__summary { color: var(--admin-muted); font-size: var(--font-size-caption); }
.is-compact :deep(.el-table td.el-table__cell) { height: 40px; }

@media (max-width: 767px) {
  .admin-data-table__footer { align-items: flex-start; flex-direction: column; }
  .admin-data-table__footer :deep(.el-pagination) { width: 100%; justify-content: flex-start; overflow-x: auto; }
}
</style>
