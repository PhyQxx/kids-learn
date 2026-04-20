<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">宠物管理</span>
        <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B">新增宠物</el-button>
      </div>
    </template>
    <el-table :data="[]" stripe>
      <el-table-column prop="petCode" label="宠物代码" />
      <el-table-column prop="petName" label="宠物名称" />
      <el-table-column prop="petType" label="类型" />
      <el-table-column prop="unlockCost" label="解锁价格" />
      <el-table-column prop="isDefault" label="默认宠物" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作" width="240">
        <template #default><el-button link type="primary">编辑</el-button><el-button link type="primary">进化配置</el-button><el-button link type="danger">删除</el-button></template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
<script setup lang="ts"></script>
