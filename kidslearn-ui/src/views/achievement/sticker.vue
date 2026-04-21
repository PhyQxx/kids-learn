<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">贴纸管理</span>
        <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B" @click="openDialog()">新增贴纸</el-button>
      </div>
    </template>
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="stickerCode" label="贴纸代码" />
      <el-table-column prop="stickerName" label="贴纸名称" />
      <el-table-column prop="rarity" label="稀有度" width="80">
        <template #default="{ row }">{{ ['', '普通', '稀有', '传说'][row.rarity] }}</template>
      </el-table-column>
      <el-table-column prop="seriesName" label="所属系列" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-if="total > 0" style="margin-top:16px;justify-content:flex-end" :total="total" :page-size="pageSize"
      v-model:current-page="currentPage" layout="total, prev, pager, next" @current-change="fetchData" />

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑贴纸' : '新增贴纸'" width="500">
      <el-form :model="form" label-width="80px">
        <el-form-item label="贴纸代码"><el-input v-model="form.stickerCode" /></el-form-item>
        <el-form-item label="贴纸名称"><el-input v-model="form.stickerName" /></el-form-item>
        <el-form-item label="图片URL"><el-input v-model="form.stickerUrl" /></el-form-item>
        <el-form-item label="稀有度"><el-select v-model="form.rarity"><el-option label="普通" :value="1" /><el-option label="稀有" :value="2" /><el-option label="传说" :value="3" /></el-select></el-form-item>
        <el-form-item label="系列">
          <el-select v-model="form.seriesId" style="width:100%">
            <el-option v-for="s in seriesList" :key="s.id" :label="s.seriesName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { getStickerList, saveSticker, deleteSticker, getStickerSeriesList } from '@/api/request'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const seriesList = ref<any[]>([])

const form = reactive({
  stickerCode: '', stickerName: '', stickerUrl: '', rarity: 1,
  seriesId: null as number | null, description: '', status: 1
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getStickerList({ page: currentPage.value, pageSize: pageSize.value })
    if (res.code === 200) { tableData.value = res.data.list; total.value = res.data.total }
  } finally { loading.value = false }
}

async function fetchSeries() {
  const res = await getStickerSeriesList()
  if (res.code === 200) seriesList.value = res.data
}

function openDialog(row?: any) {
  if (row) { editingId.value = row.id; Object.assign(form, row) }
  else { editingId.value = null; Object.assign(form, { stickerCode: '', stickerName: '', stickerUrl: '', rarity: 1, seriesId: null, description: '', status: 1 }) }
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    const res = await saveSticker({ ...form, id: editingId.value })
    if (res.code === 200) { ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() }
    else ElMessage.error(res.msg)
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  const res = await deleteSticker(id)
  if (res.code === 200) { ElMessage.success('删除成功'); fetchData() }
}

onMounted(() => { fetchSeries(); fetchData() })
</script>
