<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <span style="font-size:15px;font-weight:600">{{ subject.subjectName }} - 关卡列表</span>
      <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B" @click="openDialog()">新增关卡</el-button>
    </div>

    <el-alert type="info" :closable="false" style="margin-bottom:12px">
      关卡配置适用于<b>所有年级</b>。答题时系统会根据学生当前年级，从题库中随机抽取<b>基础12题+高阶3题</b>。
    </el-alert>

    <div ref="tableBox">
      <el-table :data="tableData" stripe v-loading="loading" :max-height="tableMaxHeight">
        <el-table-column prop="levelNum" label="关卡号" width="100">
          <template #default="{ row }">第{{ cnNum(row.levelNum) }}关</template>
        </el-table-column>
        <el-table-column prop="levelName" label="关卡名称" width="140" />
        <el-table-column label="题目配置" width="200">
          <template #default="{ row }">
            <el-tag type="success" size="small">基础 {{ row.baseQuestionCount || 0 }} 题</el-tag>
            <el-tag type="warning" size="small" style="margin-left:6px">高阶 {{ row.advancedQuestionCount || 0 }} 题</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="passScore" label="及格分" width="90" />
        <el-table-column prop="expReward" label="经验奖励" width="90" />
        <el-table-column prop="goldReward" label="金币奖励" width="90" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-if="total > 0" style="margin-top:16px;justify-content:flex-end" :total="total" :page-size="pageSize"
        v-model:current-page="currentPage" layout="total, prev, pager, next" @current-change="fetchData" />
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑关卡' : '新增关卡'" width="600">
      <el-form :model="form" label-width="100px">
        <el-form-item label="所属学科">
          <el-select v-model="form.subjectId" style="width:100%" disabled>
            <el-option :label="subject.subjectName" :value="subject.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关卡序号"><el-input-number v-model="form.levelNum" :min="1" :max="20" /></el-form-item>
        <el-form-item label="关卡名称">
          <el-input v-model="form.levelName" placeholder="如：第一关" />
          <span style="font-size:12px;color:#999">建议按序号命名，如"第一关"、"第二关"</span>
        </el-form-item>
        <el-form-item label="关卡描述"><el-input v-model="form.levelDesc" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="基础题数量">
          <el-input-number v-model="form.baseQuestionCount" :min="1" />
          <span style="margin-left: 8px; font-size: 12px; color: #999;">从学生当前年级抽取</span>
        </el-form-item>
        <el-form-item label="高阶题数量">
          <el-input-number v-model="form.advancedQuestionCount" :min="0" />
          <span style="margin-left: 8px; font-size: 12px; color: #999;">从学生下一年级抽取</span>
        </el-form-item>
        <el-form-item label="及格分"><el-input-number v-model="form.passScore" :min="0" :max="100" /></el-form-item>
        <el-form-item label="星级门槛"><el-input v-model="form.starThresholds" placeholder="如 60,80,100" /></el-form-item>
        <el-form-item label="经验奖励"><el-input-number v-model="form.expReward" :min="0" /></el-form-item>
        <el-form-item label="金币奖励"><el-input-number v-model="form.goldReward" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getLevelList, saveLevel, deleteLevel } from '@/api/request'
import { useTableHeight } from '@/composables/useTableHeight'

const { tableBox, tableMaxHeight } = useTableHeight()

const props = defineProps<{
  subject: any
}>()

const loading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(50)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  subjectId: null as number | null,
  levelNum: 1, levelName: '', levelDesc: '',
  baseQuestionCount: 12, advancedQuestionCount: 3, passScore: 60, starThresholds: '60,80,100', expReward: 10, goldReward: 10, status: 1
})

const cnNumMap = ['零', '一', '二', '三', '四', '五', '六', '七', '八', '九', '十',
  '十一', '十二', '十三', '十四', '十五', '十六', '十七', '十八', '十九', '二十']
function cnNum(n: number) {
  return cnNumMap[n] || String(n)
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getLevelList({
      page: currentPage.value,
      pageSize: pageSize.value,
      subjectId: props.subject.id,
    })
    if (res.code === 200) { tableData.value = res.data.list; total.value = res.data.total }
  } finally { loading.value = false }
}

function openDialog(row?: any) {
  if (row) {
    editingId.value = row.id
    Object.assign(form, row)
  } else {
    // 新增时自动推荐下一个关卡号和名称
    const nextNum = (tableData.value.length > 0 ? Math.max(...tableData.value.map(r => r.levelNum || 0)) : 0) + 1
    editingId.value = null
    Object.assign(form, {
      subjectId: props.subject.id,
      levelNum: nextNum,
      levelName: `第${cnNum(nextNum)}关`,
      levelDesc: '',
      baseQuestionCount: 12, advancedQuestionCount: 3, passScore: 60, starThresholds: '60,80,100', expReward: 10, goldReward: 10, status: 1
    })
  }
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    const res = await saveLevel({ ...form, id: editingId.value })
    if (res.code === 200) { ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() }
    else ElMessage.error(res.msg)
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除此关卡？', '提示', { type: 'warning' })
  const res = await deleteLevel(id)
  if (res.code === 200) { ElMessage.success('删除成功'); fetchData() }
}

onMounted(() => fetchData())
</script>
