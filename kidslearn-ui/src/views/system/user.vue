<template>
  <div class="user-page">
    <AdminPageHeader title="用户管理" description="查找账号、查看学习资产，并安全地管理账号状态。" :count="total">
      <template #primary><el-button type="primary" @click="openDialog()">新增用户</el-button></template>
    </AdminPageHeader>

    <AdminFilterBar :loading="loading" :collapsible="false" @search="applyFilters" @reset="resetFilters">
      <el-input v-model="keyword" clearable placeholder="搜索用户名、昵称或真实姓名" @keyup.enter="applyFilters" />
      <el-select v-model="statusFilter" clearable placeholder="全部状态"><el-option label="正常" :value="1" /><el-option label="已禁用" :value="0" /></el-select>
      <el-select v-model="typeFilter" clearable placeholder="全部类型"><el-option label="普通用户" :value="1" /><el-option label="管理员" :value="3" /></el-select>
    </AdminFilterBar>

    <section class="user-table-card" ref="tableBox">
      <el-table :data="filteredRows" stripe v-loading="loading" :max-height="tableMaxHeight" @row-click="openDetail">
        <el-table-column label="用户" min-width="220">
          <template #default="{ row }"><div class="identity"><el-avatar :size="34">{{ (row.nickname || row.username || '?').slice(0, 1) }}</el-avatar><div><b>{{ row.nickname || row.username }}</b><small>@{{ row.username }}</small></div></div></template>
        </el-table-column>
        <el-table-column prop="userType" label="类型" width="110"><template #default="{ row }"><AdminStatusTag :label="userTypeLabel(row.userType)" :tone="row.userType === 3 ? 'warning' : 'neutral'" /></template></el-table-column>
        <el-table-column label="学习概览" min-width="180"><template #default="{ row }"><span>Lv.{{ row.level || 1 }} · {{ Number(row.totalExp || 0).toLocaleString() }} 经验</span></template></el-table-column>
        <el-table-column prop="status" label="状态" width="100"><template #default="{ row }"><AdminStatusTag :label="row.status === 1 ? '正常' : '已禁用'" :tone="row.status === 1 ? 'success' : 'danger'" /></template></el-table-column>
        <el-table-column label="操作" width="170" fixed="right"><template #default="{ row }"><el-button link type="primary" @click.stop="openDialog(row)">编辑</el-button><el-button link :type="row.status === 1 ? 'danger' : 'success'" @click.stop="toggleStatus(row)">{{ row.status === 1 ? '禁用' : '启用' }}</el-button></template></el-table-column>
      </el-table>
      <AdminEmptyState v-if="!loading && !filteredRows.length" :type="keyword || statusFilter !== '' || typeFilter !== '' ? 'filtered' : 'empty'" />
      <el-pagination v-if="total > 0" v-model:current-page="currentPage" class="pagination" :total="total" :page-size="pageSize" layout="total, prev, pager, next" @current-change="fetchData" />
    </section>

    <el-drawer v-model="detailVisible" title="用户详情" size="min(620px, 92vw)">
      <template v-if="activeUser">
        <div class="detail-hero"><el-avatar :size="54">{{ (activeUser.nickname || activeUser.username || '?').slice(0, 1) }}</el-avatar><div><h2>{{ activeUser.nickname || activeUser.username }}</h2><p>@{{ activeUser.username }} · {{ userTypeLabel(activeUser.userType) }}</p></div><AdminStatusTag :label="activeUser.status === 1 ? '正常' : '已禁用'" :tone="activeUser.status === 1 ? 'success' : 'danger'" /></div>
        <el-tabs>
          <el-tab-pane label="账号概览"><dl class="detail-grid"><div><dt>真实姓名</dt><dd>{{ activeUser.realName || '未填写' }}</dd></div><div><dt>等级</dt><dd>Lv.{{ activeUser.level || 1 }}</dd></div><div><dt>经验</dt><dd>{{ Number(activeUser.totalExp || 0).toLocaleString() }}</dd></div><div><dt>用户 ID</dt><dd>{{ activeUser.id }}</dd></div></dl></el-tab-pane>
          <el-tab-pane label="虚拟资产"><dl class="detail-grid"><div><dt>金币</dt><dd>{{ Number(activeUser.gold || 0).toLocaleString() }}</dd></div><div><dt>钻石</dt><dd>{{ Number(activeUser.diamond || 0).toLocaleString() }}</dd></div></dl></el-tab-pane>
          <el-tab-pane label="操作记录"><AdminEmptyState title="暂无可展示的用户操作" description="管理员操作可前往系统日志按用户 ID 检索。" /></el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑用户' : '新增用户'" width="500">
      <el-form :model="form" label-width="90px">
        <el-form-item label="用户名" required><el-input v-model="form.username" :disabled="!!editingId" /></el-form-item>
        <el-form-item label="密码" :required="!editingId"><el-input v-model="form.password" type="password" :placeholder="editingId ? '留空则不修改' : '请输入密码'" show-password /></el-form-item>
        <el-form-item label="昵称"><el-input v-model="form.nickname" /></el-form-item>
        <el-form-item label="用户类型"><el-select v-model="form.userType" style="width:100%"><el-option label="普通用户" :value="1" /><el-option label="管理员" :value="3" /></el-select></el-form-item>
        <el-form-item label="真实姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :value="1">正常</el-radio><el-radio :value="0">禁用</el-radio></el-radio-group></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="handleSave">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, saveUser, updateUserStatus } from '@/api/request'
