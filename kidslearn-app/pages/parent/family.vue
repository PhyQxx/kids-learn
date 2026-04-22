<template>
  <AppLayout theme="parent" title="家庭管理" :show-back="true" active-nav="/pages/parent/index">
    <view class="family-content">
      <!-- 家庭成员 -->
      <view class="section-card card">
        <view class="section-header">
          <text class="text-md text-bold">👨‍👩‍👧 家庭成员</text>
          <view class="btn btn-outline-teal btn-pill" @tap="addMember">
            <text class="text-sm">+ 添加成员</text>
          </view>
        </view>
        <view class="member-list">
          <view v-for="member in members" :key="member.id" class="member-row">
            <view class="member-avatar" :style="{ background: member.bg }">
              <text>{{ member.icon }}</text>
            </view>
            <view class="member-info">
              <text class="text-sm text-bold">{{ member.name }}</text>
              <text class="text-xs text-light">{{ member.role }}</text>
            </view>
            <text class="text-xs" :class="member.online ? 'text-success' : 'text-light'">
              {{ member.online ? '在线' : '离线' }}
            </text>
          </view>
        </view>
      </view>

      <!-- 设备管理 -->
      <view class="section-card card">
        <view class="section-header">
          <text class="text-md text-bold">📱 设备管理</text>
        </view>
        <view class="device-list">
          <view v-for="device in devices" :key="device.id" class="device-row">
            <text class="device-icon">{{ device.icon }}</text>
            <view class="device-info">
              <text class="text-sm text-bold">{{ device.name }}</text>
              <text class="text-xs text-light">{{ device.model }}</text>
            </view>
            <tn-badge :type="device.active ? 'success' : 'info'" :text="device.active ? '活跃' : '未连接'" />
          </view>
        </view>
      </view>

      <!-- 提醒设置 -->
      <view class="section-card card">
        <view class="section-header">
          <text class="text-md text-bold">⚠️ 提醒通知</text>
        </view>
        <view class="alert-list">
          <view v-for="alert in alerts" :key="alert.id" class="alert-row">
            <text class="alert-icon">{{ alert.icon }}</text>
            <view class="alert-info">
              <text class="text-sm">{{ alert.title }}</text>
              <text class="text-xs text-light">{{ alert.desc }}</text>
            </view>
            <tn-switch v-model="alert.enabled" active-color="#4ECDC4" />
          </view>
        </view>
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { getFamilyMembers } from '@/api/parent'

const members = ref([
  { id: 1, name: '爸爸', icon: '👨', role: '管理员', bg: '#E8F0FE', online: true },
  { id: 2, name: '妈妈', icon: '👩', role: '管理员', bg: '#FFE8E8', online: true },
  { id: 3, name: '小明', icon: '👦', role: '孩子 · 低龄组', bg: '#E0F7F7', online: true },
  { id: 4, name: '小红', icon: '👧', role: '孩子 · 幼幼组', bg: '#FFF8E0', online: false }
])

onMounted(async () => {
  try {
    const res = await getFamilyMembers()
    if (res && res.members && Array.isArray(res.members)) {
      members.value = res.members.map((m, i) => ({
        id: m.id,
        name: m.nickname || m.name,
        icon: m.avatar || ['👨', '👩', '👦', '👧'][i] || '👤',
        role: m.role === 'PARENT' ? '管理员' : '孩子',
        bg: ['#E8F0FE', '#FFE8E8', '#E0F7F7', '#FFF8E0'][i] || '#F5F5F5',
        online: true
      }))
    }
  } catch (e) {
    console.log('family: 使用模拟数据')
  }
})

const devices = ref([
  { id: 1, name: 'iPad', icon: '📱', model: 'iPad Pro 12.9"', active: true },
  { id: 2, name: 'iPhone', icon: '📱', model: 'iPhone 15', active: false }
])

const alerts = ref([
  { id: 1, icon: '⏰', title: '时间即将用尽', desc: '剩余5分钟时推送通知', enabled: true },
  { id: 2, icon: '👀', title: '护眼提醒', desc: '每20分钟提醒远眺休息', enabled: true },
  { id: 3, icon: '🔓', title: '新学科解锁请求', desc: '孩子请求解锁新学科时通知', enabled: false },
  { id: 4, icon: '🛒', title: '消费提醒', desc: '孩子在商店消费时通知', enabled: true }
])

function addMember() {
  uni.showToast({ title: '请输入手机号邀请成员', icon: 'none' })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.family-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-card { padding: 16px 20px; }

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.member-list, .device-list, .alert-list {
  display: flex;
  flex-direction: column;
}

.member-row, .device-row, .alert-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  & + .member-row, & + .device-row, & + .alert-row { border-top: 1px solid #F5F5F5; }
}

.member-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.member-info, .device-info, .alert-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.device-icon, .alert-icon { font-size: 20px; width: 28px; text-align: center; }
</style>
