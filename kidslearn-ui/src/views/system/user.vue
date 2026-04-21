<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">用户管理</span>
        <div style="display:flex;gap:12px;align-items:center">
          <el-input v-model="keyword" placeholder="搜索用户名/昵称" style="width:240px" @keyup.enter="fetchData" clearable @clear="fetchData" />
          <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B" @click="openDialog()">新增用户</el-button>
        </div>
      </div>
    </template>
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="userType" label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="row.userType === 3 ? 'warning' : row.userType === 2 ? '' : 'success'" size="small">
            {{ userTypeLabel(row.userType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="level" label="等级" width="70" />
      <el-table-column prop="gold" label="金币" width="80" />
      <el-table-column prop="diamond" label="钻石" width="70" />
      <el-table-column prop="totalExp" label="经验" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-if="total > 0" style="margin-top:16px;justify-content:flex-end" :total="total" :page-size="pageSize"
      v-model:current-page="currentPage" layout="total, prev, pager, next" @current-change="fetchData" />

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑用户' : '新增用户'" width="500">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" :placeholder="editingId ? '留空则不修改' : '请输入密码'" show-password />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="用户类型">
          <el-select v-model="form.userType" style="width:100%">
            <el-option label="孩子" :value="1" />
            <el-option label="家长" :value="2" />
            <el-option label="管理员" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
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
import { getUserList, saveUser, updateUserStatus } from '@/api/request'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  userType: 1,
  realName: '',
  status: 1,
})

function userTypeLabel(type: number) {
  if (type === 3) return '管理员'
  if (type === 2) return '家长'
  return '孩子'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getUserList({ page: currentPage.value, pageSize: pageSize.value, keyword: keyword.value || undefined })
    if (res.code === 200) { tableData.value = res.data.list; total.value = res.data.total }
  } finally { loading.value = false }
}

function openDialog(row?: any) {
  if (row) {
    editingId.value = row.id
    Object.assign(form, {
      username: row.username,
      password: '',
      nickname: row.nickname || '',
      userType: row.userType,
      realName: row.realName || '',
      status: row.status,
    })
  } else {
    editingId.value = null
    Object.assign(form, { username: '', password: '', nickname: '', userType: 1, realName: '', status: 1 })
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!editingId.value && !form.username) { ElMessage.warning('请输入用户名'); return }
  if (!editingId.value && !form.password) { ElMessage.warning('请输入密码'); return }
  saving.value = true
  try {
    const data: any = { ...form, id: editingId.value }
    if (editingId.value && !form.password) delete data.password
    const res = await saveUser(data)
    if (res.code === 200) { ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() }
    else ElMessage.error(res.msg)
  } finally { saving.value = false }
}

async function toggleStatus(row: any) {
  const newStatus = row.status === 1 ? 0 : 1
  await ElMessageBox.confirm(`确认${newStatus === 1 ? '启用' : '禁用'}用户 ${row.nickname || row.username}？`, '提示', { type: 'warning' })
  const res = await updateUserStatus(row.id, newStatus)
  if (res.code === 200) { ElMessage.success('操作成功'); fetchData() }
}

onMounted(() => fetchData())
</script>
