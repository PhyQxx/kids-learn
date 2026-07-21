<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">排行榜管理</span>
        <div style="display:flex;gap:12px">
          <el-select v-model="rankType" style="width:140px" @change="fetchData">
            <el-option label="周排行榜" value="weekly" />
            <el-option label="总排行榜" value="total" />
            <el-option label="挑战赛排行" value="challenge" />
          </el-select>
          <el-button :icon="Refresh" @click="fetchData" :loading="loading">刷新</el-button>
        </div>
      </div>
    </template>

    <div ref="tableBox">
      <el-table :data="tableData" stripe v-loading="loading" :max-height="tableMaxHeight">
        <el-table-column label="排名" width="80">
          <template #default="{ $index }">
            <div style="display:flex;align-items:center;gap:6px">
              <span v-if="$index < 3" style="font-size:20px">{{ ['🥇', '🥈', '🥉'][$index] }}</span>
              <span v-else>{{ $index + 1 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="用户" min-width="150">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:10px">
              <el-avatar :size="36" :src="row.avatar">{{ (row.nickname || '?')[0] }}</el-avatar>
              <div>
                <div style="font-weight:600">{{ row.nickname }}</div>
                <div v-if="row.isMe" style="font-size:12px;color:#FF6B6B">当前用户</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分数" width="120" sortable>
          <template #default="{ row }">
            <span style="font-weight:600;color:#E6A23C">{{ row.score?.toLocaleString() }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="rank" label="排名" width="100" />
      </el-table>
    </div>

    <div v-if="tableData.length === 0 && !loading" style="text-align:center;padding:40px;color:#909399">
      暂无排行数据
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getRankingList } from '@/api/request'
import { useTableHeight } from '@/composables/useTableHeight'

const { tableBox, tableMaxHeight } = useTableHeight()

const loading = ref(false)
const rankType = ref('weekly')
const tableData = ref<any[]>([])

async function fetchData() {
  loading.value = true
  try {
    const res = await getRankingList(rankType.value)
    if (res.code === 200) {
      tableData.value = res.data || []
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchData())
</script>
