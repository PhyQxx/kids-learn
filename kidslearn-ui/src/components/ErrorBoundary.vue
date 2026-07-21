<template>
  <div v-if="hasError" class="error-boundary">
    <div class="error-content">
      <div class="error-icon">⚠️</div>
      <h2>页面出错了</h2>
      <p class="error-message">{{ errorMessage }}</p>
      <div class="error-actions">
        <el-button type="primary" @click="handleReload">刷新页面</el-button>
        <el-button @click="handleGoHome">返回首页</el-button>
      </div>
      <el-collapse v-if="errorStack" class="error-stack-collapse">
        <el-collapse-item title="错误详情">
          <pre class="error-stack">{{ errorStack }}</pre>
        </el-collapse-item>
      </el-collapse>
    </div>
  </div>
  <slot v-else />
</template>

<script setup lang="ts">
import { ref, onErrorCaptured } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const hasError = ref(false)
const errorMessage = ref('')
const errorStack = ref('')

onErrorCaptured((err, instance, info) => {
  hasError.value = true
  errorMessage.value = err.message || '未知错误'
  errorStack.value = err.stack || ''

  // 上报错误（可选）
  console.error('ErrorBoundary caught:', err, info)

  // 阻止错误继续传播
  return false
})

function handleReload() {
  window.location.reload()
}

function handleGoHome() {
  router.push('/dashboard')
}
</script>

<style scoped>
.error-boundary {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  padding: 40px;
}

.error-content {
  text-align: center;
  max-width: 500px;
}

.error-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.error-content h2 {
  font-size: 24px;
  color: #303133;
  margin-bottom: 12px;
}

.error-message {
  color: #606266;
  margin-bottom: 24px;
}

.error-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-bottom: 24px;
}

.error-stack-collapse {
  text-align: left;
  margin-top: 20px;
}

.error-stack {
  font-size: 12px;
  color: #909399;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 300px;
  overflow-y: auto;
}
</style>
