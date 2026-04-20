<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">称号管理</span>
        <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B">新增称号</el-button>
      </div>
    </template>
    <el-table :data="[]" stripe>
      <el-table-column prop="titleCode" label="称号代码" />
      <el-table-column prop="titleName" label="称号名称" />
      <el-table-column prop="obtainType" label="获取方式" />
      <el-table-column prop="isTimed" label="限时" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作" width="180">
        <template #default><el-button link type="primary">编辑</el-button><el-button link type="danger">删除</el-button></template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
<script setup lang="ts"></script>
