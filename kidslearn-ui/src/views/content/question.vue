<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">题目管理</span>
        <div>
          <el-button>批量导入</el-button>
          <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B">新增题目</el-button>
        </div>
      </div>
    </template>
    <el-table :data="[]" stripe>
      <el-table-column prop="questionType" label="题型" />
      <el-table-column prop="questionContent" label="题目内容" show-overflow-tooltip />
      <el-table-column prop="difficulty" label="难度" />
      <el-table-column prop="score" label="分值" />
      <el-table-column prop="timeLimit" label="限时(秒)" />
      <el-table-column label="操作" width="180">
        <template #default><el-button link type="primary">编辑</el-button><el-button link type="danger">删除</el-button></template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
<script setup lang="ts"></script>
