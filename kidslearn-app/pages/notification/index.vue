<template>
  <AppLayout theme="kids" title="消息通知" :show-back="true" active-nav="/pages/mine/index">
    <view class="notification-content">
      <!-- 顶部操作栏 -->
      <view class="action-bar" v-if="notifications.length > 0">
        <text class="text-sm text-light">共 {{ notifications.length }} 条消息</text>
        <text class="text-sm text-primary" @tap="markAllRead">全部已读</text>
      </view>

      <!-- 通知列表 -->
      <view v-if="notifications.length > 0" class="notification-list">
        <view
          v-for="item in notifications"
          :key="item.id"
          class="notification-item card"
          :class="{ unread: !item.isRead }"
          @tap="readNotification(item)"
        >
          <view class="notification-icon">
            <text>{{ getTypeIcon(item.type) }}</text>
          </view>
          <view class="notification-body">
            <view class="notification-header">
              <text class="text-md text-bold">{{ item.title }}</text>
              <view v-if="!item.isRead" class="unread-dot"></view>
            </view>
            <text class="text-sm text-light notification-content-text">{{ item.content }}</text>
            <text class="text-xs text-light">{{ formatTime(item.createTime) }}</text>
          </view>
        </view>
      </view>

      <!-- 空状态 -->
      <view v-else-if="!loading" class="empty-state">
        <text class="empty-emoji">🔔</text>
        <text class="text-md text-light">暂无消息通知</text>
        <text class="text-xs text-light">学习提醒、成就达成等消息将在这里显示</text>
      </view>

      <!-- 加载状态 -->
      <FunLoadingState v-if="loading" title="加载中" mascot="📨" />
    </view>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import FunLoadingState from '@/components/common/FunLoadingState.vue'
import { getNotificationList, markAsRead, markAllAsRead as apiMarkAllRead } from '@/api/notification'

const loading = ref(false)
const notifications = ref([])

function getTypeIcon(type) {
  const icons = {
    system: '📢',
    achievement: '🏆',
    friend: '👥',
    challenge: '⚔️',
    learning: '📚',
  }
  return icons[type] || '📣'
}

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'

  return `${date.getMonth() + 1}/${date.getDate()}`
}

async function loadNotifications() {
  loading.value = true
  try {
    const res = await getNotificationList()
    if (res) {
      notifications.value = res
    }
  } catch (e) {
    console.error('加载通知失败', e)
  } finally {
    loading.value = false
  }
}

async function readNotification(item) {
  if (!item.isRead) {
    try {
      await markAsRead(item.id)
      item.isRead = true
    } catch (e) {
      console.error('标记已读失败', e)
    }
  }
}

async function markAllRead() {
  try {
    await apiMarkAllRead()
    notifications.value.forEach(n => { n.isRead = true })
    uni.showToast({ title: '已全部标记为已读', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: '操作失败', icon: 'none' })
  }
}

onMounted(() => loadNotifications())
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.notification-content {
  padding: 16px;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notification-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 16px;
  transition: all 0.2s;

  &.unread {
    background: #FFF8F0;
    border-left: 3px solid $primary;
  }

  &:active {
    transform: scale(0.98);
  }
}

.notification-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  background: #F5F7FA;
  border-radius: 12px;
  flex-shrink: 0;
}

.notification-body {
  flex: 1;
  min-width: 0;
}

.notification-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  background: $primary;
  border-radius: 50%;
  flex-shrink: 0;
}

.notification-content-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 4px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 12px;
}

.empty-emoji {
  font-size: 64px;
  margin-bottom: 8px;
}
</style>
