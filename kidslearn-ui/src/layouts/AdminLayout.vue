<template>
  <el-container class="admin-layout">
    <el-aside v-if="!isMobile" :width="isCollapse ? '72px' : '232px'" class="aside">
      <div class="logo" :class="{ collapsed: isCollapse }">
        <span class="logo-mark">K</span>
        <div v-show="!isCollapse" class="logo-copy">
          <span class="logo-title">趣学星球</span>
          <small>管理后台</small>
        </div>
      </div>
      <div v-if="!userStore.userInfo" class="menu-skeleton" aria-label="正在加载菜单">
        <el-skeleton :rows="8" animated />
      </div>
      <AdminNavMenu v-else :groups="visibleGroups" :active-path="route.path" :collapsed="isCollapse" />
    </el-aside>

    <el-drawer v-model="mobileMenuOpen" class="mobile-nav-drawer" direction="ltr" size="280px" :with-header="false">
      <div class="logo">
        <span class="logo-mark">K</span>
        <div class="logo-copy"><span class="logo-title">趣学星球</span><small>管理后台</small></div>
      </div>
      <div v-if="!userStore.userInfo" class="menu-skeleton"><el-skeleton :rows="8" animated /></div>
      <AdminNavMenu v-else :groups="visibleGroups" :active-path="route.path" @select="mobileMenuOpen = false" />
    </el-drawer>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-button class="collapse-btn" text circle :aria-label="isMobile ? '打开导航' : (isCollapse ? '展开导航' : '收起导航')" @click="toggleNavigation">
            <el-icon>
              <Menu v-if="isMobile" />
              <Expand v-else-if="isCollapse" />
              <Fold v-else />
            </el-icon>
          </el-button>
          <span class="workspace-label">运营工作台</span>
        </div>

        <div class="header-right">
          <el-button class="search-trigger" plain @click="searchOpen = true">
            <el-icon><Search /></el-icon>
            <span>搜索页面</span>
            <kbd>⌘K</kbd>
          </el-button>
          <el-dropdown @command="handleCommand">
            <button type="button" class="user-info">
              <el-avatar :size="34" class="admin-avatar">A</el-avatar>
              <span class="user-name">{{ userStore.userInfo?.realName || '管理员' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </button>
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

    <el-dialog v-model="searchOpen" class="command-dialog" width="600px" title="搜索后台页面" append-to-body>
      <el-input v-model="searchKeyword" size="large" clearable autofocus placeholder="输入页面名称或功能关键词" :prefix-icon="Search" />
      <div class="command-results">
        <button v-for="item in searchResults" :key="item.path" type="button" @click="goToSearchResult(item.path)">
          <span>{{ item.label }}</span>
          <small>{{ item.group }}</small>
        </button>
        <AdminEmptyState v-if="searchKeyword && !searchResults.length" type="filtered" title="没有匹配页面" description="换一个页面名称或功能关键词试试。" />
      </div>
    </el-dialog>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DataLine, Reading, Trophy, Promotion, UserFilled, Setting, Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import AdminEmptyState from '@/components/admin/AdminEmptyState.vue'
import AdminNavMenu, { type AdminNavGroup } from '@/components/admin/AdminNavMenu.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)
const isMobile = ref(false)
const mobileMenuOpen = ref(false)
const searchOpen = ref(false)
const searchKeyword = ref('')
let mediaQuery: MediaQueryList | null = null

const navGroups: AdminNavGroup[] = [
  { key: 'workspace', label: '工作台', icon: DataLine, items: [
    { label: '首页概览', path: '/dashboard', permission: 'admin:dashboard:read', keywords: ['数据', '待办', '概览'] },
    { label: '任务中心', path: '/tasks', permission: 'admin:dashboard:read', keywords: ['批量', '进度', '失败'] },
  ] },
  { key: 'content', label: '内容生产', icon: Reading, items: [
    { label: '题库管理', path: '/question-bank', permission: 'admin:question:read', keywords: ['题目', '语音', '解析'] },
    { label: '学科与关卡', path: '/content', permission: 'admin:subject:read', keywords: ['闯关', '年级', '课程'] },
    { label: '内容审核', path: '/content/audit', permission: 'admin:content:read', keywords: ['审核', 'AI预审'] },
  ] },
  { key: 'incentive', label: '激励运营', icon: Trophy, items: [
    { label: '宠物种类', path: '/pet/list', permission: 'admin:pet:read' },
    { label: '道具管理', path: '/pet/item', permission: 'admin:pet-item:read' },
    { label: '装饰管理', path: '/pet/decoration', permission: 'admin:decoration:read' },
    { label: '成就定义', path: '/achievement/list', permission: 'admin:achievement:read' },
    { label: '贴纸管理', path: '/achievement/sticker', permission: 'admin:sticker:read' },
    { label: '称号管理', path: '/achievement/title', permission: 'admin:title:read' },
  ] },
  { key: 'business', label: '活动与商业', icon: Promotion, items: [
    { label: '排行榜', path: '/ranking', permission: 'admin:dashboard:read' },
    { label: '挑战赛', path: '/challenge', permission: 'admin:challenge:read' },
    { label: '排位赛赛季', path: '/challenge/season', permission: 'admin:challenge:read' },
    { label: '订单管理', path: '/order', permission: 'admin:order:read' },
  ] },
  { key: 'access', label: '用户与权限', icon: UserFilled, items: [
    { label: '用户管理', path: '/system/user', permission: 'admin:user:read' },
    { label: '角色管理', path: '/system/role', permission: 'admin:role:read' },
  ] },
  { key: 'platform', label: '平台设置', icon: Setting, items: [
    { label: '基础配置', path: '/system/config', permission: 'admin:config:read' },
    { label: 'AI 服务', path: '/system/ai', permission: 'admin:config:read' },
    { label: '反馈语音', path: '/system/feedback-audio', permission: 'admin:config:read' },
    { label: '字典管理', path: '/system/dict', permission: 'admin:dict:read' },
    { label: '版本管理', path: '/system/version', permission: 'admin:version:read' },
    { label: '操作日志', path: '/system/log', permission: 'admin:log:read' },
  ] },
]

const visibleGroups = computed(() => navGroups
  .map(group => ({ ...group, items: group.items.filter(item => hasPerm(item.permission)) }))
  .filter(group => group.items.length > 0))

const searchableItems = computed(() => visibleGroups.value.flatMap(group => group.items.map(item => ({ ...item, group: group.label }))))
const searchResults = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return searchableItems.value.slice(0, 8)
  return searchableItems.value.filter(item => [item.label, item.group, ...(item.keywords || [])].some(value => value.toLowerCase().includes(keyword))).slice(0, 10)
})

