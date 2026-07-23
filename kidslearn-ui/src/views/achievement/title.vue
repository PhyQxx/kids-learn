<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">称号管理</span>
        <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B" @click="openDialog()">新增称号</el-button>
      </div>
    </template>
    <div ref="tableBox">
    <el-table :data="tableData" stripe v-loading="loading" :max-height="tableMaxHeight">
      <el-table-column label="图标" width="70">
        <template #default="{ row }">
          <div class="thumb-cell" @click="previewImage(row.titleIcon)">
            <img v-if="isImageUrl(row.titleIcon)" :src="row.titleIcon" class="thumb-img" />
            <span v-else class="thumb-emoji">{{ row.titleIcon || '👑' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="titleCode" label="称号代码" />
      <el-table-column prop="titleName" label="称号名称" />
      <el-table-column prop="titleColor" label="颜色" width="100">
        <template #default="{ row }"><span :style="{ color: row.titleColor, fontWeight: 'bold' }">{{ row.titleName }}</span></template>
      </el-table-column>
      <el-table-column prop="obtainType" label="获取方式" width="100">
        <template #default="{ row }">{{ ['', '成就解锁', '活动奖励', '手动发放'][row.obtainType] }}</template>
      </el-table-column>
      <el-table-column prop="isTimed" label="限时" width="80">
        <template #default="{ row }"><el-tag :type="row.isTimed ? 'warning' : 'info'" size="small">{{ row.isTimed ? '是' : '否' }}</el-tag></template>
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
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑称号' : '新增称号'" width="500">
      <el-form :model="form" label-width="80px">
        <el-form-item label="称号代码"><el-input v-model="form.titleCode" /></el-form-item>
        <el-form-item label="称号名称"><el-input v-model="form.titleName" /></el-form-item>
        <el-form-item label="展示颜色"><el-color-picker v-model="form.titleColor" /></el-form-item>
        <el-form-item label="图标">
          <ImageInput v-model="form.titleIcon" hint="称号图标" />
        </el-form-item>
        <el-form-item label="获取方式">
          <el-select v-model="form.obtainType"><el-option label="成就解锁" :value="1" /><el-option label="活动奖励" :value="2" /><el-option label="手动发放" :value="3" /></el-select>
        </el-form-item>
        <el-form-item label="关联成就">
          <el-select v-model="form.relatedAchieveId" clearable filterable placeholder="选择成就（成就解锁方式用）" style="width:100%">
            <el-option v-for="a in achievementList" :key="a.id" :label="a.achieveName" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="限时"><el-switch v-model="form.isTimed" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item v-if="form.isTimed === 1" label="有效时间">
          <el-date-picker
            v-model="validRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width:100%"
          />
        </el-form-item>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTitleList, saveTitle, deleteTitle, getAchievementList } from '@/api/request'
import { useTableHeight } from '@/composables/useTableHeight'
import ImageInput from '@/components/ImageInput.vue'

const { tableBox, tableMaxHeight } = useTableHeight()

const URL_RE = /^(https?:|data:|\/\/|\/static\/)/
function isImageUrl(value?: string) {
  return !!value && URL_RE.test(value)
}
function previewImage(src?: string) {
  if (!isImageUrl(src)) return
  const overlay = document.createElement('div')
  overlay.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.7);display:flex;align-items:center;justify-content:center;z-index:9999;cursor:pointer'
  overlay.onclick = () => document.body.removeChild(overlay)
  const img = document.createElement('img')
  img.src = src!
  img.style.cssText = 'max-width:90%;max-height:90%;object-fit:contain;border-radius:8px'
  img.onerror = () => document.body.removeChild(overlay)
  overlay.appendChild(img)
  document.body.appendChild(overlay)
}

const loading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const achievementList = ref<any[]>([])

const form = reactive({
  titleCode: '', titleName: '', titleColor: '#FF6B6B', titleIcon: '',
  obtainType: 1, relatedAchieveId: null as number | null,
  isTimed: 0, validStart: '' as string, validEnd: '' as string, status: 1
})

// 有效时间双向绑定（拆分/合并 validStart 与 validEnd）
const validRange = computed<[string, string] | null>({
  get() {
    return form.validStart && form.validEnd ? [form.validStart, form.validEnd] : null
  },
  set(val) {
    if (val && val.length === 2) {
      form.validStart = val[0]
      form.validEnd = val[1]
    } else {
      form.validStart = ''
      form.validEnd = ''
    }
  }
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getTitleList({ page: currentPage.value, pageSize: pageSize.value })
    if (res.code === 200) { tableData.value = res.data.list; total.value = res.data.total }
  } finally { loading.value = false }
}

async function fetchAchievements() {
  try {
    const res = await getAchievementList({ page: 1, pageSize: 200 })
    if (res.code === 200) achievementList.value = res.data.list
  } catch { /* ignore */ }
}

function openDialog(row?: any) {
  if (row) {
    editingId.value = row.id
    Object.assign(form, {
      titleCode: '', titleName: '', titleColor: '#FF6B6B', titleIcon: '',
      obtainType: 1, relatedAchieveId: null, isTimed: 0, validStart: '', validEnd: '', status: 1,
      ...row
    })
  } else {
    editingId.value = null
    Object.assign(form, { titleCode: '', titleName: '', titleColor: '#FF6B6B', titleIcon: '', obtainType: 1, relatedAchieveId: null, isTimed: 0, validStart: '', validEnd: '', status: 1 })
  }
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    const res = await saveTitle({ ...form, id: editingId.value })
    if (res.code === 200) { ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() }
    else ElMessage.error(res.msg)
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  const res = await deleteTitle(id)
  if (res.code === 200) { ElMessage.success('删除成功'); fetchData() }
}

onMounted(() => { fetchAchievements(); fetchData() })
</script>

<style scoped>
.thumb-cell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: #f7f8fa;
  cursor: pointer;
  overflow: hidden;
}

.thumb-cell:hover {
  transform: scale(1.05);
  transition: transform 0.2s;
}

.thumb-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.thumb-emoji {
  font-size: 24px;
  line-height: 1;
}
</style>
