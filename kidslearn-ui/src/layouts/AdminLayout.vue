<template>
  <el-container class="admin-layout">
    <el-aside :width="isCollapse ? '72px' : '232px'" class="aside">
      <div class="logo" :class="{ collapsed: isCollapse }">
        <span class="logo-mark">K</span>
        <span v-show="!isCollapse" class="logo-title">趣学星球</span>
      </div>

      <el-menu
        :default-active="route.path"
        :collapse="isCollapse"
        router
        class="side-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>首页概览</template>
        </el-menu-item>

        <el-sub-menu index="learning">
          <template #title>
            <el-icon><Reading /></el-icon>
            <span>学习管理</span>
          </template>
          <el-menu-item index="/question-bank">题库管理</el-menu-item>
          <el-menu-item index="/content">闯关管理</el-menu-item>
          <el-menu-item index="/content/audit">内容审核</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="pet">
          <template #title>
            <el-icon><Pointer /></el-icon>
            <span>宠物管理</span>
          </template>
          <el-menu-item index="/pet/list">宠物种类</el-menu-item>
          <el-menu-item index="/pet/item">道具管理</el-menu-item>
          <el-menu-item index="/pet/decoration">装饰管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="achievement">
          <template #title>
            <el-icon><Trophy /></el-icon>
            <span>成就管理</span>
          </template>
          <el-menu-item index="/achievement/list">成就定义</el-menu-item>
          <el-menu-item index="/achievement/sticker">贴纸管理</el-menu-item>
          <el-menu-item index="/achievement/title">称号管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/user">用户管理</el-menu-item>
          <el-menu-item index="/system/config">系统配置</el-menu-item>
          <el-menu-item index="/system/ai">AI配置</el-menu-item>
          <el-menu-item index="/system/feedback-audio">反馈语音配置</el-menu-item>
          <el-menu-item index="/system/log">操作日志</el-menu-item>
          <el-menu-item index="/system/dict">字典管理</el-menu-item>
          <el-menu-item index="/system/version">版本管理</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-button class="collapse-btn" text circle @click="isCollapse = !isCollapse">
            <el-icon>
              <Expand v-if="isCollapse" />
              <Fold v-else />
            </el-icon>
          </el-button>
          <div>
            <div class="page-title">{{ route.meta.title || '控制台' }}</div>
            <div class="page-subtitle">儿童游戏化学习平台管理后台</div>
          </div>
        </div>

        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="34" class="admin-avatar">A</el-avatar>
              <span>{{ userStore.userInfo?.realName || '管理员' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view v-slot="{ Component }">
          <component :is="Component" class="page-fill" />
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)

function handleCommand(command: string) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  background: var(--admin-bg);
}

.aside {
  background: var(--admin-surface);
  border-right: 1px solid var(--admin-border);
  box-shadow: 4px 0 18px rgba(31, 41, 55, 0.04);
  overflow-y: auto;
  transition: width 0.24s ease;
}

.logo {
  height: 68px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  border-bottom: 1px solid var(--admin-border);
}

.logo.collapsed {
  justify-content: center;
  padding: 0;
}

.logo-mark {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #fff;
  font-weight: 800;
  background: linear-gradient(135deg, var(--primary), var(--teal));
}

.logo-title {
  font-size: 18px;
  font-weight: 800;
  color: var(--admin-text);
  letter-spacing: 0;
}

.side-menu {
  border-right: 0;
  padding: 10px 8px 16px;
}

.side-menu:not(.el-menu--collapse) {
  width: 100%;
}

.header {
  height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid var(--admin-border);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.collapse-btn {
  color: var(--admin-muted);
}

.collapse-btn:hover {
  color: var(--primary);
  background: rgba(255, 107, 107, 0.08);
}

.page-title {
  color: var(--admin-text);
  font-size: 17px;
  font-weight: 800;
  line-height: 1.3;
}

.page-subtitle {
  margin-top: 3px;
  color: var(--admin-muted);
  font-size: 12px;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 9px;
  color: var(--admin-text);
  cursor: pointer;
}

.admin-avatar {
  background: var(--primary);
  font-weight: 700;
}

.main {
  min-width: 0;
  padding: 24px;
  background: var(--admin-bg);
  overflow: hidden;
}
</style>

<!-- 非 scoped：页面级布局，page-fill 和 .el-card 是同一个元素 -->
<style>
/* 卡片填满 main */
.page-fill {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 卡片 header 不收缩 */
.page-fill > .el-card__header {
  flex-shrink: 0;
}

/* 卡片 body 撑满剩余空间，内部由 JS 控制布局 */
.page-fill > .el-card__body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
</style>
