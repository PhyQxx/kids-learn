<template>
  <el-card class="admin-crud-page">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">订单管理</span>
        <div style="display:flex;gap:12px">
          <el-input v-model="keyword" placeholder="搜索订单号/用户名" style="width:200px" @keyup.enter="fetchData" clearable @clear="fetchData" />
          <el-select v-model="filterStatus" placeholder="订单状态" clearable @change="fetchData" style="width:120px">
            <el-option label="待支付" :value="0" />
            <el-option label="已支付" :value="1" />
            <el-option label="已退款" :value="2" />
            <el-option label="已关闭" :value="3" />
          </el-select>
          <el-button :icon="Refresh" @click="fetchData" :loading="loading">刷新</el-button>
        </div>
      </div>
    </template>

    <div ref="tableBox">
      <el-table :data="tableData" stripe v-loading="loading" :max-height="tableMaxHeight">
        <el-table-column prop="orderNo" label="订单号" width="180" show-overflow-tooltip />
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="planName" label="套餐" width="120" />
        <el-table-column prop="amount" label="金额" width="100">
          <template #default="{ row }">
            <span style="color:#E6A23C;font-weight:600">¥{{ Number(row.amount).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payChannel" label="支付渠道" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column prop="payTime" label="支付时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-if="total > 0" style="margin-top:16px;justify-content:flex-end" :total="total" :page-size="pageSize"
        v-model:current-page="currentPage" layout="total, prev, pager, next" @current-change="fetchData" />
    </div>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="500">
      <el-descriptions :column="2" border v-if="currentOrder">
        <el-descriptions-item label="订单号" :span="2">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ currentOrder.username }}</el-descriptions-item>
        <el-descriptions-item label="套餐">{{ currentOrder.planName }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ Number(currentOrder.amount).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(currentOrder.status)">{{ statusText(currentOrder.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="支付渠道">{{ currentOrder.payChannel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentOrder.createTime }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ currentOrder.payTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentOrder.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <section v-if="currentOrder" class="payment-timeline">
        <h3>支付时间线</h3>
        <el-timeline>
          <el-timeline-item v-for="item in orderTimeline" :key="item.label" :timestamp="item.time || '—'" :type="item.type">{{ item.label }}</el-timeline-item>
        </el-timeline>
      </section>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getOrderList } from '@/api/request'
import { useTableHeight } from '@/composables/useTableHeight'

const { tableBox, tableMaxHeight } = useTableHeight()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const filterStatus = ref<number | null>(null)

const detailVisible = ref(false)
const currentOrder = ref<any>(null)
const orderTimeline = computed(() => {
  if (!currentOrder.value) return []
  const row = currentOrder.value
  const result: Array<{ label: string; time?: string; type: 'primary' | 'success' | 'warning' | 'danger' | 'info' }> = [{ label: '订单已创建', time: row.createTime, type: 'primary' }]
  if (row.payTime) result.push({ label: `支付成功${row.payChannel ? ` · ${row.payChannel}` : ''}`, time: row.payTime, type: 'success' })
  if (row.status === 2) result.push({ label: '订单已退款', time: row.refundTime || row.updateTime, type: 'warning' })
  if (row.status === 3) result.push({ label: '订单已关闭', time: row.closeTime || row.updateTime, type: 'info' })
  return result
})

function statusText(status: number) {
  return ['待支付', '已支付', '已退款', '已关闭'][status] || '未知'
}

function statusType(status: number): 'warning' | 'success' | 'danger' | 'info' {
  const types: ('warning' | 'success' | 'danger' | 'info')[] = ['warning', 'success', 'danger', 'info']
  return types[status] || 'info'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getOrderList({
      page: currentPage.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      status: filterStatus.value ?? undefined,
    })
    if (res.code === 200) {
      tableData.value = res.data?.list || []
      total.value = res.data?.total || 0
    }
  } finally {
    loading.value = false
  }
}

function viewDetail(row: any) {
  currentOrder.value = row
  detailVisible.value = true
}

onMounted(() => fetchData())
</script>

<style scoped>
.payment-timeline { margin-top: var(--space-5); }.payment-timeline h3 { margin-bottom: var(--space-4); font-size: var(--font-size-heading-2); }
</style>
