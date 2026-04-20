<template>
  <el-container class="admin-layout">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <div class="logo">
        <span class="logo-emoji">🌟</span>
        <span v-show="!isCollapse" class="logo-title">趣学星球</span>
      </div>
      <el-menu
        :default-active="route.path"
        :collapse="isCollapse"
        router
        background-color="#fff"
        text-color="#333"
        active-text-color="#FF6B6B"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>首页概览</template>
        </el-menu-item>

        <el-sub-menu index="content">
          <template #title>
            <el-icon><Reading /></el-icon>
            <span>内容管理</span>
          </template>
          <el-menu-item index="/content/subject">学科管理</el-menu-item>
          <el-menu-item index="/content/course">课程管理</el-menu-item>
          <el-menu-item index="/content/level">关卡管理</el-menu-item>
          <el-menu-item index="/content/question">题目管理</el-menu-item>
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
          <el-menu-item index="/system/role">角色管理</el-menu-item>
          <el-menu-item index="/system/config">系统配置</el-menu-item>
          <el-menu-item index="/system/log">操作日志</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
          <Expand v-if="isCollapse" />
          <Fold v-else />
        </el-icon>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" style="background-color: #FF6B6B">A</el-avatar>
              <span style="margin-left: 8px">{{ userStore.userInfo?.realName || '管理员' }}</span>
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
        <router-view />
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
}

.aside {
  border-right: 1px solid #f0f0f0;
  overflow-y: auto;
  transition: width 0.3s;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.logo-emoji {
  font-size: 28px;
}

.logo-title {
  font-size: 18px;
  font-weight: 800;
  color: #FF6B6B;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #666;
}

.collapse-btn:hover {
  color: #FF6B6B;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.main {
  background: var(--bg);
  overflow-y: auto;
}
</style>
