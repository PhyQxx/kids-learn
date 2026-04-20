<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">装饰管理</span>
        <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B">新增装饰</el-button>
      </div>
    </template>
    <el-table :data="[]" stripe>
      <el-table-column prop="decoCode" label="装饰代码" />
      <el-table-column prop="decoName" label="装饰名称" />
      <el-table-column prop="slot" label="部位" />
      <el-table-column prop="priceGold" label="金币价格" />
      <el-table-column prop="rarity" label="稀有度" />
      <el-table-column prop="isLimited" label="限定" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作" width="240">
        <template #default><el-button link type="primary">编辑</el-button><el-button link type="primary">预览</el-button><el-button link type="danger">删除</el-button></template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
<script setup lang="ts"></script>
