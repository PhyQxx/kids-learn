<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:700">成就管理</span>
        <el-button type="primary" style="background:#FF6B6B;border-color:#FF6B6B" @click="openDialog()">新增成就</el-button>
      </div>
    </template>
    <div ref="tableBox">
    <el-table :data="tableData" stripe v-loading="loading" :max-height="tableMaxHeight">
      <el-table-column label="图标" width="80">
        <template #default="{ row }">
          <div class="thumb-cell" @click="previewImage(row.achieveIcon)">
            <img v-if="isImageUrl(row.achieveIcon)" :src="row.achieveIcon" class="thumb-img" />
            <span v-else class="thumb-emoji">{{ row.achieveIcon || '🏅' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="achieveCode" label="成就代码" />
      <el-table-column prop="achieveName" label="成就名称" />
      <el-table-column prop="achieveDesc" label="描述" show-overflow-tooltip />
      <el-table-column prop="achieveType" label="类型" width="80">
        <template #default="{ row }">{{ ['', '学习', '收集', '社交', '时长', '特殊'][row.achieveType] }}</template>
      </el-table-column>
      <el-table-column prop="isTiered" label="分级" width="80">
        <template #default="{ row }"><el-tag :type="row.isTiered ? 'success' : 'info'" size="small">{{ row.isTiered ? '是' : '否' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="70" />
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑成就' : '新增成就'" width="720" :close-on-click-modal="false">
      <el-form :model="form" label-width="80px">
        <el-form-item label="成就代码"><el-input v-model="form.achieveCode" /></el-form-item>
        <el-form-item label="成就名称"><el-input v-model="form.achieveName" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.achieveDesc" type="textarea" :autosize="{ minRows: 1, maxRows: 3 }" /></el-form-item>
        <el-form-item label="图标">
          <ImageInput v-model="form.achieveIcon" hint="成就图标" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.achieveType">
            <el-option label="学习" :value="1" /><el-option label="收集" :value="2" /><el-option label="社交" :value="3" /><el-option label="时长" :value="4" /><el-option label="特殊" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="分级"><el-switch v-model="form.isTiered" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>

        <!-- 分级档位配置 -->
        <el-divider v-if="form.isTiered === 1" content-position="left">分级档位（铜/银/金）</el-divider>
        <div v-if="form.isTiered === 1" class="tier-list">
          <div v-for="(tier, idx) in tiers" :key="idx" class="tier-block">
            <div class="tier-header">
              <span class="tier-title">档位 {{ idx + 1 }}</span>
              <el-button link type="danger" :disabled="tiers.length <= 1" @click="removeTier(idx)">删除</el-button>
            </div>
            <el-form-item label="档位级别" label-width="90px">
              <el-select v-model="tier.tierLevel" style="width:160px">
                <el-option label="1 · 铜档" :value="1" />
                <el-option label="2 · 银档" :value="2" />
                <el-option label="3 · 金档" :value="3" />
                <el-option label="4 · 传说" :value="4" />
              </el-select>
            </el-form-item>
            <el-form-item label="档位名称" label-width="90px">
              <el-input v-model="tier.tierName" placeholder="如：青铜学徒" style="width:220px" />
            </el-form-item>

            <!-- 达成条件（表单化） -->
            <el-form-item label="达成条件" label-width="90px">
              <div class="inline-form">
                <el-select v-model="tier.condition.type" placeholder="条件类型" style="width:200px">
                  <el-option v-for="c in conditionTypes" :key="c.value" :label="c.label" :value="c.value" />
                </el-select>
                <el-input-number v-model="tier.condition.target" :min="1" placeholder="目标值" />
                <span class="hint-text">{{ conditionHint(tier.condition.type) }}</span>
              </div>
            </el-form-item>

            <!-- 奖励内容（表单化动态列表） -->
            <el-form-item label="奖励内容" label-width="90px">
              <div class="reward-list">
                <div v-for="(reward, rIdx) in tier.rewards" :key="rIdx" class="reward-row">
                  <el-select v-model="reward.type" placeholder="奖励类型" style="width:130px">
                    <el-option v-for="r in rewardTypes" :key="r.value" :label="r.label" :value="r.value" />
                  </el-select>
                  <el-input-number v-model="reward.value" :min="1" placeholder="数量" style="width:130px" />
                  <el-input-number v-if="reward.type === 'STICKER' || reward.type === 'TITLE'"
                    v-model="reward.itemId" :min="1" placeholder="目标ID" style="width:130px" />
                  <el-button link type="danger" @click="tier.rewards.splice(rIdx, 1)">删除</el-button>
                </div>
                <el-button size="small" @click="tier.rewards.push({ type: 'GOLD', value: 100, itemId: null })">+ 添加奖励</el-button>
              </div>
            </el-form-item>
          </div>
          <el-button type="primary" plain size="small" @click="addTier">+ 添加档位</el-button>
        </div>
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
import { getAchievementList, getAchievementDetail, saveAchievement, deleteAchievement } from '@/api/request'
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

// 条件类型枚举（对应后端 AchievementRuleEngine.normalizeType）
const conditionTypes = [
  { value: 'COMPLETE_LEVEL', label: '通关关卡数' },
  { value: 'THREE_STAR', label: '三星通关数' },
  { value: 'PERFECT_SCORE', label: '满分通关数' },
  { value: 'STREAK_DAYS', label: '连续学习天数' },
  { value: 'STICKER_COUNT', label: '收集贴纸数' },
  { value: 'SUBJECT_COUNT', label: '学习科目数' },
  { value: 'RANK_TOP', label: '排行榜名次' }
]
const conditionHintMap: Record<string, string> = {
  COMPLETE_LEVEL: '累计通关的关卡数量',
  THREE_STAR: '获得三星的关卡数量',
  PERFECT_SCORE: '满分（全对）的关卡数量',
  STREAK_DAYS: '连续学习的天数',
  STICKER_COUNT: '收集到的贴纸总数',
  SUBJECT_COUNT: '已学习的科目数量',
  RANK_TOP: '排行榜最高名次（第N名）'
}
function conditionHint(type: string) {
  return conditionHintMap[type] || ''
}

// 奖励类型枚举
const rewardTypes = [
  { value: 'GOLD', label: '金币' },
  { value: 'EXP', label: '经验' },
  { value: 'DIAMOND', label: '钻石' },
  { value: 'STICKER', label: '贴纸' },
  { value: 'TITLE', label: '称号' }
]

const form = reactive({
  achieveCode: '', achieveName: '', achieveDesc: '', achieveIcon: '',
  achieveType: 1, isTiered: 0, sortOrder: 0, status: 1
})

// 分级档位（表单结构，保存时序列化为 conditionJson / rewardJson）
const tiers = ref<any[]>([])

function emptyTier() {
  return {
    tierLevel: tiers.value.length + 1,
    tierName: '',
    condition: { type: 'COMPLETE_LEVEL', target: 10 },
    rewards: [{ type: 'GOLD', value: 100, itemId: null }]
  }
}
function addTier() {
  tiers.value.push(emptyTier())
}
function removeTier(idx: number) {
  tiers.value.splice(idx, 1)
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getAchievementList({ page: currentPage.value, pageSize: pageSize.value })
    if (res.code === 200) { tableData.value = res.data.list; total.value = res.data.total }
  } finally { loading.value = false }
}

function resetForm() {
  Object.assign(form, {
    achieveCode: '', achieveName: '', achieveDesc: '', achieveIcon: '',
    achieveType: 1, isTiered: 0, sortOrder: 0, status: 1
  })
  tiers.value = []
}

async function openDialog(row?: any) {
  if (row) {
    editingId.value = row.id
    Object.assign(form, row)
    // 拉取详情回显 tiers
    tiers.value = []
    try {
      const res = await getAchievementDetail(row.id)
      if (res.code === 200 && res.data?.tiers) {
        tiers.value = (res.data.tiers as any[]).map(parseTierFromServer)
      }
    } catch (e) {
      // 详情拉取失败不阻塞编辑
    }
    // 非分级成就也允许配一档（兼容旧数据），分级成就至少有一档
    if (tiers.value.length === 0) {
      addTier()
    }
  } else {
    editingId.value = null
    resetForm()
  }
  dialogVisible.value = true
}

/** 把后端 tier（conditionJson/rewardJson 字符串）解析为表单结构 */
function parseTierFromServer(t: any) {
  let condition = { type: 'COMPLETE_LEVEL', target: 10 }
  try {
    const c = typeof t.conditionJson === 'string' ? JSON.parse(t.conditionJson) : t.conditionJson
    if (c && typeof c === 'object') {
      condition = {
        type: c.type || c.conditionType || 'COMPLETE_LEVEL',
        target: Number(c.target || c.targetValue || c.count || c.value || c.levelCount || c.starCount || c.stickerCount || c.subjectCount || c.days || c.rank || 10)
      }
    }
  } catch { /* ignore */ }

  let rewards: any[] = []
  try {
    const r = typeof t.rewardJson === 'string' ? JSON.parse(t.rewardJson) : t.rewardJson
    const items = r?.rewards || r?.items || (Array.isArray(r) ? r : [])
    rewards = items.map((it: any) => ({
      type: (it.type || 'GOLD').toUpperCase(),
      value: Number(it.value || it.quantity || it.amount || it.count || 0),
      itemId: Number(it.itemId || it.rewardItemId || it.id || it.stickerId || it.titleId) || null
    }))
    if (rewards.length === 0) rewards = [{ type: 'GOLD', value: 100, itemId: null }]
  } catch {
    rewards = [{ type: 'GOLD', value: 100, itemId: null }]
  }

  return {
    id: t.id,
    tierLevel: t.tierLevel || 1,
    tierName: t.tierName || '',
    condition,
    rewards
  }
}

/** 把表单结构序列化为后端期望的 tiers 结构 */
function buildTiersForSave() {
  return tiers.value.map(t => ({
    id: t.id,
    tierLevel: t.tierLevel,
    tierName: t.tierName,
    conditionJson: JSON.stringify({ type: t.condition.type, target: t.condition.target }),
    rewardJson: JSON.stringify({
      rewards: t.rewards
        .filter((r: any) => r.type && r.value > 0)
        .map((r: any) => {
          const item: any = { type: r.type, value: r.value }
          if ((r.type === 'STICKER' || r.type === 'TITLE') && r.itemId) {
            item[r.type === 'STICKER' ? 'stickerId' : 'titleId'] = r.itemId
          }
          return item
        })
    })
  }))
}

async function handleSave() {
  saving.value = true
  try {
    const payload: any = { ...form, id: editingId.value }
    if (form.isTiered === 1) {
      payload.tiers = buildTiersForSave()
    }
    const res = await saveAchievement(payload)
    if (res.code === 200) { ElMessage.success('保存成功'); dialogVisible.value = false; fetchData() }
    else ElMessage.error(res.msg)
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除？删除后分级配置也会一并清除。', '提示', { type: 'warning' })
  const res = await deleteAchievement(id)
  if (res.code === 200) { ElMessage.success('删除成功'); fetchData() }
}

onMounted(() => fetchData())
</script>

<style scoped>
.thumb-cell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 50px;
  height: 50px;
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
  font-size: 28px;
  line-height: 1;
}

.tier-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 12px;
}

.tier-block {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 16px 16px 4px;
  background: #fafbfc;
}

.tier-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.tier-title {
  font-weight: 600;
  color: #303133;
}

.inline-form {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.hint-text {
  font-size: 12px;
  color: #909399;
}

.reward-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.reward-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
