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
        <el-menu-item v-if="hasAnyPerm(['admin:dashboard:read', 'admin:*'])" index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>首页概览</template>
        </el-menu-item>

        <el-sub-menu v-if="hasAnyPerm(['admin:question:read', 'admin:subject:read', 'admin:content:read'])" index="learning">
          <template #title>
            <el-icon><Reading /></el-icon>
            <span>学习管理</span>
          </template>
          <el-menu-item v-if="hasPerm('admin:question:read')" index="/question-bank">题库管理</el-menu-item>
          <el-menu-item v-if="hasPerm('admin:subject:read')" index="/content">闯关管理</el-menu-item>
          <el-menu-item v-if="hasPerm('admin:content:read')" index="/content/audit">内容审核</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="hasAnyPerm(['admin:pet:read', 'admin:pet-item:read', 'admin:decoration:read'])" index="pet">
          <template #title>
            <el-icon><Pointer /></el-icon>
            <span>宠物管理</span>
          </template>
          <el-menu-item v-if="hasPerm('admin:pet:read')" index="/pet/list">宠物种类</el-menu-item>
          <el-menu-item v-if="hasPerm('admin:pet-item:read')" index="/pet/item">道具管理</el-menu-item>
          <el-menu-item v-if="hasPerm('admin:decoration:read')" index="/pet/decoration">装饰管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="hasAnyPerm(['admin:achievement:read', 'admin:sticker:read', 'admin:title:read'])" index="achievement">
          <template #title>
            <el-icon><Trophy /></el-icon>
            <span>成就管理</span>
          </template>
          <el-menu-item v-if="hasPerm('admin:achievement:read')" index="/achievement/list">成就定义</el-menu-item>
          <el-menu-item v-if="hasPerm('admin:sticker:read')" index="/achievement/sticker">贴纸管理</el-menu-item>
          <el-menu-item v-if="hasPerm('admin:title:read')" index="/achievement/title">称号管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="hasAnyPerm(['admin:dashboard:read', 'admin:order:read', 'admin:challenge:read'])" index="operation">
          <template #title>
            <el-icon><Promotion /></el-icon>
            <span>运营管理</span>
          </template>
          <el-menu-item v-if="hasAnyPerm(['admin:dashboard:read', 'admin:*'])" index="/ranking">排行榜管理</el-menu-item>
          <el-menu-item v-if="hasPerm('admin:challenge:read')" index="/challenge">挑战赛管理</el-menu-item>
          <el-menu-item v-if="hasPerm('admin:order:read')" index="/order">订单管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="hasAnyPerm(['admin:user:read', 'admin:role:read', 'admin:config:read', 'admin:log:read', 'admin:dict:read', 'admin:version:read'])" index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>

          <el-sub-menu v-if="hasAnyPerm(['admin:user:read', 'admin:role:read'])" index="system-auth">
            <template #title>
              <el-icon><Lock /></el-icon>
              <span>权限管理</span>
            </template>
            <el-menu-item v-if="hasPerm('admin:user:read')" index="/system/user">用户管理</el-menu-item>
            <el-menu-item v-if="hasPerm('admin:role:read')" index="/system/role">角色管理</el-menu-item>
          </el-sub-menu>

          <el-sub-menu v-if="hasAnyPerm(['admin:config:read', 'admin:dict:read'])" index="system-config">
            <template #title>
              <el-icon><Tools /></el-icon>
              <span>配置管理</span>
            </template>
            <el-menu-item v-if="hasPerm('admin:config:read')" index="/system/config">系统配置</el-menu-item>
            <el-menu-item v-if="hasPerm('admin:config:read')" index="/system/ai">AI配置</el-menu-item>
            <el-menu-item v-if="hasPerm('admin:config:read')" index="/system/feedback-audio">反馈语音配置</el-menu-item>
            <el-menu-item v-if="hasPerm('admin:dict:read')" index="/system/dict">字典管理</el-menu-item>
          </el-sub-menu>

          <el-sub-menu v-if="hasAnyPerm(['admin:log:read', 'admin:version:read'])" index="system-ops">
            <template #title>
              <el-icon><Monitor /></el-icon>
              <span>运维管理</span>
            </template>
            <el-menu-item v-if="hasPerm('admin:log:read')" index="/system/log">操作日志</el-menu-item>
            <el-menu-item v-if="hasPerm('admin:version:read')" index="/system/version">版本管理</el-menu-item>
          </el-sub-menu>
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

// 权限检查函数
function hasPerm(permission: string): boolean {
  // 如果没有userInfo（还没加载完），暂时显示所有菜单
  if (!userStore.userInfo) return true
  return userStore.hasPermission(permission)
}

function hasAnyPerm(perms: string[]): boolean {
  if (!userStore.userInfo) return true
  return userStore.hasAnyPermission(perms)
}

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
