<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">字典管理</span>
        <div style="display:flex;gap:12px;align-items:center">
          <el-input v-model="keyword" placeholder="搜索字典名称/类型" style="width:240px" @keyup.enter="fetchTypes" clearable @clear="fetchTypes" />
          <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B" @click="openTypeDialog()">新增字典</el-button>
        </div>
      </div>
    </template>

    <!-- 字典类型列表 -->
    <el-table :data="typeList" stripe v-loading="typeLoading" highlight-current-row @current-change="onTypeSelect">
      <el-table-column prop="dictName" label="字典名称" />
      <el-table-column prop="dictType" label="字典类型" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" show-overflow-tooltip />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="openTypeDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleTypeDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-if="typeTotal > 0" style="margin-top:16px;justify-content:flex-end" :total="typeTotal" :page-size="20"
      v-model:current-page="typePage" layout="total, prev, pager, next" @current-change="fetchTypes" />

    <!-- 字典数据 -->
    <el-divider v-if="selectedType" content-position="left">{{ selectedType.dictName }} - 字典数据</el-divider>
    <div v-if="selectedType" style="margin-bottom:12px">
      <el-button type="primary" size="small" @click="openDataDialog()">新增数据</el-button>
    </div>
    <el-table v-if="selectedType" :data="dataList" stripe v-loading="dataLoading" size="small">
      <el-table-column prop="dictLabel" label="标签" />
      <el-table-column prop="dictValue" label="值" />
      <el-table-column prop="sortOrder" label="排序" width="70" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" show-overflow-tooltip />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDataDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDataDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 字典类型弹窗 -->
    <el-dialog v-model="typeDialogVisible" :title="editingTypeId ? '编辑字典类型' : '新增字典类型'" width="500">
      <el-form :model="typeForm" label-width="80px">
        <el-form-item label="字典名称"><el-input v-model="typeForm.dictName" /></el-form-item>
        <el-form-item label="字典类型"><el-input v-model="typeForm.dictType" :disabled="!!editingTypeId" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="typeForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="typeForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleTypeSave" :loading="typeSaving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 字典数据弹窗 -->
    <el-dialog v-model="dataDialogVisible" :title="editingDataId ? '编辑字典数据' : '新增字典数据'" width="500">
      <el-form :model="dataForm" label-width="80px">
        <el-form-item label="标签"><el-input v-model="dataForm.dictLabel" /></el-form-item>
        <el-form-item label="值"><el-input v-model="dataForm.dictValue" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="dataForm.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="dataForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="dataForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDataSave" :loading="dataSaving">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDictTypeList, saveDictType, deleteDictType, getDictDataList, saveDictData, deleteDictData } from '@/api/request'

// ==================== 字典类型 ====================
const typeLoading = ref(false)
const typeSaving = ref(false)
const typeList = ref<any[]>([])
const typeTotal = ref(0)
const typePage = ref(1)
const keyword = ref('')
const typeDialogVisible = ref(false)
const editingTypeId = ref<number | null>(null)
const typeForm = reactive({ dictName: '', dictType: '', status: 1, remark: '' })

async function fetchTypes() {
  typeLoading.value = true
  try {
    const res = await getDictTypeList({ page: typePage.value, pageSize: 20, keyword: keyword.value || undefined })
    if (res.code === 200) { typeList.value = res.data.list; typeTotal.value = res.data.total }
  } finally { typeLoading.value = false }
}

function openTypeDialog(row?: any) {
  if (row) { editingTypeId.value = row.id; Object.assign(typeForm, row) }
  else { editingTypeId.value = null; Object.assign(typeForm, { dictName: '', dictType: '', status: 1, remark: '' }) }
  typeDialogVisible.value = true
}

async function handleTypeSave() {
  if (!typeForm.dictName || !typeForm.dictType) { ElMessage.warning('请填写字典名称和类型'); return }
  typeSaving.value = true
  try {
    const res = await saveDictType({ ...typeForm, id: editingTypeId.value })
    if (res.code === 200) { ElMessage.success('保存成功'); typeDialogVisible.value = false; fetchTypes() }
    else ElMessage.error(res.msg)
  } finally { typeSaving.value = false }
}

async function handleTypeDelete(id: number) {
  await ElMessageBox.confirm('删除字典类型会同时删除其下所有数据，确认删除？', '提示', { type: 'warning' })
  const res = await deleteDictType(id)
  if (res.code === 200) { ElMessage.success('删除成功'); if (selectedType.value?.id === id) selectedType.value = null; fetchTypes() }
}

// ==================== 字典数据 ====================
const selectedType = ref<any>(null)
const dataLoading = ref(false)
const dataSaving = ref(false)
const dataList = ref<any[]>([])
const dataDialogVisible = ref(false)
const editingDataId = ref<number | null>(null)
const dataForm = reactive({ dictLabel: '', dictValue: '', sortOrder: 0, status: 1, remark: '' })

function onTypeSelect(row: any) {
  selectedType.value = row
  if (row) fetchDataList()
  else dataList.value = []
}

async function fetchDataList() {
  if (!selectedType.value) return
  dataLoading.value = true
  try {
    const res = await getDictDataList({ page: 1, pageSize: 200, dictTypeId: selectedType.value.id })
    if (res.code === 200) dataList.value = res.data.list
  } finally { dataLoading.value = false }
}

function openDataDialog(row?: any) {
  if (row) { editingDataId.value = row.id; Object.assign(dataForm, row) }
  else { editingDataId.value = null; Object.assign(dataForm, { dictLabel: '', dictValue: '', sortOrder: 0, status: 1, remark: '' }) }
  dataDialogVisible.value = true
}

async function handleDataSave() {
  if (!dataForm.dictLabel || !dataForm.dictValue) { ElMessage.warning('请填写标签和值'); return }
  dataSaving.value = true
  try {
    const res = await saveDictData({ ...dataForm, id: editingDataId.value, dictTypeId: selectedType.value.id })
    if (res.code === 200) { ElMessage.success('保存成功'); dataDialogVisible.value = false; fetchDataList() }
    else ElMessage.error(res.msg)
  } finally { dataSaving.value = false }
}

async function handleDataDelete(id: number) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  const res = await deleteDictData(id)
  if (res.code === 200) { ElMessage.success('删除成功'); fetchDataList() }
}

onMounted(() => fetchTypes())
</script>
