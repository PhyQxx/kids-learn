<template>
  <el-card>
    <template #header><span style="font-weight:700">系统配置</span></template>
    <el-table :data="[]" stripe>
      <el-table-column prop="configKey" label="配置键" />
      <el-table-column prop="configValue" label="配置值" show-overflow-tooltip />
      <el-table-column prop="configType" label="类型" />
      <el-table-column prop="description" label="描述" />
      <el-table-column label="操作" width="120">
        <template #default><el-button link type="primary">编辑</el-button></template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
<script setup lang="ts"></script>
