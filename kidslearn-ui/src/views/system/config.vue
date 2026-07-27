<template>
  <div class="config-page">
    <AdminPageHeader title="系统配置" description="按用途管理运行参数，保存前核对差异，并保留本地回滚记录。" :count="total">
      <template #secondary><el-button :disabled="!history.length" @click="historyVisible = true">变更历史</el-button></template>
    </AdminPageHeader>
    <AdminFilterBar :loading="loading" :collapsible="false" @search="applyFilters" @reset="resetFilters">
      <el-input v-model="keyword" clearable placeholder="搜索配置键或说明" @keyup.enter="applyFilters" />
      <el-select v-model="typeFilter" clearable placeholder="全部类型"><el-option label="系统参数" :value="1" /><el-option label="运营参数" :value="2" /></el-select>
    </AdminFilterBar>
    <section class="config-card" ref="tableBox">
      <el-table :data="filteredRows" stripe v-loading="loading" :max-height="tableMaxHeight">
        <el-table-column label="配置项" min-width="260"><template #default="{ row }"><div class="config-name"><code>{{ row.configKey }}</code><small>{{ row.description || '暂无说明' }}</small></div></template></el-table-column>
        <el-table-column prop="configValue" label="当前值" min-width="240" show-overflow-tooltip><template #default="{ row }"><span :class="{ masked: isSensitive(row.configKey) }">{{ displayValue(row) }}</span></template></el-table-column>
        <el-table-column prop="configType" label="分组" width="110"><template #default="{ row }"><AdminStatusTag :label="row.configType === 1 ? '系统' : '运营'" :tone="row.configType === 1 ? 'info' : 'neutral'" /></template></el-table-column>
        <el-table-column label="操作" width="100"><template #default="{ row }"><el-button link type="primary" @click="openDialog(row)">编辑</el-button></template></el-table-column>
      </el-table>
      <AdminEmptyState v-if="!loading && !filteredRows.length" :type="keyword || typeFilter !== '' ? 'filtered' : 'empty'" />
      <el-pagination v-if="total > 0" v-model:current-page="currentPage" class="pagination" :total="total" :page-size="pageSize" layout="total, prev, pager, next" @current-change="fetchData" />
    </section>

    <el-dialog v-model="dialogVisible" title="编辑配置" width="560">
      <el-alert v-if="isSensitive(form.configKey)" title="敏感配置" description="页面不会在列表中展示完整值。请仅在确定需要替换时输入新值。" type="warning" show-icon :closable="false" />
      <el-form :model="form" label-position="top">
        <el-form-item label="配置键"><el-input v-model="form.configKey" disabled /></el-form-item>
        <el-form-item label="配置值"><el-input v-model="form.configValue" :type="isSensitive(form.configKey) ? 'password' : 'textarea'" :rows="3" show-password /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" disabled /></el-form-item>
      </el-form>
      <section v-if="form.configValue !== originalValue" class="diff-card"><b>保存差异</b><div><span>原值</span><code>{{ maskedValue(originalValue, form.configKey) }}</code></div><div><span>新值</span><code>{{ maskedValue(form.configValue, form.configKey) }}</code></div></section>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :disabled="form.configValue === originalValue" :loading="saving" @click="handleSave">确认保存</el-button></template>
    </el-dialog>

    <el-drawer v-model="historyVisible" title="配置变更历史" size="min(560px, 92vw)">
      <div v-if="history.length" class="history-list"><article v-for="item in history" :key="item.id"><header><code>{{ item.configKey }}</code><time>{{ formatTime(item.changedAt) }}</time></header><p><s>{{ maskedValue(item.oldValue, item.configKey) }}</s> → {{ maskedValue(item.newValue, item.configKey) }}</p><el-button link type="primary" @click="rollback(item)">回滚到原值</el-button></article></div>
      <AdminEmptyState v-else title="暂无变更记录" description="此浏览器保存的配置变更会显示在这里。" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getConfigList, saveConfig } from '@/api/request'
import { useTableHeight } from '@/composables/useTableHeight'
import AdminEmptyState from '@/components/admin/AdminEmptyState.vue'
import AdminFilterBar from '@/components/admin/AdminFilterBar.vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import AdminStatusTag from '@/components/admin/AdminStatusTag.vue'

