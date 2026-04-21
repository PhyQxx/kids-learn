<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">用户管理</span>
        <el-input v-model="keyword" placeholder="搜索用户名/昵称" style="width:240px" @keyup.enter="fetchData" clearable @clear="fetchData" />
      </div>
    </template>
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="userType" label="类型" width="80">
        <template #default="{ row }">{{ row.userType === 1 ? '孩子' : '家长' }}</template>
      </el-table-column>
      <el-table-column prop="level" label="等级" width="70" />
      <el-table-column prop="gold" label="金币" width="80" />
      <el-table-column prop="diamond" label="钻石" width="70" />
      <el-table-column prop="totalExp" label="经验" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-if="total > 0" style="margin-top:16px;justify-content:flex-end" :total="total" :page-size="pageSize"
      v-model:current-page="currentPage" layout="total, prev, pager, next" @current-change="fetchData" />
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, updateUserStatus } from '@/api/request'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const keyword = ref('')

async function fetchData() {
  loading.value = true
  try {
    const res = await getUserList({ page: currentPage.value, pageSize: pageSize.value, keyword: keyword.value || undefined })
    if (res.code === 200) { tableData.value = res.data.list; total.value = res.data.total }
  } finally { loading.value = false }
}

async function toggleStatus(row: any) {
  const newStatus = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(`确认${newStatus === 1 ? '启用' : '禁用'}用户 ${row.nickname || row.username}？`, '提示', { type: 'warning' })
  const res = await updateUserStatus(row.id, newStatus)
  if (res.code === 200) { ElMessage.success('操作成功'); fetchData() }
}

onMounted(() => fetchData())
</script>