// 权限检查函数
function hasPerm(permission: string): boolean {
  // 如果没有userInfo（还没加载完），暂时显示所有菜单
  if (!userStore.userInfo) return true
  return userStore.hasPermission(permission)
}

function toggleNavigation() {
  if (isMobile.value) mobileMenuOpen.value = true
  else isCollapse.value = !isCollapse.value
}

function handleMediaChange(event: MediaQueryListEvent | MediaQueryList) {
  isMobile.value = event.matches
  if (!event.matches) mobileMenuOpen.value = false
}

function handleShortcut(event: KeyboardEvent) {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    searchOpen.value = true
  }
}

function goToSearchResult(path: string) {
  searchOpen.value = false
  searchKeyword.value = ''
  router.push(path)
}

function handleCommand(command: string) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}

onMounted(() => {
  mediaQuery = window.matchMedia('(max-width: 1023px)')
  handleMediaChange(mediaQuery)
  mediaQuery.addEventListener('change', handleMediaChange)
  window.addEventListener('keydown', handleShortcut)
})

onBeforeUnmount(() => {
  mediaQuery?.removeEventListener('change', handleMediaChange)
  window.removeEventListener('keydown', handleShortcut)
})
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

.logo-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.logo-copy small {
  margin-top: -2px;
  color: var(--admin-muted);
  font-size: 10px;
  line-height: 14px;
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

.menu-skeleton {
  padding: var(--space-4);
}

.header {
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-6);
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

.workspace-label {
  color: var(--color-gray-700);
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.user-info {
  appearance: none;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: var(--space-1);
  color: var(--admin-text);
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: var(--radius-control);
}

.admin-avatar {
  background: var(--primary);
  font-weight: 700;
}

.main {
  min-width: 0;
  max-width: var(--content-max-width);
  padding: var(--space-6);
  background: var(--admin-bg);
  overflow: hidden;
}

.search-trigger kbd {
  padding: 1px 6px;
  color: var(--admin-muted);
  font: inherit;
  font-size: var(--font-size-caption);
  background: var(--color-gray-100);
  border: 1px solid var(--admin-border);
  border-radius: 4px;
}

.command-results {
  max-height: 420px;
  margin-top: var(--space-3);
  overflow-y: auto;
}

.command-results > button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: var(--space-3);
  color: var(--admin-text);
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: var(--radius-control);
}

.command-results > button:hover,
.command-results > button:focus-visible {
  background: var(--color-brand-50);
}

.command-results small {
  color: var(--admin-muted);
}

@media (max-width: 1023px) {
  .header { padding: 0 var(--space-5); }
  .main { padding: var(--space-5); }
}

@media (max-width: 767px) {
  .header { padding: 0 var(--space-4); }
  .main { padding: var(--space-4); overflow: auto; }
  .workspace-label, .search-trigger span, .search-trigger kbd, .user-name { display: none; }
  .page-fill { overflow: visible; }
}
</style>

<style>
.mobile-nav-drawer .el-drawer__body {
  padding: 0;
}

.command-dialog .el-dialog__body {
  padding-top: var(--space-4);
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
