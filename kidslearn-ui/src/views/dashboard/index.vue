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
import { ref, onMounted } from 'vue'
import { getDashboardStats } from '@/api/request'

const stats = ref([
  { icon: '👨‍👩‍👧‍👦', label: '注册用户', value: '0', color: '#FF6B6B' },
  { icon: '📚', label: '课程数量', value: '0', color: '#4ECDC4' },
  { icon: '🎯', label: '订单数量', value: '0', color: '#FFE66D' },
  { icon: '📊', label: '今日活跃', value: '0', color: '#A78BFA' },
])

async function fetchStats() {
  try {
    const res = await getDashboardStats()
    if (res.code === 200) {
      const d = res.data
      stats.value[0].value = String(d.totalUsers || 0)
      stats.value[1].value = String(d.totalCourses || 0)
      stats.value[2].value = String(d.totalOrders || 0)
      stats.value[3].value = String(d.todayActiveUsers || 0)
    }
  } catch (e) { /* use defaults */ }
}

onMounted(() => fetchStats())
</script>

<style scoped>
.stat-card { display: flex; align-items: center; gap: 16px; }
.stat-icon { width: 56px; height: 56px; border-radius: 14px; display: flex; align-items: center; justify-content: center; font-size: 28px; }
.stat-value { font-size: 24px; font-weight: 700; color: #333; }
.stat-label { font-size: 13px; color: #999; margin-top: 2px; }
</style>
