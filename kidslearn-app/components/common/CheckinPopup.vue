<template>
  <view v-if="visible" class="checkin-mask" @tap.self="close">
    <view class="checkin-popup animate-pop-in">
      <text class="checkin-title text-title text-bold">每日签到</text>
      <view class="checkin-streak">
        <text class="streak-num">{{ streak }}</text>
        <text class="streak-label">天连续签到</text>
      </view>

      <view class="checkin-grid">
        <view
          v-for="d in weekDays"
          :key="d.day"
          class="checkin-day"
          :class="{ done: d.done, today: d.isToday, current: d.day === nextRewardDay && !checkedIn }"
        >
          <text class="day-label">Day{{ d.day }}</text>
          <text class="day-emoji">{{ d.done ? '完成' : d.day === 7 ? '加倍' : '待签' }}</text>
          <text class="day-reward">金币 +{{ d.gold }}</text>
        </view>
      </view>

      <tn-button
        v-if="!checkedIn"
        type="primary"
        size="xl"
        shape="round"
        class="checkin-btn"
        :loading="loading"
        @click="doCheckin"
        style="background: linear-gradient(135deg, #FFD700, #FFA500);"
      >
        签到领取 · 金币 {{ nextGold }} · 经验 {{ nextExp }}
      </tn-button>
      <view v-else class="checked-tip">
        <text class="text-md text-bold">今日已签到</text>
        <text class="text-sm text-light">明天记得继续哦！</text>
      </view>

      <view class="checkin-close" @tap="close">
        <text>关闭</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCheckinStatus, postCheckin } from '@/api/learn'

const emit = defineEmits(['close'])

const props = defineProps({
  autoCloseIfDone: { type: Boolean, default: false }
})

const visible = ref(false)
const loading = ref(false)
const checkedIn = ref(false)
const streak = ref(0)
const nextRewardDay = ref(1)
const nextGold = ref(5)
const nextExp = ref(5)
const weekDays = ref([])

onMounted(async () => {
  try {
    const res = await getCheckinStatus()
    if (res) {
      checkedIn.value = res.checkedIn || false
      streak.value = res.streak || 0
      nextRewardDay.value = res.nextRewardDay || 1
      nextGold.value = res.nextGoldReward || 5
      nextExp.value = res.nextExpReward || 5
      weekDays.value = res.weekDays || []
    }
    // If already checked in today and auto-close is enabled, don't show popup
    if (checkedIn.value && props.autoCloseIfDone) {
      emit('close')
      return
    }
    visible.value = true
  } catch (e) {
    console.log('签到状态获取失败:', e)
    emit('close')
  }
})

async function doCheckin() {
  if (loading.value) return
  loading.value = true
  try {
    const res = await postCheckin()
    if (res) {
      checkedIn.value = true
      weekDays.value = weekDays.value.map(d =>
        d.isToday ? { ...d, done: true } : d
      )
      uni.showToast({ title: `金币 +${res.goldReward} · 经验 +${res.expReward}`, icon: 'none' })
    }
  } catch (e) {
    uni.showToast({ title: e.message || '签到失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function close() {
  visible.value = false
  emit('close')
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.checkin-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.checkin-popup {
  width: 360px;
  max-width: 90vw;
  background: #fff;
  border-radius: 24px;
  padding: 28px 24px;
  text-align: center;
  position: relative;
}

.checkin-title {
  display: block;
  margin-bottom: 8px;
}

.checkin-streak {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
  margin-bottom: 16px;
}
.streak-num {
  font-size: 36px;
  font-weight: 800;
  color: #FFA500;
}
.streak-label {
  font-size: 14px;
  color: #999;
}

.checkin-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  margin-bottom: 20px;
}

.checkin-day {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 6px 2px;
  border-radius: 10px;
  background: #F5F5F5;
  transition: all 0.2s ease;

  &.done {
    background: #E8FFF0;
  }
  &.today {
    border: 2px solid #FFA500;
  }
  &.current {
    background: #FFF8E6;
    border: 2px solid #FFD700;
  }
}

.day-label {
  font-size: 10px;
  color: #999;
  font-weight: 700;
}
.day-emoji {
  font-size: 18px;
}
.day-reward {
  font-size: 9px;
  color: #666;
  font-weight: 600;
}

.checkin-btn {
  width: 100%;
  margin-bottom: 8px;
}

.checked-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 16px;
}

.checkin-close {
  position: absolute;
  top: 12px;
  right: 16px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #F0F0F0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  cursor: pointer;
}
</style>
