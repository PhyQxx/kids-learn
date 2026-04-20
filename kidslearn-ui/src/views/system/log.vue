<template>
  <el-card>
    <template #header><span style="font-weight:700">操作日志</span></template>
    <el-table :data="[]" stripe>
      <el-table-column prop="adminUserId" label="操作人" />
      <el-table-column prop="module" label="模块" />
      <el-table-column prop="action" label="操作" />
      <el-table-column prop="targetType" label="对象类型" />
      <el-table-column prop="ip" label="IP" />
      <el-table-column prop="createTime" label="操作时间" />
    </el-table>
  </el-card>
</template>
<script setup lang="ts"></script>
