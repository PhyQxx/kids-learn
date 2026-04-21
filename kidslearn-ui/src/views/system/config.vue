<template>
  <el-card>
    <template #header><span style="font-weight:700">系统配置</span></template>
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="configKey" label="配置键" />
      <el-table-column prop="configValue" label="配置值" show-overflow-tooltip />
      <el-table-column prop="configType" label="类型" width="80">
        <template #default="{ row }">{{ row.configType === 1 ? '系统' : '运营' }}</template>
      </el-table-column>
      <el-table-column prop="description" label="描述" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-if="total > 0" style="margin-top:16px;justify-content:flex-end" :total="total" :page-size="pageSize"
      v-model:current-page="currentPage" layout="total, prev, pager, next" @current-change="fetchData" />

    <el-dialog v-model="dialogVisible" title="编辑配置" width="500">
      <el-form :model="form" label-width="80px">
        <el-form-item label="配置键"><el-input v-model="form.configKey" disabled /></el-form-item>
        <el-form-item label="配置值"><el-input v-model="form.configValue" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" disabled /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getConfigList, saveConfig } from '@/api/request'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const dialogVisible = ref(false)

const form = reactive({ id: null as number | null, configKey: '', configValue: '', description: '' })

async function fetchData() {
  loading.value = true
  try {
    const res = await getConfigList({ page: currentPage.value, pageSize: pageSize.value })
    if (res.code === 200) { tableData.value = res.data.list; total.value = res.data.total }
  } finally { loading.value = false }
}

function openDialog(row: any) {
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    const res = await saveConfig(form)
    if (res.code === 200) { ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() }
    else ElMessage.error(res.msg)
  } finally { saving.value = false }
}

onMounted(() => fetchData())
</script>
