<template>
  <AppLayout theme="kids" title="设置" :show-back="true" active-nav="/pages/mine/index">
    <view class="settings-content">
      <!-- 账号设置 -->
      <view class="settings-group card">
        <view class="settings-group-title">
          <text class="settings-group-mark">账号</text>
          <text class="text-md text-bold">账号设置</text>
        </view>
        <view class="setting-item" @tap="editPhone">
          <text class="text-sm">手机号</text>
          <view class="setting-right">
            <text class="text-sm text-light">{{ maskedPhone }}</text>
            <text class="menu-arrow">进入</text>
          </view>
        </view>
        <view class="setting-item" @tap="editPassword">
          <text class="text-sm">修改密码</text>
          <view class="setting-right">
            <text class="menu-arrow">进入</text>
          </view>
        </view>
        <view class="setting-item" @tap="editProfile">
          <text class="text-sm">孩子资料</text>
          <view class="setting-right">
            <text class="text-sm text-light">修改</text>
            <text class="menu-arrow">进入</text>
          </view>
        </view>
      </view>

      <!-- 学习设置 -->
      <view class="settings-group card">
        <view class="settings-group-title">
          <text class="settings-group-mark">学习</text>
          <text class="text-md text-bold">学习设置</text>
        </view>
        <view class="setting-item" @tap="showGradePopup = true">
          <text class="text-sm">年级</text>
          <view class="setting-right">
            <text class="text-sm" :class="currentGrade ? '' : 'text-light'">{{ currentGradeLabel }}</text>
            <text class="menu-arrow">进入</text>
          </view>
        </view>
      </view>

      <!-- 通知设置 -->
      <view class="settings-group card">
        <view class="settings-group-title">
          <text class="settings-group-mark">提醒</text>
          <text class="text-md text-bold">通知设置</text>
        </view>
        <view class="setting-item">
          <text class="text-sm">学习提醒</text>
          <tn-switch v-model="settings.studyReminder" active-color="#FF6B6B" />
        </view>
        <view class="setting-item">
          <text class="text-sm">成就通知</text>
          <tn-switch v-model="settings.achievementNotify" active-color="#FF6B6B" />
        </view>
        <view class="setting-item">
          <text class="text-sm">排行榜变动</text>
          <tn-switch v-model="settings.rankNotify" active-color="#FF6B6B" />
        </view>
      </view>

      <!-- 显示设置 -->
      <view class="settings-group card">
        <view class="settings-group-title">
          <text class="settings-group-mark">界面</text>
          <text class="text-md text-bold">显示设置</text>
        </view>
        <view class="setting-item">
          <text class="text-sm">主题</text>
          <view class="theme-options">
            <view
              v-for="t in themes"
              :key="t.key"
              class="theme-dot"
              :class="{ active: settings.theme === t.key }"
              :style="{ background: t.color }"
              @tap="settings.theme = t.key"
            ></view>
          </view>
        </view>
        <view class="setting-item">
          <text class="text-sm">音效</text>
          <tn-switch v-model="settings.soundEnabled" active-color="#FF6B6B" />
        </view>
      </view>

      <!-- 关于 -->
      <view class="settings-group card">
        <view class="setting-item" @tap="checkUpdate">
          <text class="text-sm">检查更新</text>
          <text class="text-xs text-light">v{{ appVersion }}</text>
        </view>
        <view class="setting-item" @tap="showAgreement">
          <text class="text-sm">用户协议</text>
          <text class="menu-arrow">进入</text>
        </view>
        <view class="setting-item" @tap="showPrivacy">
          <text class="text-sm">隐私政策</text>
          <text class="menu-arrow">进入</text>
        </view>
      </view>

      <!-- 清除缓存 -->
      <tn-button shape="round" block @click="clearCache">清除缓存</tn-button>

      <!-- 退出登录 -->
      <view style="margin-top: 12px;">
        <tn-button shape="round" block type="danger" @click="handleLogout">退出登录</tn-button>
      </view>
    </view>
  </AppLayout>

  <!-- 年级选择弹框 -->
  <GradeSelectPopup
    :visible.sync="showGradePopup"
    :current-grade="currentGrade"
    @confirm="handleGradeConfirm"
    @close="showGradePopup = false"
  />
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import GradeSelectPopup from '@/components/GradeSelectPopup.vue'
import { useUserStore } from '@/store/user'
import { useLearnStore } from '@/store/learn'
import { getUserInfo, updateChildProfile } from '@/api/user'
import doCheckUpdate from '@/utils/update.mjs'
import manifest from '@/manifest.json'

const SETTINGS_KEY = 'kidslearn_settings'

// 从本地存储加载设置
function loadSettings() {
  try {
    const saved = uni.getStorageSync(SETTINGS_KEY)
    if (saved) return JSON.parse(saved)
  } catch {}
  return null
}

const userStore = useUserStore()
const learnStore = useLearnStore()

const showGradePopup = ref(false)

const defaultSettings = {
  studyReminder: true,
  achievementNotify: true,
  rankNotify: false,
  theme: 'coral',
  soundEnabled: true
}

const settings = ref(loadSettings() || { ...defaultSettings })

// 监听设置变化，自动保存到本地存储
watch(settings, (newVal) => {
  try {
    uni.setStorageSync(SETTINGS_KEY, JSON.stringify(newVal))
  } catch {}
}, { deep: true })

const themes = ref([
  { key: 'coral', color: '#FF6B6B' },
  { key: 'blue', color: '#4A90D9' },
  { key: 'teal', color: '#4ECDC4' },
  { key: 'purple', color: '#9B59B6' }
])

const currentGrade = ref(null)

