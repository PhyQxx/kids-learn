<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">角色管理</span>
        <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B">新增角色</el-button>
      </div>
    </template>
    <el-table :data="[]" stripe>
      <el-table-column prop="roleName" label="角色名称" />
      <el-table-column prop="roleCode" label="角色代码" />
      <el-table-column prop="description" label="描述" />
      <el-table-column label="操作" width="180">
        <template #default><el-button link type="primary">编辑</el-button><el-button link type="danger">删除</el-button></template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
<script setup lang="ts"></script>
