<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">宠物管理</span>
        <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B" @click="openDialog()">新增宠物</el-button>
      </div>
    </template>
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="petCode" label="宠物代码" />
      <el-table-column prop="petName" label="宠物名称" />
      <el-table-column prop="petType" label="类型" width="80">
        <template #default="{ row }">{{ ['', '动物', '植物', '幻想'][row.petType] }}</template>
      </el-table-column>
      <el-table-column prop="unlockCost" label="解锁价格" width="100" />
      <el-table-column prop="isDefault" label="默认宠物" width="100">
        <template #default="{ row }"><el-tag :type="row.isDefault ? 'success' : 'info'" size="small">{{ row.isDefault ? '是' : '否' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="primary" @click="openEvolution(row)">进化配置</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-if="total > 0" style="margin-top:16px;justify-content:flex-end" :total="total" :page-size="pageSize"
      v-model:current-page="currentPage" layout="total, prev, pager, next" @current-change="fetchData" />

    <!-- 宠物编辑 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑宠物' : '新增宠物'" width="500">
      <el-form :model="form" label-width="80px">
        <el-form-item label="宠物代码"><el-input v-model="form.petCode" /></el-form-item>
        <el-form-item label="宠物名称"><el-input v-model="form.petName" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.petType"><el-option label="动物" :value="1" /><el-option label="植物" :value="2" /><el-option label="幻想" :value="3" /></el-select>
        </el-form-item>
        <el-form-item label="形象URL"><el-input v-model="form.baseImageUrl" /></el-form-item>
        <el-form-item label="进化等级"><el-input v-model="form.evolveLevels" placeholder="如 3,5,7,9" /></el-form-item>
        <el-form-item label="解锁价格"><el-input-number v-model="form.unlockCost" :min="0" /></el-form-item>
        <el-form-item label="默认宠物"><el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 进化配置 -->
    <el-dialog v-model="evoDialogVisible" title="进化配置" width="600">
      <el-table :data="evolutions" stripe>
        <el-table-column prop="evolveLevel" label="进化等级" width="100" />
        <el-table-column prop="imageUrl" label="形象URL" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEvoDialog(row)">编辑</el-button>
            <el-button link type="danger">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-button style="margin-top:12px" @click="openEvoDialog()">+ 添加进化阶段</el-button>
      <el-dialog v-model="evoFormVisible" :title="evoEditingId ? '编辑进化' : '新增进化'" width="400" append-to-body>
        <el-form :model="evoForm" label-width="80px">
          <el-form-item label="进化等级"><el-input-number v-model="evoForm.evolveLevel" :min="1" :max="10" /></el-form-item>
          <el-form-item label="形象URL"><el-input v-model="evoForm.imageUrl" /></el-form-item>
          <el-form-item label="描述"><el-input v-model="evoForm.description" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="evoFormVisible = false">取消</el-button>
          <el-button type="primary" @click="saveEvolution">保存</el-button>
        </template>
      </el-dialog>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPetList, savePet, deletePet, getPetEvolutions, savePetEvolution } from '@/api/request'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const evoDialogVisible = ref(false)
const evoFormVisible = ref(false)
const evoEditingId = ref<number | null>(null)
const currentPetId = ref<number>(0)
const evolutions = ref<any[]>([])

const form = reactive({
  petCode: '', petName: '', petType: 1, baseImageUrl: '', evolveLevels: '3,5,7,9',
  unlockCost: 0, isDefault: 0, status: 1
})
const evoForm = reactive({ evolveLevel: 3, imageUrl: '', description: '' })

async function fetchData() {
  loading.value = true
  try {
    const res = await getPetList({ page: currentPage.value, pageSize: pageSize.value })
    if (res.code === 200) { tableData.value = res.data.list; total.value = res.data.total }
  } finally { loading.value = false }
}

function openDialog(row?: any) {
  if (row) { editingId.value = row.id; Object.assign(form, row) }
  else { editingId.value = null; Object.assign(form, { petCode: '', petName: '', petType: 1, baseImageUrl: '', evolveLevels: '3,5,7,9', unlockCost: 0, isDefault: 0, status: 1 }) }
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    const res = await savePet({ ...form, id: editingId.value })
    if (res.code === 200) { ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() }
    else ElMessage.error(res.msg)
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  const res = await deletePet(id)
  if (res.code === 200) { ElMessage.success('删除成功'); fetchData() }
}

async function openEvolution(row: any) {
  currentPetId.value = row.id
  const res = await getPetEvolutions(row.id)
  if (res.code === 200) evolutions.value = res.data
  evoDialogVisible.value = true
}

function openEvoDialog(row?: any) {
  if (row) { evoEditingId.value = row.id; Object.assign(evoForm, row) }
  else { evoEditingId.value = null; Object.assign(evoForm, { evolveLevel: 3, imageUrl: '', description: '' }) }
  evoFormVisible.value = true
}

async function saveEvolution() {
  const res = await savePetEvolution({ ...evoForm, id: evoEditingId.value, petId: currentPetId.value })
  if (res.code === 200) { ElMessage.success('保存成功'); evoFormVisible.value = false; openEvolution({ id: currentPetId.value }) }
}

onMounted(() => fetchData())
</script>
