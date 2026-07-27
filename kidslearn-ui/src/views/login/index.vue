<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <span class="login-logo" aria-hidden="true">K</span>
        <h2>趣学星球</h2>
        <p>管理后台 · 安全登录</p>
      </div>
      <el-alert v-if="loginError" class="login-error" type="error" :title="loginError" show-icon :closable="false" />
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="login-form">
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            autocomplete="username"
            placeholder="例如：admin"
            :prefix-icon="User"
            size="large"
            @input="loginError = ''"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            autocomplete="current-password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            size="large"
            show-password
            @keydown="detectCapsLock"
            @keyup="detectCapsLock"
            @keyup.enter="handleLogin"
            @input="loginError = ''"
          />
          <p v-if="capsLockOn" class="caps-lock-tip" role="status">Caps Lock 已开启</p>
        </el-form-item>
        <el-form-item>
          <el-button class="login-submit" type="primary" size="large" :loading="loading" @click="handleLogin">
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-help">
        <span>账号问题请联系系统管理员</span>
        <span>{{ environmentLabel }} · v1.0.0</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/request'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const capsLockOn = ref(false)
const loginError = ref('')
const environmentLabel = import.meta.env.MODE === 'production' ? '生产环境' : '开发环境'

const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  loginError.value = ''
  try {
    const res = await login(form)
    if (res.code === 200) {
      // 统一写入 token 对，并记录过期时间戳供主动续期使用
      const expiresIn = res.data.expiresIn || 7200
      userStore.setTokens(
        res.data.accessToken,
        res.data.refreshToken,
        Date.now() + expiresIn * 1000
      )
      userStore.setUserInfo(res.data.userInfo)
      ElMessage.success('登录成功')
      router.push('/dashboard')
    } else {
      loginError.value = res.msg || '用户名或密码错误，请检查后重试。'
    }
  } catch (e: any) {
    loginError.value = e.message || '暂时无法连接服务器，请稍后重试。'
    form.password = ''
  } finally {
    loading.value = false
  }
}

function detectCapsLock(event: Event | KeyboardEvent) {
  capsLockOn.value = (event as KeyboardEvent).getModifierState?.('CapsLock') || false
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-4);
  background:
    radial-gradient(circle at 20% 10%, rgba(255, 255, 255, 0.24), transparent 32%),
    linear-gradient(135deg, var(--color-brand-700), var(--color-brand-500));
}

.login-card {
  width: min(400px, calc(100vw - 32px));
  padding: var(--space-8);
  background: var(--admin-surface);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-popover);
}

.login-header {
  text-align: center;
  margin-bottom: var(--space-6);
}

.login-logo {
  width: 48px;
  height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--color-gray-0);
  font-size: 22px;
  font-weight: 800;
  background: linear-gradient(135deg, var(--color-brand-600), var(--teal));
  border-radius: var(--radius-card);
}

.login-header h2 {
  margin: var(--space-3) 0 var(--space-1);
  color: var(--admin-text);
  font-size: 24px;
}

.login-header p {
  margin: 0;
  color: var(--admin-muted);
  font-size: var(--font-size-body);
}

.login-submit {
  width: 100%;
}

.login-form :deep(.el-form-item) {
  margin-bottom: var(--space-5);
}

.login-form :deep(.el-form-item__label) {
  margin-bottom: var(--space-1);
  line-height: var(--line-height-body);
}

.login-error {
  margin-bottom: var(--space-4);
}

.caps-lock-tip {
  position: absolute;
  right: 0;
  top: 100%;
  margin: 2px 0 0;
  color: var(--color-warning-700);
  font-size: var(--font-size-caption);
}

.login-help {
  display: flex;
  justify-content: space-between;
  gap: var(--space-3);
  padding-top: var(--space-4);
  color: var(--admin-muted);
  font-size: var(--font-size-caption);
  border-top: 1px solid var(--admin-border);
}

@media (max-width: 480px) {
  .login-container {
    align-items: flex-start;
    padding-top: max(12vh, 72px);
  }

  .login-card {
    padding: var(--space-6) var(--space-5);
  }

  .login-help {
    flex-direction: column;
    text-align: center;
  }
}
</style>
