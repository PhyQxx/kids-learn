<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">课程管理</span>
        <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B">新增课程</el-button>
      </div>
    </template>
    <el-table :data="[]" stripe>
      <el-table-column prop="courseName" label="课程名称" />
      <el-table-column prop="subjectName" label="所属学科" />
      <el-table-column prop="difficulty" label="难度" />
      <el-table-column prop="totalLevels" label="关卡数" />
      <el-table-column prop="isElite" label="精英关" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作" width="240">
        <template #default><el-button link type="primary">编辑</el-button><el-button link type="primary">关卡</el-button><el-button link type="danger">删除</el-button></template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
<script setup lang="ts"></script>
