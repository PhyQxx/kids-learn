<template>
  <AppLayout theme="parent" title="时间管理" :show-back="true" active-nav="/pages/parent/index">
    <view class="time-control-content">
      <!-- 每日时长限制 -->
      <view class="control-section card">
        <text class="text-md text-bold" style="margin-bottom: 12px;">⏰ 每日学习时长</text>
        <view class="limit-options">
          <view
            v-for="opt in limitOptions"
            :key="opt.value"
            class="limit-option"
            :class="{ active: dailyLimit === opt.value }"
            @tap="dailyLimit = opt.value"
          >
            <text class="text-md text-bold">{{ opt.label }}</text>
            <text class="text-xs text-light">{{ opt.desc }}</text>
          </view>
        </view>
      </view>

      <!-- 禁止使用时段 -->
      <view class="control-section card">
        <text class="text-md text-bold" style="margin-bottom: 12px;">🌙 禁止使用时段</text>
        <view class="time-range-row">
          <view class="time-picker">
            <text class="text-sm text-light">开始</text>
            <view class="time-value" @tap="editStartTime = true">
              <text class="text-lg text-bold">{{ forbiddenStart }}</text>
            </view>
          </view>
          <text class="text-lg">—</text>
          <view class="time-picker">
            <text class="text-sm text-light">结束</text>
            <view class="time-value" @tap="editEndTime = true">
              <text class="text-lg text-bold">{{ forbiddenEnd }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 提醒设置 -->
      <view class="control-section card">
        <text class="text-md text-bold" style="margin-bottom: 12px;">🔔 提醒设置</text>
        <view class="toggle-row">
          <view class="toggle-info">
            <text class="text-sm">休息提醒</text>
            <text class="text-xs text-light">每30分钟提醒休息</text>
          </view>
          <tn-switch v-model="breakReminder" active-color="#4ECDC4" />
        </view>
        <view class="toggle-row">
          <view class="toggle-info">
            <text class="text-sm">护眼提醒</text>
            <text class="text-xs text-light">每20分钟提醒远眺</text>
          </view>
          <tn-switch v-model="eyeCareReminder" active-color="#4ECDC4" />
        </view>
        <view class="toggle-row">
          <view class="toggle-info">
            <text class="text-sm">时间耗尽提醒</text>
            <text class="text-xs text-light">剩余5分钟时提醒</text>
          </view>
          <tn-switch v-model="timeUpReminder" active-color="#4ECDC4" />
        </view>
      </view>

      <!-- 保存 -->
      <tn-button type="primary" shape="round" size="lg" block @click="saveSettings">保存设置</tn-button>
    </view>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { getTimeControl, updateTimeControl } from '@/api/parent'

const dailyLimit = ref(60)
const forbiddenStart = ref('21:00')
const forbiddenEnd = ref('07:00')
const breakReminder = ref(true)
const eyeCareReminder = ref(true)
const timeUpReminder = ref(true)
const editStartTime = ref(false)
const editEndTime = ref(false)

const limitOptions = ref([
  { value: 30, label: '30分钟', desc: '适合幼幼组' },
  { value: 60, label: '60分钟', desc: '推荐' },
  { value: 90, label: '90分钟', desc: '周末放宽' },
  { value: 120, label: '120分钟', desc: '假期模式' }
])

onMounted(async () => {
  try {
    const res = await getTimeControl()
    if (res) {
      dailyLimit.value = res.dailyLimitMinutes || 60
      // 后端存的是允许使用时段 (allowedStartTime ~ allowedEndTime)，前端显示为禁止时段
      forbiddenStart.value = res.allowedEndTime || '21:00'
      forbiddenEnd.value = res.allowedStartTime || '07:00'
      breakReminder.value = res.restReminder !== false
    }
  } catch (e) {
    console.log('time-control: 使用默认值')
  }
})

async function saveSettings() {
  try {
    await updateTimeControl({
      dailyLimitMinutes: dailyLimit.value,
      allowedStartTime: forbiddenEnd.value,
      allowedEndTime: forbiddenStart.value,
      restReminder: breakReminder.value
    })
    uni.showToast({ title: '设置已保存', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 1000)
  } catch (e) {
    uni.showToast({ title: '保存失败', icon: 'none' })
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.time-control-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.control-section { padding: 16px 20px; }

.limit-options {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.limit-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 14px;
  border-radius: $radius;
  background: #F8F8F8;
  cursor: pointer;
  border: 2px solid transparent;

  &.active {
    border-color: $teal;
    background: #E0F7F7;
  }
}

.time-range-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.time-picker {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.time-value {
  padding: 8px 24px;
  background: #F5F5F5;
  border-radius: $radius;
  cursor: pointer;
}

.toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  & + .toggle-row { border-top: 1px solid #F5F5F5; }
}

.toggle-info {
  display: flex;
  flex-direction: column;
}
</style>
