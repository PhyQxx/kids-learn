<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">学科管理</span>
        <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B">新增学科</el-button>
      </div>
    </template>
    <el-table :data="[]" stripe>
      <el-table-column prop="subjectCode" label="学科代码" />
      <el-table-column prop="subjectName" label="学科名称" />
      <el-table-column prop="color" label="主题色" />
      <el-table-column prop="sortOrder" label="排序" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作" width="180">
        <template #default><el-button link type="primary">编辑</el-button><el-button link type="danger">删除</el-button></template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
<script setup lang="ts"></script>