type ConfigHistory = { id: string; configKey: string; configId: number | null; oldValue: string; newValue: string; description: string; changedAt: string }
const HISTORY_KEY = 'admin_config_history'
const { tableBox, tableMaxHeight } = useTableHeight()
const loading = ref(false), saving = ref(false), dialogVisible = ref(false), historyVisible = ref(false)
const tableData = ref<any[]>([]), total = ref(0), currentPage = ref(1), pageSize = ref(50)
const keyword = ref(''), typeFilter = ref<number | ''>(''), originalValue = ref('')
const form = reactive({ id: null as number | null, configKey: '', configValue: '', description: '', configType: 1 })
const history = ref<ConfigHistory[]>(loadHistory())
const filteredRows = computed(() => tableData.value.filter(row => (!keyword.value || `${row.configKey} ${row.description}`.toLowerCase().includes(keyword.value.toLowerCase())) && (typeFilter.value === '' || row.configType === typeFilter.value)))

function loadHistory(): ConfigHistory[] { try { return JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]').slice(0, 50) } catch { return [] } }
function persistHistory() { localStorage.setItem(HISTORY_KEY, JSON.stringify(history.value.slice(0, 50))) }
function isSensitive(key = '') { return /(secret|token|password|api[_-]?key)/i.test(key) }
function maskedValue(value: string, key: string) { return isSensitive(key) && value ? '••••••••' : value || '（空）' }
function displayValue(row: any) { return maskedValue(String(row.configValue || ''), String(row.configKey || '')) }
function formatTime(value: string) { return new Date(value).toLocaleString('zh-CN', { hour12: false }) }
async function fetchData() { loading.value = true; try { const res = await getConfigList({ page: currentPage.value, pageSize: pageSize.value }); if (res.code === 200) { tableData.value = (res.data.list || []).filter(item => !String(item.configKey || '').startsWith('ai.')); total.value = tableData.value.length } } finally { loading.value = false } }
function applyFilters() { currentPage.value = 1; fetchData() }
function resetFilters() { keyword.value = ''; typeFilter.value = ''; applyFilters() }
function openDialog(row: any) { Object.assign(form, row); originalValue.value = String(row.configValue || ''); dialogVisible.value = true }
async function saveAndRecord(oldValue: string) { const res = await saveConfig(form); if (res.code !== 200) return void ElMessage.error(res.msg); history.value.unshift({ id: `${Date.now()}`, configKey: form.configKey, configId: form.id, oldValue, newValue: form.configValue, description: form.description, changedAt: new Date().toISOString() }); persistHistory(); ElMessage.success('配置已保存'); dialogVisible.value = false; await fetchData() }
async function handleSave() { await ElMessageBox.confirm('配置变更可能立即影响线上行为，请确认差异无误。', '确认配置变更', { type: 'warning', confirmButtonText: '确认保存' }); saving.value = true; try { await saveAndRecord(originalValue.value) } finally { saving.value = false } }
async function rollback(item: ConfigHistory) { await ElMessageBox.confirm(`将 ${item.configKey} 回滚到上一个值？`, '确认回滚', { type: 'warning' }); Object.assign(form, { id: item.configId, configKey: item.configKey, configValue: item.oldValue, description: item.description }); const res = await saveConfig(form); if (res.code === 200) { history.value.unshift({ ...item, id: `${Date.now()}`, oldValue: item.newValue, newValue: item.oldValue, changedAt: new Date().toISOString() }); persistHistory(); ElMessage.success('已回滚'); historyVisible.value = false; fetchData() } else ElMessage.error(res.msg) }
onMounted(fetchData)
</script>

<style scoped>
.config-card { padding: var(--space-3); background: var(--admin-surface); border: 1px solid var(--admin-border); border-radius: var(--radius-card); }.config-name { display: grid; gap: var(--space-1); }.config-name code { color: var(--admin-text); }.config-name small { color: var(--admin-muted); }.masked { letter-spacing: .12em; }.pagination { margin-top: var(--space-4); justify-content: flex-end; }
.diff-card { display: grid; gap: var(--space-2); padding: var(--space-3); background: var(--color-gray-50); border-radius: var(--radius-control); }.diff-card div { display: grid; grid-template-columns: 60px 1fr; gap: var(--space-2); }.diff-card span { color: var(--admin-muted); }.diff-card code { overflow-wrap: anywhere; }
.history-list { display: grid; gap: var(--space-3); }.history-list article { padding: var(--space-3); border: 1px solid var(--admin-border); border-radius: var(--radius-control); }.history-list header { display: flex; justify-content: space-between; gap: var(--space-2); }.history-list time { color: var(--admin-muted); font-size: var(--font-size-caption); }.history-list p { overflow-wrap: anywhere; }
</style>
