<template>
  <AppLayout theme="parent" title="家长中心" active-nav="/pages/parent/index">
    <view class="parent-home">
      <view class="overview-band">
        <view>
          <text class="overview-title">今日家庭学习</text>
          <text class="overview-subtitle">{{ summary.onlineCount }} 个在线，{{ summary.learningCount }} 个学习中</text>
        </view>
        <view class="monitor-link" @tap="go('/pages/parent/monitor')">
          <text>实时监控</text>
        </view>
      </view>

      <view class="menu-grid">
        <view v-for="item in menus" :key="item.path" class="menu-card" @tap="go(item.path)">
          <view class="menu-icon">
            <text>{{ item.icon }}</text>
          </view>
          <view class="menu-info">
            <text class="menu-label">{{ item.label }}</text>
            <text class="menu-desc">{{ item.desc }}</text>
          </view>
          <text class="menu-arrow">›</text>
        </view>
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { getRealtimeMonitor } from '@/api/parent'
import { useRealtimeStore } from '@/store/realtime'

const realtimeStore = useRealtimeStore()

const summary = computed(() => realtimeStore.parentMonitor.summary || {
  childCount: 0,
  onlineCount: 0,
  learningCount: 0,
  todayMinutes: 0,
  completedLevels: 0,
  alertCount: 0
})

const menus = [
  { icon: '📡', label: '实时监控', desc: '在线状态、学习进度、最近活动', path: '/pages/parent/monitor' },
  { icon: '📊', label: '学习报告', desc: '月度趋势、正确率、薄弱点', path: '/pages/parent/report' },
  { icon: '⏱', label: '时间管理', desc: '每日时长、可用时间、休息提醒', path: '/pages/parent/time-control' },
  { icon: '👨‍👩‍👧‍👦', label: '家庭管理', desc: '成员、设备、通知设置', path: '/pages/parent/family' },
  { icon: '👑', label: 'VIP 会员', desc: '订阅权益与订单管理', path: '/pages/mine/vip' }
]

onMounted(async () => {
  realtimeStore.connect()
  try {
    const data = await getRealtimeMonitor()
    realtimeStore.setParentMonitor(data)
  } catch (e) {
    // Parent home can still render static entries when monitor data is unavailable.
  }
})

function go(url) {
  uni.navigateTo({ url })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.parent-home {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.overview-band {
  min-height: 112px;
  padding: 22px 24px;
  border-radius: $radius-lg;
  background: linear-gradient(135deg, $teal, $teal-dark);
  color: $white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  box-shadow: $shadow-sm;
}

.overview-title {
  display: block;
  font-size: 24px;
  font-weight: 800;
  line-height: 1.2;
}

.overview-subtitle {
  display: block;
  margin-top: 8px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.82);
}

.monitor-link {
  min-height: 40px;
  padding: 0 18px;
  border-radius: 20px;
  background: $white;
  color: $teal-dark;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 800;
  flex-shrink: 0;
}

.menu-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.menu-card {
  min-height: 108px;
  padding: 18px;
  border-radius: $radius-md;
  background: $white;
  box-shadow: $shadow-sm;
  display: flex;
  align-items: center;
  gap: 14px;
}

.menu-icon {
  width: 46px;
  height: 46px;
  border-radius: 16px;
  background: #E8F8F8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.menu-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.menu-label {
  font-size: 17px;
  font-weight: 800;
  color: $text;
}

.menu-desc {
  font-size: 13px;
  color: $text-light;
  line-height: 1.35;
}

.menu-arrow {
  color: $text-light;
  font-size: 24px;
  line-height: 1;
}

@media (max-width: 800px) {
  .overview-band {
    align-items: flex-start;
    flex-direction: column;
  }

  .menu-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
