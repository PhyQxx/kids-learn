<template>
  <AppLayout theme="kids" title="好友" :show-back="true" active-nav="/pages/mine/index">
    <view class="friend-content">
      <!-- 我的邀请码卡 -->
      <view class="invite-card card">
        <view class="invite-info">
          <text class="text-sm text-light">我的邀请码</text>
          <text class="invite-code">{{ inviteCode || '加载中...' }}</text>
        </view>
        <view class="invite-actions">
          <button class="invite-btn" @tap="copyInviteCode" :disabled="!inviteCode">复制</button>
          <button class="invite-btn" @tap="shareInviteCode" :disabled="!inviteCode">分享</button>
        </view>
      </view>

      <!-- Tab 切换 -->
      <tn-tabs v-model="activeTab" active-color="#FF7A59">
        <tn-tabs-item title="好友" />
        <tn-tabs-item :title="`新请求${requestList.length ? '(' + requestList.length + ')' : ''}`" />
        <tn-tabs-item title="添加好友" />
      </tn-tabs>

      <!-- 好友列表 -->
      <view v-if="activeTab === 0" class="list-wrap">
        <view v-if="loading" class="empty-tip"><text class="text-sm text-light">加载中...</text></view>
        <view v-else-if="friendList.length === 0" class="empty-tip">
          <text class="text-sm text-light">还没有好友，去"添加好友"页用邀请码加好友吧</text>
        </view>
        <view v-else class="friend-list">
          <view v-for="f in friendList" :key="f.friendId" class="friend-card card">
            <image class="friend-avatar" :src="f.avatar || '/static/default-avatar.png'" mode="aspectFill" />
            <view class="friend-info">
              <text class="text-sm text-bold">{{ f.nickname || '未命名' }}</text>
              <text class="text-xs text-light">{{ f.level != null ? 'Lv.' + f.level : '' }}</text>
            </view>
            <view class="friend-ops">
              <button class="op-btn op-btn-primary" @tap.stop="challengeFriend(f)">挑战</button>
              <button class="op-btn op-btn-danger" @tap.stop="confirmRemove(f)">删除</button>
            </view>
          </view>
        </view>
      </view>

      <!-- 好友请求 -->
      <view v-else-if="activeTab === 1" class="list-wrap">
        <view v-if="loading" class="empty-tip"><text class="text-sm text-light">加载中...</text></view>
        <view v-else-if="requestList.length === 0" class="empty-tip">
          <text class="text-sm text-light">暂无新的好友请求</text>
        </view>
        <view v-else class="friend-list">
          <view v-for="r in requestList" :key="r.requestId" class="friend-card card">
            <image class="friend-avatar" :src="r.avatar || '/static/default-avatar.png'" mode="aspectFill" />
            <view class="friend-info">
              <text class="text-sm text-bold">{{ r.nickname || '未命名' }}</text>
              <text class="text-xs text-light">请求加你为好友</text>
            </view>
            <view class="friend-ops">
              <button class="op-btn op-btn-primary" @tap.stop="handleRequest(r, true)">接受</button>
              <button class="op-btn op-btn-ghost" @tap.stop="handleRequest(r, false)">拒绝</button>
            </view>
          </view>
        </view>
      </view>

      <!-- 添加好友 -->
      <view v-else class="list-wrap">
        <view class="search-box card">
          <input
            class="search-input"
            v-model="searchKeyword"
            placeholder="输入邀请码、用户名或昵称"
            confirm-type="search"
            :disabled="searching"
            @confirm="doSearch"
          />
          <button class="search-btn" :disabled="searching" @tap="doSearch">搜索</button>
        </view>

        <view v-if="searching" class="empty-tip"><text class="text-sm text-light">搜索中...</text></view>
        <view v-else-if="searchResult.length === 0 && hasSearched" class="empty-tip">
          <text class="text-sm text-light">没有找到该用户，检查下邀请码是否正确</text>
        </view>
        <view v-else-if="searchResult.length > 0" class="friend-list">
          <view v-for="u in searchResult" :key="u.userId" class="friend-card card">
            <image class="friend-avatar" :src="u.avatar || '/static/default-avatar.png'" mode="aspectFill" />
            <view class="friend-info">
              <text class="text-sm text-bold">{{ u.nickname || '未命名' }}</text>
              <text class="text-xs text-light">{{ u.level != null ? 'Lv.' + u.level : '' }}{{ u.inviteCode ? ' · ' + u.inviteCode : '' }}</text>
            </view>
            <view class="friend-ops">
              <button v-if="u.isFriend" class="op-btn op-btn-ghost" disabled>已添加</button>
              <button v-else class="op-btn op-btn-primary" @tap.stop="addFriend(u)">加好友</button>
            </view>
          </view>
        </view>
        <view v-else class="search-hint">
          <text class="text-sm text-light">把你的邀请码告诉小伙伴，他们搜索即可加你为好友</text>
        </view>
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import AppLayout from '@/components/AppLayout.vue'
import {
  getFriendList,
  getFriendRequests,
  sendFriendRequest,
  handleFriendRequest,
  removeFriend,
  searchUsers,
  getMyInviteCode
} from '@/api/friend'

const activeTab = ref(0)
const loading = ref(false)
const friendList = ref([])
const requestList = ref([])
const inviteCode = ref('')

const searchKeyword = ref('')
const searchResult = ref([])
const searching = ref(false)
const hasSearched = ref(false)

