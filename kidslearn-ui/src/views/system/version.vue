<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">版本管理</span>
        <el-button type="primary" @click="openDialog()">新增版本</el-button>
      </div>
    </template>
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="versionName" label="版本号" width="120" />
      <el-table-column prop="versionCode" label="版本编号" width="100" />
      <el-table-column prop="platform" label="平台" width="100">
        <template #default="{ row }">
          <el-tag :type="row.platform === 'ios' ? 'success' : 'primary'" size="small">
            {{ row.platform === 'ios' ? 'iOS' : 'Android' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="packageType" label="包类型" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ row.packageType?.toUpperCase() }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="forceUpdate" label="强制更新" width="100">
        <template #default="{ row }">
          <el-tag :type="row.forceUpdate === 1 ? 'danger' : 'info'" size="small">
            {{ row.forceUpdate === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="downloadUrl" label="下载地址" show-overflow-tooltip />
      <el-table-column prop="updateLog" label="更新日志" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-popconfirm title="确定删除此版本？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-if="total > 0" style="margin-top:16px;justify-content:flex-end" :total="total" :page-size="pageSize"
      v-model:current-page="currentPage" layout="total, prev, pager, next" @current-change="fetchData" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑版本' : '新增版本'" width="560">
      <el-form :model="form" label-width="100px">
        <el-form-item label="版本号" required>
          <el-input v-model="form.versionName" placeholder="如 1.0.1" />
        </el-form-item>
        <el-form-item label="版本编号" required>
          <el-input-number v-model="form.versionCode" :min="1" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="平台" required>
          <el-select v-model="form.platform" style="width:100%">
            <el-option label="Android" value="android" />
            <el-option label="iOS" value="ios" />
          </el-select>
        </el-form-item>
        <el-form-item label="包类型" required>
          <el-select v-model="form.packageType" style="width:100%">
            <el-option label="WGT（热更新）" value="wgt" />
            <el-option label="APK（整包）" value="apk" />
          </el-select>
        </el-form-item>
        <el-form-item label="下载地址" required>
          <el-input v-model="form.downloadUrl" placeholder="wgt/apk 文件下载地址" />
        </el-form-item>
        <el-form-item label="更新日志">
          <el-input v-model="form.updateLog" type="textarea" :rows="3" placeholder="描述此版本的更新内容" />
        </el-form-item>
        <el-form-item label="强制更新">
          <el-switch v-model="form.forceUpdate" :active-value="1" :inactive-value="0" active-text="是" inactive-text="否" />
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
import { ElMessage } from 'element-plus'
import { getVersionList, saveVersion, deleteVersion } from '@/api/request'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const dialogVisible = ref(false)

const form = reactive({
  id: null as number | null,
  versionName: '',
  versionCode: 100,
  platform: 'android',
  downloadUrl: '',
  updateLog: '',
  forceUpdate: 0,
  packageType: 'wgt'
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getVersionList({ page: currentPage.value, pageSize: pageSize.value })
    if (res.code === 200) {
      tableData.value = res.data.list
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

function openDialog(row?: any) {
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, {
      id: null,
      versionName: '',
      versionCode: 100,
      platform: 'android',
      downloadUrl: '',
      updateLog: '',
      forceUpdate: 0,
      packageType: 'wgt'
    })
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.versionName || !form.downloadUrl) {
    ElMessage.warning('请填写必填字段')
    return
  }
  saving.value = true
  try {
    const res = await saveVersion({ ...form })
    if (res.code === 200) {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res.msg)
    }
  } finally {
    saving.value = false
  }
}

async function handleDelete(id: number) {
  const res = await deleteVersion(id)
  if (res.code === 200) {
    ElMessage.success('删除成功')
    fetchData()
  } else {
    ElMessage.error(res.msg)
  }
}

onMounted(() => fetchData())
</script>
