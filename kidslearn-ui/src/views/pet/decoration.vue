<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">装饰管理</span>
        <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B" @click="openDialog()">新增装饰</el-button>
      </div>
    </template>
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="decoCode" label="装饰代码" />
      <el-table-column prop="decoName" label="装饰名称" />
      <el-table-column prop="slot" label="部位" width="100">
        <template #default="{ row }">{{ slotMap[row.slot] || row.slot }}</template>
      </el-table-column>
      <el-table-column prop="priceGold" label="金币价" width="80" />
      <el-table-column prop="priceDiamond" label="钻石价" width="80" />
      <el-table-column prop="rarity" label="稀有度" width="80">
        <template #default="{ row }">{{ ['', '普通', '稀有', '传说'][row.rarity] }}</template>
      </el-table-column>
      <el-table-column prop="isLimited" label="限定" width="80">
        <template #default="{ row }"><el-tag :type="row.isLimited ? 'warning' : 'info'" size="small">{{ row.isLimited ? '是' : '否' }}</el-tag></template>
      </el-table-column>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑装饰' : '新增装饰'" width="500">
      <el-form :model="form" label-width="80px">
        <el-form-item label="装饰代码"><el-input v-model="form.decoCode" /></el-form-item>
        <el-form-item label="装饰名称"><el-input v-model="form.decoName" /></el-form-item>
        <el-form-item label="部位">
          <el-select v-model="form.slot">
            <el-option v-for="(v, k) in slotMap" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="图片URL"><el-input v-model="form.imageUrl" /></el-form-item>
        <el-form-item label="层级"><el-input-number v-model="form.layerOrder" :min="0" /></el-form-item>
        <el-form-item label="金币价"><el-input-number v-model="form.priceGold" :min="0" /></el-form-item>
        <el-form-item label="钻石价"><el-input-number v-model="form.priceDiamond" :min="0" /></el-form-item>
        <el-form-item label="稀有度"><el-select v-model="form.rarity"><el-option label="普通" :value="1" /><el-option label="稀有" :value="2" /><el-option label="传说" :value="3" /></el-select></el-form-item>
        <el-form-item label="限定"><el-switch v-model="form.isLimited" :active-value="1" :inactive-value="0" /></el-form-item>
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
import { getDecorationList, saveDecoration, deleteDecoration } from '@/api/request'

const slotMap: Record<string, string> = { HEAD: '头部', BODY: '身体', FACE: '脸部', BACK: '背部', FOOT: '脚部', EFFECT: '特效' }
const loading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  decoCode: '', decoName: '', slot: 'HEAD', imageUrl: '', layerOrder: 0,
  priceGold: 0, priceDiamond: 0, rarity: 1, isLimited: 0, status: 1
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getDecorationList({ page: currentPage.value, pageSize: pageSize.value })
    if (res.code === 200) { tableData.value = res.data.list; total.value = res.data.total }
  } finally { loading.value = false }
}

function openDialog(row?: any) {
  if (row) { editingId.value = row.id; Object.assign(form, row) }
  else { editingId.value = null; Object.assign(form, { decoCode: '', decoName: '', slot: 'HEAD', imageUrl: '', layerOrder: 0, priceGold: 0, priceDiamond: 0, rarity: 1, isLimited: 0, status: 1 }) }
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    const res = await saveDecoration({ ...form, id: editingId.value })
    if (res.code === 200) { ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() }
    else ElMessage.error(res.msg)
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  const res = await deleteDecoration(id)
  if (res.code === 200) { ElMessage.success('删除成功'); fetchData() }
}

onMounted(() => fetchData())
</script>
