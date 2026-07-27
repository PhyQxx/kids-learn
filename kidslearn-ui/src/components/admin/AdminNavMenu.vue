<template>
  <el-menu
    :default-active="activePath"
    :collapse="collapsed"
    :unique-opened="true"
    router
    class="admin-nav-menu"
    @select="emit('select')"
  >
    <template v-for="group in groups" :key="group.key">
      <el-menu-item v-if="group.direct && group.items[0]" :index="group.items[0].path">
        <el-icon><component :is="group.icon" /></el-icon>
        <template #title>{{ group.label }}</template>
      </el-menu-item>
      <el-sub-menu v-else :index="group.key">
        <template #title>
          <el-icon><component :is="group.icon" /></el-icon>
          <span>{{ group.label }}</span>
        </template>
        <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
          {{ item.label }}
        </el-menu-item>
      </el-sub-menu>
    </template>
  </el-menu>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

export type AdminNavItem = {
  label: string
  path: string
  permission: string
  keywords?: string[]
}

export type AdminNavGroup = {
  key: string
  label: string
  icon: Component
  items: AdminNavItem[]
  direct?: boolean
}

withDefaults(defineProps<{
  groups: AdminNavGroup[]
  activePath: string
  collapsed?: boolean
}>(), {
  collapsed: false,
})

const emit = defineEmits<{ select: [] }>()
</script>

<style scoped>
.admin-nav-menu {
  width: 100%;
  padding: var(--space-2);
  border-right: 0;
}

.admin-nav-menu:not(.el-menu--collapse) {
  width: 100%;
}
</style>

