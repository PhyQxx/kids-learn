<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">关卡管理</span>
        <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B">新增关卡</el-button>
      </div>
    </template>
    <el-table :data="[]" stripe>
      <el-table-column prop="levelNum" label="序号" width="80" />
      <el-table-column prop="levelName" label="关卡名称" />
      <el-table-column prop="totalQuestions" label="题目数" />
      <el-table-column prop="passScore" label="及格分" />
      <el-table-column prop="expReward" label="经验奖励" />
      <el-table-column prop="goldReward" label="金币奖励" />
      <el-table-column label="操作" width="180">
        <template #default><el-button link type="primary">编辑</el-button><el-button link type="danger">删除</el-button></template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
<script setup lang="ts"></script>
