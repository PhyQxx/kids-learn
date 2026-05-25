<template>
  <div class="ai-config-page">
    <section class="page-bar">
      <div>
        <h2>AI 配置</h2>
        <p>配置保存到数据库 app_config，支持多家 OpenAI 兼容模型服务商。</p>
      </div>
      <div class="actions">
        <el-button :icon="Refresh" @click="fetchData" :loading="loading">刷新</el-button>
        <el-button type="primary" :icon="Check" @click="handleSave" :loading="saving">保存</el-button>
      </div>
    </section>

    <section class="summary-grid" v-loading="loading">
      <div class="summary-panel">
        <el-form label-position="top" :model="form">
          <el-form-item label="当前服务商">
            <el-select v-model="form.provider" class="wide" @change="activateProvider">
              <el-option
                v-for="item in form.providers"
                :key="item.provider"
                :label="item.name || item.provider"
                :value="item.provider"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="请求超时">
            <el-input-number v-model="form.timeout" :min="1" :max="120" controls-position="right" />
            <span class="unit">秒</span>
          </el-form-item>
        </el-form>
      </div>

      <div class="summary-panel status-panel">
        <div
          v-for="item in form.providers"
          :key="item.provider"
          class="provider-status"
          :class="{ active: item.provider === form.provider }"
        >
          <div>
            <div class="provider-name">{{ item.name || item.provider }}</div>
            <div class="provider-code">{{ item.provider }}</div>
          </div>
          <el-tag :type="item.apiKeyConfigured || item.apiKey ? 'success' : 'warning'" effect="plain">
            {{ item.apiKeyConfigured || item.apiKey ? '已配置' : '未配置' }}
          </el-tag>
        </div>
      </div>
    </section>

    <section class="provider-section">
      <el-table :data="form.providers" stripe>
        <el-table-column label="启用" width="88" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" />
          </template>
        </el-table-column>
        <el-table-column label="服务商" min-width="150">
          <template #default="{ row }">
            <div class="provider-name">{{ row.name || row.provider }}</div>
            <div class="provider-code">{{ row.provider }}</div>
          </template>
        </el-table-column>
        <el-table-column label="API 地址" min-width="260">
          <template #default="{ row }">
            <el-input v-model="row.baseUrl" placeholder="https://api.example.com" />
          </template>
        </el-table-column>
        <el-table-column label="模型" min-width="180">
          <template #default="{ row }">
            <el-input v-model="row.model" placeholder="model-name" />
          </template>
        </el-table-column>
        <el-table-column label="API Key" min-width="260">
          <template #default="{ row }">
            <div class="api-key-cell">
              <el-input
                v-model="row.apiKey"
                type="password"
                show-password
                clearable
                :placeholder="row.apiKeyConfigured ? '留空保持原密钥' : '请输入 API Key'"
              />
              <el-tag size="small" :type="row.apiKeyConfigured || row.apiKey ? 'success' : 'info'" effect="plain">
                {{ row.apiKeyConfigured || row.apiKey ? '已配置' : '未配置' }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Check, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getAiConfig, saveAiConfig } from '@/api/request'
import type { AiConfigPayload } from '@/api/request'

const loading = ref(false)
const saving = ref(false)
const form = reactive<AiConfigPayload>({
  provider: 'deepseek',
  timeout: 15,
  providers: [],
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getAiConfig()
    if (res.code === 200) {
      form.provider = res.data.provider
      form.timeout = res.data.timeout
      form.providers = res.data.providers.map(item => ({ ...item, apiKey: '' }))
    } else {
      ElMessage.error(res.msg || '加载失败')
    }
  } catch {
    form.providers = form.providers.length ? form.providers : []
  } finally {
    loading.value = false
  }
}

function activateProvider(provider: string) {
  const current = form.providers.find(item => item.provider === provider)
  if (current) current.enabled = true
}

async function handleSave() {
  const active = form.providers.find(item => item.provider === form.provider)
  if (!active) {
    ElMessage.error('请选择AI服务商')
    return
  }
  if (!active.baseUrl || !active.model) {
    ElMessage.error('请完善当前服务商的API地址和模型')
    return
  }

  saving.value = true
  try {
    const res = await saveAiConfig({
      provider: form.provider,
      timeout: form.timeout,
      providers: form.providers,
    })
    if (res.code === 200) {
      ElMessage.success('保存成功')
      await fetchData()
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch {
    // Request interceptor already shows the concrete error message.
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
}

.page-bar,
.summary-panel,
.provider-section {
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

.summary-grid {
  display: grid;
  grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
  gap: 18px;
}

.summary-panel {
  padding: 18px;
}

.wide {
  width: 100%;
}

.unit {
  margin-left: 10px;
  color: var(--admin-muted);
  font-size: 13px;
}

.status-panel {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 12px;
}

.provider-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 58px;
  padding: 10px 12px;
  border: 1px solid var(--admin-border);
  border-radius: 8px;
}

.provider-status.active {
  border-color: var(--primary);
  background: rgba(255, 107, 107, 0.06);
}

.provider-name {
  color: var(--admin-text);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.35;
}

.provider-code {
  margin-top: 3px;
  color: var(--admin-muted);
  font-size: 12px;
  line-height: 1.3;
}

.provider-section {
  padding: 12px;
}

.api-key-cell {
  display: grid;
  grid-template-columns: minmax(160px, 1fr) auto;
  align-items: center;
  gap: 8px;
}

@media (max-width: 960px) {
  .page-bar {
    align-items: flex-start;
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
