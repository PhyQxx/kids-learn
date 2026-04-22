<template>
  <view class="register-page">
    <view class="bg-decor">
      <view class="decor-circle c1"></view>
      <view class="decor-circle c2"></view>
      <view class="decor-circle c3"></view>
    </view>

    <view class="register-card">
      <!-- 左侧品牌 -->
      <view class="brand-side">
        <text class="brand-emoji">🌍</text>
        <text class="text-xl text-bold text-white">趣学星球</text>
        <text class="text-sm text-white" style="opacity: 0.8;">加入我们，开启学习之旅</text>
        <view class="features">
          <text class="text-sm text-white" style="opacity: 0.7;">📚 丰富课程</text>
          <text class="text-sm text-white" style="opacity: 0.7;">🎮 趣味学习</text>
          <text class="text-sm text-white" style="opacity: 0.7;">🐱 宠物陪伴</text>
          <text class="text-sm text-white" style="opacity: 0.7;">🏆 排行竞技</text>
        </view>
      </view>

      <!-- 右侧表单 -->
      <view class="form-side">
        <text class="text-xl text-bold">创建账号</text>
        <text class="text-sm text-light">填写信息完成注册</text>

        <!-- 账号 -->
        <view class="form-group">
          <text class="text-sm text-bold">账号</text>
          <input class="input" v-model="username" placeholder="请输入登录账号" placeholder-class="input-placeholder" />
        </view>

        <!-- 昵称 -->
        <view class="form-group">
          <text class="text-sm text-bold">昵称</text>
          <input class="input" v-model="nickname" placeholder="给孩子取个名字" placeholder-class="input-placeholder" />
        </view>

        <!-- 年龄组 -->
        <view class="form-group">
          <text class="text-sm text-bold">选择年龄组</text>
          <view class="age-group-row">
            <view
              v-for="g in ageGroups"
              :key="g.key"
              class="age-card"
              :class="{ active: selectedAge === g.key }"
              @tap="selectedAge = g.key"
            >
              <text class="age-emoji">{{ g.icon }}</text>
              <text class="text-xs">{{ g.name }}</text>
            </view>
          </view>
        </view>

        <!-- 手机号 -->
        <view class="form-group">
          <text class="text-sm text-bold">手机号</text>
          <input class="input" v-model="phone" placeholder="请输入手机号" placeholder-class="input-placeholder" type="number" />
        </view>

        <!-- 验证码 -->
        <view class="form-group">
          <text class="text-sm text-bold">验证码</text>
          <view class="sms-row">
            <input class="input" v-model="code" placeholder="请输入验证码" placeholder-class="input-placeholder" style="flex: 1;" />
            <view class="btn btn-outline-primary btn-pill" @tap="sendCode">
              <text class="text-sm">{{ codeSent ? `${countdown}s` : '发送验证码' }}</text>
            </view>
          </view>
        </view>

        <!-- 密码 -->
        <view class="form-group">
          <text class="text-sm text-bold">密码</text>
          <input class="input" v-model="password" placeholder="请设置密码（6位以上）" placeholder-class="input-placeholder" type="password" />
        </view>

        <view class="btn btn-primary btn-lg btn-block" @tap="doRegister">
          <text class="text-white text-md">注册</text>
        </view>

        <text v-if="errorMsg" class="text-xs text-error">{{ errorMsg }}</text>

        <view class="login-link">
          <text class="text-sm text-light">已有账号？</text>
          <text class="text-sm text-primary" @tap="goLogin">去登录</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { register as registerApi } from '@/api/auth'
import { useUserStore } from '@/store/user'

const nickname = ref('')
const phone = ref('')
const code = ref('')
const password = ref('')
const username = ref('')
const selectedAge = ref(2)
const codeSent = ref(false)
const countdown = ref(60)
const errorMsg = ref('')

const ageGroups = ref([
  { key: 1, name: '幼幼组', icon: '👶' },
  { key: 2, name: '低龄组', icon: '🧒' },
  { key: 3, name: '高龄组', icon: '👦' }
])

onMounted(() => {
  const pages = getCurrentPages()
  const page = pages[pages.length - 1]
  const group = page.$page?.options?.learnAgeGroup
  if (group) selectedAge.value = parseInt(group)
})

function sendCode() {
  if (codeSent.value || !phone.value) return
  codeSent.value = true
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
      codeSent.value = false
      countdown.value = 60
    }
  }, 1000)
}

async function doRegister() {
  if (!username.value) { errorMsg.value = '请输入账号'; return }
  if (!nickname.value) { errorMsg.value = '请输入昵称'; return }
  if (!phone.value) { errorMsg.value = '请输入手机号'; return }
  if (!code.value) { errorMsg.value = '请输入验证码'; return }
  if (!password.value || password.value.length < 6) { errorMsg.value = '密码至少6位'; return }

  errorMsg.value = ''
  uni.showLoading({ title: '注册中...' })
  try {
    const res = await registerApi({
      username: username.value,
      password: password.value,
      nickname: nickname.value,
      userType: 1,
      loginType: 1,
      phone: phone.value,
      learnAgeGroup: selectedAge.value
    })
    const userStore = useUserStore()
    userStore.setToken(res.accessToken)
    if (res.userInfo) {
      userStore.setUserInfo(res.userInfo)
    }
    uni.hideLoading()
    uni.showToast({ title: '注册成功！', icon: 'success' })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/main/index' })
    }, 1500)
  } catch (e) {
    uni.hideLoading()
    errorMsg.value = e?.msg || '注册失败，请重试'
  }
}

function goLogin() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.register-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #FFF5F5, #FFF0F0);
  position: relative;
  overflow: hidden;
}

.bg-decor { position: absolute; top: 0; left: 0; right: 0; bottom: 0; pointer-events: none; }
.decor-circle {
  position: absolute; border-radius: 50%; opacity: 0.12;
  &.c1 { width: 300px; height: 300px; background: $primary; top: -80px; right: -60px; }
  &.c2 { width: 200px; height: 200px; background: $teal; bottom: -40px; left: 10%; }
  &.c3 { width: 150px; height: 150px; background: $primary-light; top: 40%; left: -40px; }
}

.register-card {
  display: flex;
  width: 900px;
  min-height: 540px;
  background: $white;
  border-radius: $radius-xl;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  z-index: 1;
}

.brand-side {
  width: 360px;
  background: linear-gradient(135deg, $primary, $primary-light);
  padding: 48px 36px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.brand-emoji { font-size: 56px; }
.features { display: flex; flex-direction: column; gap: 8px; margin-top: 16px; }

.form-side {
  flex: 1;
  padding: 36px 40px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.age-group-row {
  display: flex;
  gap: 10px;
}

.age-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px;
  border-radius: $radius;
  background: #F8F8F8;
  cursor: pointer;
  border: 2px solid transparent;

  &.active {
    border-color: $primary;
    background: #FFF0F0;
  }
}

.age-emoji { font-size: 24px; }

.sms-row {
  display: flex;
  gap: 8px;
}

.login-link {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 4px;
}
</style>
