<template>
  <div class="role-page">
    <AdminPageHeader title="角色与权限" description="通过权限矩阵和岗位模板控制后台访问范围。" :count="tableData.length">
      <template #primary><el-button type="primary" @click="openDialog()">新增角色</el-button></template>
    </AdminPageHeader>
    <section class="role-table">
    <div ref="tableBox">
    <el-table :data="tableData" stripe v-loading="loading" :max-height="tableMaxHeight">
      <el-table-column prop="roleName" label="角色名称" />
      <el-table-column prop="roleCode" label="角色代码" />
      <el-table-column prop="permissions" label="权限码" min-width="220" show-overflow-tooltip />
      <el-table-column prop="description" label="描述" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑角色' : '新增角色'" width="min(880px, 94vw)">
      <el-form :model="form" label-width="80px">
        <el-form-item label="角色名称"><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="角色代码"><el-input v-model="form.roleCode" /></el-form-item>
        <el-form-item label="岗位模板">
          <div class="permission-editor">
            <el-space wrap>
              <el-tooltip
                v-for="preset in ROLE_PERMISSION_PRESETS"
                :key="preset.key"
                :content="preset.description"
                placement="top"
              >
                <el-button size="small" @click="applyPermissionPreset(preset.permissions)">
                  {{ preset.name }}
                </el-button>
              </el-tooltip>
            </el-space>
          </div>
        </el-form-item>
        <el-form-item label="权限矩阵">
          <div class="permission-matrix">
            <section v-for="group in permissionGroups" :key="group.name"><header><b>{{ group.name }}</b><el-checkbox :model-value="isGroupSelected(group.codes)" :indeterminate="isGroupIndeterminate(group.codes)" @change="toggleGroup(group.codes, Boolean($event))">全选</el-checkbox></header><el-checkbox-group v-model="selectedPermissions"><el-checkbox v-for="item in group.items" :key="item.code" :value="item.code">{{ item.label }}</el-checkbox></el-checkbox-group></section>
          </div>
        </el-form-item>
        <el-form-item label="高级权限"><el-input v-model="advancedPermissions" type="textarea" :rows="3" placeholder="通配或自定义权限，每行一个，如 admin:*" /></el-form-item>
        <el-alert v-if="permissionDiff" type="info" :closable="false" title="本次权限变化" :description="permissionDiff" show-icon />
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRoleList, saveRole, deleteRole } from '@/api/request'
import { ROLE_PERMISSION_PRESETS, mergePermissionCodes } from '@/utils/adminPermissions'
import { formatPermissionCodes, parsePermissionText } from '@/utils/adminPermissions'
import { useTableHeight } from '@/composables/useTableHeight'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'

const { tableBox, tableMaxHeight } = useTableHeight()

const loading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const selectedPermissions = ref<string[]>([])
const advancedPermissions = ref('')
const baselinePermissions = ref<string[]>([])

const permissionGroups = [
  { name: '工作台', items: [{ label: '查看概览', code: 'admin:dashboard:read' }, { label: '查看日志', code: 'admin:log:read' }] },
  { name: '内容运营', items: [{ label: '学科管理', code: 'admin:subject:*' }, { label: '课程管理', code: 'admin:course:*' }, { label: '关卡管理', code: 'admin:level:*' }, { label: '题库管理', code: 'admin:question:*' }, { label: '专项练习', code: 'admin:practice:*' }] },
  { name: '用户与商业', items: [{ label: '用户管理', code: 'admin:user:*' }, { label: '订单查看', code: 'admin:order:read' }, { label: '挑战运营', code: 'admin:challenge:*' }] },
  { name: '游戏化运营', items: [{ label: '宠物', code: 'admin:pet:*' }, { label: '道具', code: 'admin:pet-item:*' }, { label: '装饰', code: 'admin:decoration:*' }, { label: '成就', code: 'admin:achievement:*' }, { label: '贴纸', code: 'admin:sticker:*' }, { label: '称号', code: 'admin:title:*' }] },
  { name: '系统设置', items: [{ label: '角色管理', code: 'admin:role:*' }, { label: '配置管理', code: 'admin:config:*' }, { label: '字典管理', code: 'admin:dict:*' }, { label: '版本发布', code: 'admin:version:*' }] },
].map(group => ({ ...group, codes: group.items.map(item => item.code) }))
const knownCodes = new Set(permissionGroups.flatMap(group => group.codes))

const form = reactive({ roleName: '', roleCode: '', permissions: '', description: '' })

async function fetchData() {
  loading.value = true
  try {
    const res = await getRoleList()
    if (res.code === 200) tableData.value = res.data
  } finally { loading.value = false }
}

function openDialog(row?: any) {
  if (row) { editingId.value = row.id; Object.assign(form, row) }
  else { editingId.value = null; Object.assign(form, { roleName: '', roleCode: '', permissions: '', description: '' }) }
  const current = parsePermissionText(form.permissions)
  baselinePermissions.value = [...current]
  selectedPermissions.value = current.filter(code => knownCodes.has(code))
  advancedPermissions.value = formatPermissionCodes(current.filter(code => !knownCodes.has(code)))
  dialogVisible.value = true
}

function applyPermissionPreset(permissions: string[]) {
  form.permissions = mergePermissionCodes(form.permissions, permissions)
  const current = parsePermissionText(form.permissions)
  selectedPermissions.value = current.filter(code => knownCodes.has(code))
  advancedPermissions.value = formatPermissionCodes(current.filter(code => !knownCodes.has(code)))
}

function isGroupSelected(codes: string[]) { return codes.every(code => selectedPermissions.value.includes(code)) }
function isGroupIndeterminate(codes: string[]) { const count = codes.filter(code => selectedPermissions.value.includes(code)).length; return count > 0 && count < codes.length }
function toggleGroup(codes: string[], checked: boolean) { selectedPermissions.value = checked ? [...new Set([...selectedPermissions.value, ...codes])] : selectedPermissions.value.filter(code => !codes.includes(code)) }
const currentPermissions = computed(() => [...selectedPermissions.value, ...parsePermissionText(advancedPermissions.value)])
const permissionDiff = computed(() => { const added = currentPermissions.value.filter(code => !baselinePermissions.value.includes(code)); const removed = baselinePermissions.value.filter(code => !currentPermissions.value.includes(code)); return [added.length ? `新增：${added.join('、')}` : '', removed.length ? `移除：${removed.join('、')}` : ''].filter(Boolean).join('；') })
watch([selectedPermissions, advancedPermissions], () => { form.permissions = formatPermissionCodes(currentPermissions.value) }, { deep: true })

async function handleSave() {
  saving.value = true
  try {
    const res = await saveRole({ ...form, id: editingId.value })
    if (res.code === 200) { ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() }
    else ElMessage.error(res.msg)
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  const res = await deleteRole(id)
  if (res.code === 200) { ElMessage.success('删除成功'); fetchData() }
}

onMounted(() => fetchData())
</script>

<style scoped>
.permission-editor {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}
.role-table { padding: var(--space-3); background: var(--admin-surface); border: 1px solid var(--admin-border); border-radius: var(--radius-card); }
.permission-matrix { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--space-3); width: 100%; }
.permission-matrix section { padding: var(--space-3); border: 1px solid var(--admin-border); border-radius: var(--radius-control); }
.permission-matrix header { display: flex; justify-content: space-between; margin-bottom: var(--space-2); }
.permission-matrix :deep(.el-checkbox-group) { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); }
@media (max-width: 700px) { .permission-matrix { grid-template-columns: 1fr; } }
</style>
