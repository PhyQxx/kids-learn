<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">成就管理</span>
        <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B">新增成就</el-button>
      </div>
    </template>
    <el-table :data="[]" stripe>
      <el-table-column prop="achieveCode" label="成就代码" />
      <el-table-column prop="achieveName" label="成就名称" />
      <el-table-column prop="achieveType" label="类型" />
      <el-table-column prop="isTiered" label="分级" />
      <el-table-column prop="sortOrder" label="排序" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作" width="240">
        <template #default><el-button link type="primary">编辑</el-button><el-button link type="primary">条件配置</el-button><el-button link type="danger">删除</el-button></template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
<script setup lang="ts"></script>
