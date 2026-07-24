<template>
  <div class="ai-config-page">
    <section class="page-bar">
      <div>
        <h2>AI 配置</h2>
        <p>按功能分类管理 AI 服务商，配置保存到数据库。</p>
      </div>
      <div class="actions">
        <el-button :icon="Refresh" @click="fetchData" :loading="loading">刷新</el-button>
        <el-button type="primary" :icon="Check" @click="handleSave" :loading="saving">保存</el-button>
      </div>
    </section>

    <!-- 全局设置 -->
    <section class="global-bar" v-loading="loading">
      <el-form inline>
        <el-form-item label="请求超时">
          <el-input-number v-model="form.timeout" :min="1" :max="120" controls-position="right" style="width: 120px" />
          <span class="unit">秒</span>
        </el-form-item>
      </el-form>
    </section>

    <!-- 分类 Tab -->
    <section class="category-section" v-loading="loading">
      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane
          v-for="cat in categories"
          :key="cat.type"
          :label="categoryIcon(cat.type) + ' ' + cat.label"
          :name="cat.type"
        >
          <div class="tab-toolbar">
            <div class="toolbar-left">
              <span class="toolbar-label">默认服务商：</span>
              <el-select v-model="formDefaults[cat.type]" style="width: 200px">
                <el-option
                  v-for="p in cat.providers"
                  :key="p.provider"
                  :label="p.name || p.provider"
                  :value="p.provider"
                />
              </el-select>
            </div>
            <el-button type="primary" plain @click="addProvider(cat.type)">+ 新增服务商</el-button>
          </div>

          <el-table :data="cat.providers" stripe :max-height="500">
            <el-table-column label="服务商" min-width="150">
              <template #default="{ row }">
                <el-input v-model="row.name" placeholder="显示名称" style="margin-bottom:4px" />
                <el-input v-model="row.provider" placeholder="标识符(英文)" size="small" />
              </template>
            </el-table-column>
            <el-table-column :label="cat.type === 'tts' ? '命令路径 / API 地址' : 'API 地址'" min-width="240">
              <template #default="{ row }">
                <el-input v-model="row.baseUrl" :placeholder="cat.type === 'tts' ? '命令路径或 API 地址' : 'https://api.example.com/v1'" />
              </template>
            </el-table-column>
            <el-table-column :label="cat.type === 'tts' ? '后端 / 模型' : '模型'" min-width="160">
              <template #default="{ row }">
                <el-input v-model="row.model" :placeholder="cat.type === 'tts' ? '后端或模型名' : 'model-name'" />
              </template>
            </el-table-column>
            <el-table-column label="API Key" min-width="220" v-if="cat.type !== 'tts'">
              <template #default="{ row }">
                <div class="api-key-cell">
                  <el-input v-model="row.apiKey" type="password" show-password clearable
                    :placeholder="row.apiKeyConfigured ? '留空保持原密钥' : '请输入 API Key'" />
                  <el-tag size="small" :type="row.apiKeyConfigured || row.apiKey ? 'success' : 'info'" effect="plain">
                    {{ row.apiKeyConfigured || row.apiKey ? '已配置' : '未配置' }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="API Key" min-width="200" v-if="cat.type === 'tts'">
              <template #default="{ row }">
                <div class="api-key-cell">
                  <el-input v-model="row.apiKey" type="password" show-password clearable
                    :placeholder="row.apiKeyConfigured ? '留空保持原密钥' : 'API Key（云端需要）'" />
                  <el-tag size="small" :type="row.apiKeyConfigured || row.apiKey ? 'success' : 'info'" effect="plain">
                    {{ row.apiKeyConfigured || row.apiKey ? '已配置' : '未配置' }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="语音" min-width="120" v-if="cat.type === 'tts'">
              <template #default="{ row }">
                <el-input v-model="row.voice" placeholder="语音名称" />
              </template>
            </el-table-column>
            <el-table-column label="最大Token" min-width="130" v-if="cat.type === 'text'">
              <template #default="{ row }">
                <el-input-number v-model="row.maxTokens" :min="0" :max="32768" :step="100" controls-position="right" size="small" placeholder="默认" style="width:110px" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center">
              <template #default="{ row }">
                <el-button type="danger" link @click="removeProvider(cat, row as AiProviderConfig)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Check, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getAiConfig, saveAiConfig } from '@/api/request'
import type { AiProviderConfig, AiCategoryGroup } from '@/api/request'

const loading = ref(false)
const saving = ref(false)
const activeTab = ref('text')

const form = reactive({ timeout: 15 })

// 各分类默认服务商
const formDefaults = reactive<Record<string, string>>({
  text: 'deepseek',
  image: 'zhipu',
  tts: 'mimo',
})

// 分类数据
const categories = ref<AiCategoryGroup[]>([])

const CATEGORY_ICONS: Record<string, string> = { text: '✍️', image: '🎨', tts: '🗣️' }
function categoryIcon(type: string) { return CATEGORY_ICONS[type] || '📦' }

async function fetchData() {
  loading.value = true
  try {
    const res = await getAiConfig()
    if (res.code === 200) {
      form.timeout = res.data.timeout
      formDefaults.text = res.data.provider || 'deepseek'
      formDefaults.image = res.data.imageProvider || 'zhipu'
      formDefaults.tts = res.data.ttsProvider || 'mimo'
      categories.value = res.data.categories.map(cat => ({
        ...cat,
        providers: cat.providers.map(p => ({ ...p, apiKey: '' })),
      }))
    } else {
      ElMessage.error(res.msg || '加载失败')
    }
  } catch {
    // interceptor shows error
  } finally {
    loading.value = false
  }
}

function addProvider(category: string) {
  const cat = categories.value.find(c => c.type === category)
  if (!cat) return
  cat.providers.push({
    provider: '',
    name: '',
    category,
    enabled: true,
    baseUrl: '',
    model: '',
    apiKey: '',
    apiKeyConfigured: false,
    voice: '',
  })
}

function removeProvider(cat: AiCategoryGroup, row: AiProviderConfig) {
  const idx = cat.providers.indexOf(row)
  if (idx >= 0) cat.providers.splice(idx, 1)
}

async function handleSave() {
  saving.value = true
  try {
    // 扁平化所有 providers
    const allProviders: AiProviderConfig[] = []
    for (const cat of categories.value) {
      for (const p of cat.providers) {
        allProviders.push({ ...p, category: cat.type, enabled: true })
      }
    }

    const res = await saveAiConfig({
      provider: formDefaults.text,
      imageProvider: formDefaults.image,
      ttsProvider: formDefaults.tts,
      timeout: form.timeout,
      providers: allProviders,
    })
    if (res.code === 200) {
      ElMessage.success('保存成功')
      await fetchData()
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch {
    // interceptor shows error
  } finally {
    saving.value = false
  }
}

onMounted(() => fetchData())
</script>

<style scoped>
.ai-config-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
  height: 100%;
}

.page-bar,
.global-bar,
.category-section {
  background: #fff;
  border: 1px solid var(--admin-border);
  border-radius: 8px;
}

.page-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
}

.page-bar h2 {
  margin: 0;
  color: var(--admin-text);
  font-size: 20px;
  line-height: 1.35;
}

.page-bar p {
  margin: 6px 0 0;
  color: var(--admin-muted);
  font-size: 13px;
}

.actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.global-bar {
  padding: 16px 20px;
}

.global-bar .el-form-item {
  margin-bottom: 0;
}

.unit {
  margin-left: 10px;
  color: var(--admin-muted);
  font-size: 13px;
}

.category-section {
  padding: 0;
  overflow: hidden;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.category-section :deep(.el-tabs) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.category-section :deep(.el-tabs__content) {
  flex: 1;
  overflow: hidden;
}

.category-section :deep(.el-tab-pane) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.category-section :deep(.el-table) {
  flex: 1;
}

.category-section :deep(.el-tabs__content) {
  padding: 18px;
}

.tab-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-label {
  font-size: 14px;
  color: var(--admin-text);
  white-space: nowrap;
}

.api-key-cell {
  display: grid;
  grid-template-columns: minmax(140px, 1fr) auto;
  align-items: center;
  gap: 8px;
}

@media (max-width: 960px) {
  .page-bar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