async function loadData() {
  loading.value = true
  try {
    const [friends, requests] = await Promise.allSettled([getFriendList(), getFriendRequests()])
    friendList.value = friends.status === 'fulfilled' ? (friends.value || []) : []
    requestList.value = requests.status === 'fulfilled' ? (requests.value || []) : []
  } catch (e) {
    console.log('加载好友数据失败', e)
  } finally {
    loading.value = false
  }
}

async function loadInviteCode() {
  try {
    inviteCode.value = await getMyInviteCode()
  } catch (e) {
    console.log('获取邀请码失败', e)
  }
}

function copyInviteCode() {
  if (!inviteCode.value) return
  uni.setClipboardData({
    data: inviteCode.value,
    success: () => uni.showToast({ title: '邀请码已复制', icon: 'success' })
  })
}

function shareInviteCode() {
  if (!inviteCode.value) return
  uni.setClipboardData({
    data: `来趣学星球和我一起学习吧！我的邀请码：${inviteCode.value}`,
    success: () => uni.showToast({ title: '邀请语已复制，去粘贴给小伙伴', icon: 'none' })
  })
}

async function doSearch() {
  const kw = searchKeyword.value.trim()
  if (!kw) {
    uni.showToast({ title: '请输入邀请码或用户名', icon: 'none' })
    return
  }
  searching.value = true
  hasSearched.value = true
  try {
    searchResult.value = await searchUsers(kw) || []
  } catch (e) {
    searchResult.value = []
    console.log('搜索失败', e)
  } finally {
    searching.value = false
  }
}

async function addFriend(u) {
  try {
    await sendFriendRequest(u.userId)
    uni.showToast({ title: '请求已发送', icon: 'success' })
    u.isFriend = true
  } catch (e) {
    uni.showToast({ title: e.message || '发送失败', icon: 'none' })
  }
}

async function handleRequest(r, accept) {
  const action = accept ? '接受' : '拒绝'
  uni.showModal({
    title: '确认',
    content: `${action} ${r.nickname || '该用户'} 的好友请求？`,
    success: async ({ confirm }) => {
      if (!confirm) return
      try {
        await handleFriendRequest(r.requestId, accept)
        uni.showToast({ title: '已' + action, icon: 'success' })
        requestList.value = requestList.value.filter(x => x.requestId !== r.requestId)
        if (accept) {
          // 接受后刷新好友列表
          loadData()
        }
      } catch (e) {
        uni.showToast({ title: e.message || '操作失败', icon: 'none' })
      }
    }
  })
}

function confirmRemove(f) {
  uni.showModal({
    title: '删除好友',
    content: `确定删除好友 ${f.nickname || ''} 吗？`,
    success: async ({ confirm }) => {
      if (!confirm) return
      try {
        await removeFriend(f.friendId)
        uni.showToast({ title: '已删除', icon: 'success' })
        friendList.value = friendList.value.filter(x => x.friendId !== f.friendId)
      } catch (e) {
        uni.showToast({ title: e.message || '删除失败', icon: 'none' })
      }
    }
  })
}

function challengeFriend(f) {
  uni.navigateTo({
    url: `/pages/challenge/index?opponentId=${f.friendId}`
  })
}

// 读取路由参数：通知中心点击好友请求/结果跳转时带 tab=1（新请求）或 tab=0（好友列表）
onLoad((options) => {
  if (options && options.tab != null && options.tab !== '') {
    const tab = Number(options.tab)
    if (!Number.isNaN(tab) && tab >= 0 && tab <= 2) {
      activeTab.value = tab
    }
  }
})

onMounted(() => {
  loadInviteCode()
})

onShow(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.friend-content {
  padding: 16px;
}

/* 邀请码卡 */
.invite-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #FFF4EE, #FFE8DC);
}

.invite-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.invite-code {
  font-size: 24px;
  font-weight: bold;
  letter-spacing: 3px;
  color: $primary;
  font-family: monospace;
}

.invite-actions {
  display: flex;
  gap: 8px;
}

.invite-btn {
  font-size: 13px;
  padding: 6px 14px;
  border-radius: 999px;
  background: #fff;
  color: $primary;
  border: 1px solid $primary;
  line-height: 1.6;

  &[disabled] {
    opacity: 0.5;
  }
}

/* 列表区域 */
.list-wrap {
  margin-top: 12px;
}

.friend-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.friend-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
}

.friend-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #f0f0f0;
  flex-shrink: 0;
}

.friend-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;

  text {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.friend-ops {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.op-btn {
  font-size: 13px;
  padding: 5px 12px;
  border-radius: 999px;
  line-height: 1.6;

  &-primary {
    background: $primary;
    color: #fff;
  }

  &-danger {
    background: #fff;
    color: #E74C3C;
    border: 1px solid #E74C3C;
  }

  &-ghost {
    background: #f5f5f5;
    color: $text-light;

    &[disabled] {
      opacity: 0.6;
    }
  }
}

/* 搜索框 */
.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
}

.search-input {
  flex: 1;
  height: 40px;
  padding: 0 12px;
  border-radius: 12px;
  background: #F5F7FA;
  font-size: 14px;
}

.search-btn {
  font-size: 14px;
  padding: 8px 16px;
  border-radius: 12px;
  background: $primary;
  color: #fff;
  line-height: 1.4;
}

/* 空状态 / 提示 */
.empty-tip,
.search-hint {
  text-align: center;
  padding: 48px 24px;
}
</style>
