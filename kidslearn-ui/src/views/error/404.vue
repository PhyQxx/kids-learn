<template>
  <div class="error-page">
    <div class="error-content">
      <div class="error-emoji">🔍</div>
      <h1 class="error-code">404</h1>
      <p class="error-message">页面走丢了</p>
      <p class="error-desc">抱歉，您访问的页面不存在或已被移除</p>
      <el-button type="primary" @click="goHome">
        返回首页
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.push('/dashboard')
}
</script>

<style scoped>
.error-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--admin-bg);
}

.error-content {
  text-align: center;
  padding: 40px;
}

.error-emoji {
  font-size: 80px;
  margin-bottom: 20px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.error-code {
  font-size: 72px;
  font-weight: 800;
  color: var(--primary);
  margin: 0;
  line-height: 1;
}

.error-message {
  font-size: 24px;
  font-weight: 600;
  color: var(--admin-text);
  margin: 16px 0 8px;
}

.error-desc {
  font-size: 14px;
  color: var(--admin-muted);
  margin-bottom: 32px;
}
</style>
