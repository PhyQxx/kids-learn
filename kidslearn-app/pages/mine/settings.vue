<template>
  <AppLayout theme="kids" title="设置" :show-back="true" active-nav="/pages/mine/index">
    <view class="settings-content">
      <!-- 账号设置 -->
      <view class="settings-group card">
        <text class="text-md text-bold" style="margin-bottom: 12px;">👤 账号设置</text>
        <view class="setting-item" @tap="editPhone">
          <text class="text-sm">手机号</text>
          <view class="setting-right">
            <text class="text-sm text-light">{{ maskedPhone }}</text>
            <text class="menu-arrow">→</text>
          </view>
        </view>
        <view class="setting-item" @tap="editPassword">
          <text class="text-sm">修改密码</text>
          <view class="setting-right">
            <text class="menu-arrow">→</text>
          </view>
        </view>
        <view class="setting-item" @tap="editProfile">
          <text class="text-sm">孩子资料</text>
          <view class="setting-right">
            <text class="text-sm text-light">修改</text>
            <text class="menu-arrow">→</text>
          </view>
        </view>
      </view>

      <!-- 学习设置 -->
      <view class="settings-group card">
        <text class="text-md text-bold" style="margin-bottom: 12px;">📚 学习设置</text>
        <view class="setting-item" @tap="showGradePopup = true">
          <text class="text-sm">年级</text>
          <view class="setting-right">
            <text class="text-sm" :class="currentGrade ? '' : 'text-light'">{{ currentGradeLabel }}</text>
            <text class="menu-arrow">→</text>
          </view>
        </view>
      </view>

      <!-- 通知设置 -->
      <view class="settings-group card">
        <text class="text-md text-bold" style="margin-bottom: 12px;">🔔 通知设置</text>
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
        <text class="text-md text-bold" style="margin-bottom: 12px;">🎨 显示设置</text>
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
          <text class="text-xs text-light">v1.0.0</text>
        </view>
        <view class="setting-item" @tap="showAgreement">
          <text class="text-sm">用户协议</text>
          <text class="menu-arrow">→</text>
        </view>
        <view class="setting-item" @tap="showPrivacy">
          <text class="text-sm">隐私政策</text>
          <text class="menu-arrow">→</text>
        </view>
      </view>

      <!-- 清除缓存 -->
      <tn-button shape="round" block @click="clearCache">清除缓存</tn-button>
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
import { ref, computed } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import GradeSelectPopup from '@/components/GradeSelectPopup.vue'
import { useUserStore } from '@/store/user'
import { getUserInfo, updateChildProfile } from '@/api/user'

const userStore = useUserStore()

const showGradePopup = ref(false)

const settings = ref({
  studyReminder: true,
  achievementNotify: true,
  rankNotify: false,
  theme: 'coral',
  soundEnabled: true
})

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
    uni.showToast({ title: '已更新年级', icon: 'success' })
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

function editPhone() { uni.showToast({ title: '修改手机号', icon: 'none' }) }
function editPassword() { uni.showToast({ title: '修改密码', icon: 'none' }) }
function editProfile() { uni.showToast({ title: '修改孩子资料', icon: 'none' }) }
function checkUpdate() { uni.showToast({ title: '已是最新版本', icon: 'none' }) }
function showAgreement() { uni.showToast({ title: '用户协议', icon: 'none' }) }
function showPrivacy() { uni.showToast({ title: '隐私政策', icon: 'none' }) }
function clearCache() {
  uni.showModal({
    title: '确认清除',
    content: '清除缓存不会影响登录状态',
    success: (res) => { if (res.confirm) uni.showToast({ title: '已清除', icon: 'success' }) }
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
