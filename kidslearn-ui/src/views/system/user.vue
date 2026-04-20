<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">用户管理</span>
        <el-input placeholder="搜索用户" style="width:240px" />
      </div>
    </template>
    <el-table :data="[]" stripe>
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="userType" label="类型" />
      <el-table-column prop="level" label="等级" />
      <el-table-column prop="gold" label="金币" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作" width="180">
        <template #default><el-button link type="primary">查看</el-button><el-button link type="danger">禁用</el-button></template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
<script setup lang="ts"></script>