const currentGradeLabel = computed(() => {
  if (!currentGrade.value) return '请选择'
  const labels = { 1: '小班', 2: '中班', 3: '大班', 4: '一年级', 5: '二年级', 6: '三年级', 7: '四年级', 8: '五年级', 9: '六年级' }
  return labels[currentGrade.value] || '请选择'
})

async function handleGradeConfirm(grade) {
  currentGrade.value = grade
  try {
    await updateChildProfile({ gradeLevel: grade })
    const info = await getUserInfo()
    if (info) userStore.setUserInfo(info)
    learnStore.clearLearningContext()
    uni.showToast({ title: '已更新年级', icon: 'success' })
    uni.reLaunch({ url: '/pages/main/index?tab=learn' })
  } catch (e) {
    uni.showToast({ title: '更新失败', icon: 'none' })
  }
  showGradePopup.value = false
}

const maskedPhone = computed(() => {
  const phone = userStore.userInfo?.phone || '13812345678'
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
})

// 刷新用户信息
getUserInfo().then(info => {
  if (info) {
    userStore.setUserInfo(info)
    // init grade from gradeLevelId returned by backend
    currentGrade.value = info.gradeLevelId || null
  }
}).catch(() => {})

function editPhone() { uni.showToast({ title: '修改手机号功能开发中', icon: 'none' }) }

function editPassword() {
  uni.showModal({
    title: '修改密码',
    editable: true,
    placeholderText: '请输入新密码（6-20位）',
    success: async (res) => {
      if (res.confirm && res.content) {
        const newPwd = res.content.trim()
        if (newPwd.length < 6 || newPwd.length > 20) {
          uni.showToast({ title: '密码长度需6-20位', icon: 'none' })
          return
        }
        try {
          const { updatePassword } = await import('@/api/user')
          await updatePassword({ password: newPwd })
          uni.showToast({ title: '密码修改成功', icon: 'success' })
        } catch (e) {
          uni.showToast({ title: e.message || '修改失败', icon: 'none' })
        }
      }
    }
  })
}

function editProfile() {
  uni.navigateTo({ url: '/pages/onboarding/index?mode=edit' })
}
const appVersion = manifest.versionName || '1.0.0'

function checkUpdate() {
  // #ifdef APP-PLUS
  doCheckUpdate({ showToast: true })
  // #endif
  // #ifndef APP-PLUS
  uni.showModal({
    title: '更新提示',
    content: '当前平台不支持应用内更新',
    showCancel: false
  })
  // #endif
}
function showAgreement() {
  uni.showModal({
    title: '用户协议',
    content: '欢迎使用趣学星球！\n\n本协议是您与趣学星球之间关于使用本应用服务的约定。请您仔细阅读以下条款：\n\n1. 服务内容：本应用为3-12岁儿童提供游戏化学习服务。\n2. 账号安全：请妥善保管您的账号信息。\n3. 知识产权：本应用内容受著作权法保护。\n4. 免责声明：本应用不对因不可抗力导致的服务中断承担责任。\n\n如有疑问，请联系客服。',
    showCancel: false,
    confirmText: '我知道了'
  })
}

function showPrivacy() {
  uni.showModal({
    title: '隐私政策',
    content: '趣学星球非常重视您的隐私保护。\n\n1. 信息收集：我们收集您的账号信息、学习记录等必要数据。\n2. 信息使用：收集的信息用于提供个性化学习服务。\n3. 信息保护：我们采用加密技术保护您的数据安全。\n4. 信息共享：未经您同意，我们不会向第三方分享您的个人信息。\n5. 未成年人保护：我们严格遵守未成年人个人信息保护相关法规。\n\n更新日期：2026年1月1日',
    showCancel: false,
    confirmText: '我知道了'
  })
}
function clearCache() {
  uni.showModal({
    title: '确认清除',
    content: '清除缓存不会影响登录状态，但会清除离线数据',
    success: (res) => {
      if (res.confirm) {
        try {
          // 保留token和用户信息，清除其他缓存
          const token = uni.getStorageSync('token')
          const refreshToken = uni.getStorageSync('refresh_token')
          const userInfo = uni.getStorageSync('userInfo')

          // 清除所有本地存储
          uni.clearStorageSync()

          // 恢复重要数据
          if (token) uni.setStorageSync('token', token)
          if (refreshToken) uni.setStorageSync('refresh_token', refreshToken)
          if (userInfo) uni.setStorageSync('userInfo', userInfo)

          uni.showToast({ title: '缓存已清除', icon: 'success' })
        } catch (e) {
          uni.showToast({ title: '清除失败', icon: 'none' })
        }
      }
    }
  })
}

function handleLogout() {
  uni.showModal({
    title: '退出确认',
    content: '确定要退出登录吗？',
    cancelText: '点错了',
    confirmText: '确定退出',
    confirmColor: '#FF6B6B',
    success: (res) => {
      if (res.confirm) {
        userStore.logout()
      }
    }
  })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.settings-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.settings-group { padding: 8px 20px; }

.settings-group-title {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 0 8px;
}

.settings-group-mark {
  min-width: 42px;
  padding: 5px 8px;
  border-radius: 999px;
  background: #EEF3FF;
  color: #315EBA;
  font-size: 11px;
  font-weight: 850;
  line-height: 1.2;
  text-align: center;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  cursor: pointer;

  & + .setting-item { border-top: 1px solid #F5F5F5; }
}

.setting-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.menu-arrow { color: $text-light; font-size: 14px; }

.grade-picker {
  display: flex;
  align-items: center;
  gap: 6px;
}

.theme-options {
  display: flex;
  gap: 10px;
}

.theme-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid transparent;

  &.active { border-color: $text; box-shadow: 0 0 0 2px rgba(0,0,0,0.1); }
}
</style>
