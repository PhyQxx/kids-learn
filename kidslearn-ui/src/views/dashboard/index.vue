<template>
  <div class="dashboard">
    <h2>首页概览</h2>
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="6" v-for="item in stats" :key="item.label">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" :style="{ background: item.color }">{{ item.icon }}</div>
            <div class="stat-info">
              <div class="stat-value">{{ item.value }}</div>
              <div class="stat-label">{{ item.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
const stats = [
  { icon: '👨‍👩‍👧‍👦', label: '注册用户', value: '1,234', color: '#FF6B6B' },
  { icon: '📚', label: '课程数量', value: '56', color: '#4ECDC4' },
  { icon: '🎯', label: '题目数量', value: '2,340', color: '#FFE66D' },
  { icon: '📊', label: '今日学习人次', value: '892', color: '#A78BFA' },
]
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #333;
}

.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 2px;
}
</style>