import { useTableHeight } from '@/composables/useTableHeight'
import AdminEmptyState from '@/components/admin/AdminEmptyState.vue'
import AdminFilterBar from '@/components/admin/AdminFilterBar.vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import AdminStatusTag from '@/components/admin/AdminStatusTag.vue'

const { tableBox, tableMaxHeight } = useTableHeight()
const loading = ref(false), saving = ref(false), dialogVisible = ref(false), detailVisible = ref(false)
const tableData = ref<any[]>([]), total = ref(0), currentPage = ref(1), pageSize = ref(20)
const keyword = ref(''), statusFilter = ref<number | ''>(''), typeFilter = ref<number | ''>('')
const editingId = ref<number | null>(null), activeUser = ref<any>(null)
const form = reactive({ username: '', password: '', nickname: '', userType: 1, realName: '', status: 1 })
const filteredRows = computed(() => tableData.value.filter(row => (statusFilter.value === '' || row.status === statusFilter.value) && (typeFilter.value === '' || row.userType === typeFilter.value)))

function userTypeLabel(type: number) { return type === 3 ? '管理员' : '普通用户' }
async function fetchData() { loading.value = true; try { const res = await getUserList({ page: currentPage.value, pageSize: pageSize.value, keyword: keyword.value || undefined }); if (res.code === 200) { tableData.value = res.data.list; total.value = res.data.total } } finally { loading.value = false } }
function applyFilters() { currentPage.value = 1; fetchData() }
function resetFilters() { keyword.value = ''; statusFilter.value = ''; typeFilter.value = ''; applyFilters() }
function openDetail(row: any) { activeUser.value = row; detailVisible.value = true }
function openDialog(row?: any) { editingId.value = row?.id || null; Object.assign(form, row ? { username: row.username, password: '', nickname: row.nickname || '', userType: row.userType === 3 ? 3 : 1, realName: row.realName || '', status: row.status } : { username: '', password: '', nickname: '', userType: 1, realName: '', status: 1 }); dialogVisible.value = true }
async function handleSave() { if (!editingId.value && !form.username) return void ElMessage.warning('请输入用户名'); if (!editingId.value && !form.password) return void ElMessage.warning('请输入密码'); saving.value = true; try { const data: any = { ...form, id: editingId.value }; if (editingId.value && !form.password) delete data.password; const res = await saveUser(data); if (res.code === 200) { ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() } else ElMessage.error(res.msg) } finally { saving.value = false } }
async function toggleStatus(row: any) { const enabled = row.status !== 1; await ElMessageBox.confirm(enabled ? `确认启用 ${row.nickname || row.username}？` : `禁用后该用户将无法登录，确认禁用 ${row.nickname || row.username}？`, enabled ? '启用用户' : '高风险操作', { type: enabled ? 'info' : 'warning', confirmButtonText: enabled ? '确认启用' : '确认禁用' }); const res = await updateUserStatus(row.id, enabled ? 1 : 0); if (res.code === 200) { ElMessage.success('账号状态已更新'); detailVisible.value = false; fetchData() } }
onMounted(fetchData)
</script>

<style scoped>
.user-page { min-width: 0; }
.user-table-card { padding: var(--space-3); background: var(--admin-surface); border: 1px solid var(--admin-border); border-radius: var(--radius-card); }
.identity, .detail-hero { display: flex; align-items: center; gap: var(--space-3); }
.identity div { display: grid; }.identity small, .detail-hero p, dt { color: var(--admin-muted); }.identity b { color: var(--admin-text); }
.pagination { margin-top: var(--space-4); justify-content: flex-end; }
.detail-hero { padding-bottom: var(--space-4); border-bottom: 1px solid var(--admin-border); }.detail-hero div { flex: 1; }.detail-hero h2, .detail-hero p { margin: 0; }
.detail-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--space-3); }.detail-grid div { padding: var(--space-3); background: var(--color-gray-50); border-radius: var(--radius-control); }.detail-grid dt { font-size: var(--font-size-caption); }.detail-grid dd { margin: var(--space-1) 0 0; font-weight: 700; }
@media (max-width: 600px) { .detail-grid { grid-template-columns: 1fr; } }
</style>
