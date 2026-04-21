<template>
  <el-card>
    <template #header><span style="font-weight:700">操作日志</span></template>
    <div style="margin-bottom:16px">
      <el-select v-model="filterModule" placeholder="筛选模块" clearable @change="fetchData" style="width:160px">
        <el-option label="学科" value="subject" /><el-option label="课程" value="course" /><el-option label="题目" value="question" />
        <el-option label="宠物" value="pet" /><el-option label="成就" value="achievement" /><el-option label="用户" value="user" />
      </el-select>
    </div>
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="adminUserId" label="操作人" width="100" />
      <el-table-column prop="module" label="模块" width="100" />
      <el-table-column prop="action" label="操作" width="100" />
      <el-table-column prop="targetType" label="对象类型" width="100" />
      <el-table-column prop="targetId" label="对象ID" width="80" />
      <el-table-column prop="ip" label="IP" width="130" />
      <el-table-column prop="createTime" label="操作时间" width="180" />
    </el-table>
    <el-pagination v-if="total > 0" style="margin-top:16px;justify-content:flex-end" :total="total" :page-size="pageSize"
      v-model:current-page="currentPage" layout="total, prev, pager, next" @current-change="fetchData" />
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getLogList } from '@/api/request'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const filterModule = ref('')

async function fetchData() {
  loading.value = true
  try {
    const res = await getLogList({ page: currentPage.value, pageSize: pageSize.value, module: filterModule.value || undefined })
    if (res.code === 200) { tableData.value = res.data.list; total.value = res.data.total }
  } finally { loading.value = false }
}

onMounted(() => fetchData())
</script>
