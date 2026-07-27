import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页概览' },
      },
      {
        path: 'tasks',
        name: 'TaskCenter',
        component: () => import('@/views/system/TaskCenter.vue'),
        meta: { title: '任务中心' },
      },
      {
        path: 'question-bank',
        name: 'QuestionBank',
        component: () => import('@/views/content/QuestionBank.vue'),
        meta: { title: '题库管理' },
      },
      {
        path: 'question-bank/new',
        name: 'QuestionCreate',
        component: () => import('@/views/content/QuestionBank.vue'),
        meta: { title: '新增题目', permission: 'admin:question:read' },
      },
      {
        path: 'question-bank/:id/edit',
        name: 'QuestionEdit',
        component: () => import('@/views/content/QuestionBank.vue'),
        meta: { title: '编辑题目', permission: 'admin:question:read' },
      },
      {
        path: 'content',
        name: 'ContentExplorer',
        component: () => import('@/views/content/ContentExplorer.vue'),
        meta: { title: '闯关管理' },
      },
      {
        path: 'content/audit',
        name: 'ContentAudit',
        component: () => import('@/views/content/AuditQueue.vue'),
        meta: { title: '内容审核' },
      },
      {
        path: 'pet/list',
        name: 'PetList',
        component: () => import('@/views/pet/list.vue'),
        meta: { title: '宠物种类' },
      },
      {
        path: 'pet/item',
        name: 'PetItem',
        component: () => import('@/views/pet/item.vue'),
        meta: { title: '道具管理' },
      },
      {
        path: 'pet/decoration',
        name: 'PetDecoration',
        component: () => import('@/views/pet/decoration.vue'),
        meta: { title: '装饰管理' },
      },
      {
        path: 'achievement/list',
        name: 'AchievementList',
        component: () => import('@/views/achievement/list.vue'),
        meta: { title: '成就定义' },
      },
      {
        path: 'achievement/sticker',
        name: 'Sticker',
        component: () => import('@/views/achievement/sticker.vue'),
        meta: { title: '贴纸管理' },
      },
      {
        path: 'achievement/title',
        name: 'Title',
        component: () => import('@/views/achievement/title.vue'),
        meta: { title: '称号管理' },
      },
      {
        path: 'ranking',
        name: 'Ranking',
        component: () => import('@/views/ranking/index.vue'),
        meta: { title: '排行榜管理' },
      },
      {
        path: 'order',
        name: 'Order',
        component: () => import('@/views/order/index.vue'),
        meta: { title: '订单管理' },
      },
      {
        path: 'challenge',
        name: 'Challenge',
        component: () => import('@/views/challenge/index.vue'),
        meta: { title: '挑战赛管理' },
      },
      {
        path: 'challenge/season',
        name: 'ChallengeSeason',
        component: () => import('@/views/challenge/season.vue'),
        meta: { title: '排位赛赛季' },
      },
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/user.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'system/role',
        name: 'SystemRole',
        component: () => import('@/views/system/role.vue'),
        meta: { title: '角色管理' },
      },
      {
        path: 'system/config',
        name: 'SystemConfig',
        component: () => import('@/views/system/config.vue'),
        meta: { title: '系统配置' },
      },
      {
        path: 'system/ai',
        name: 'SystemAiConfig',
        component: () => import('@/views/system/AiConfig.vue'),
        meta: { title: 'AI配置' },
      },
      {
        path: 'system/feedback-audio',
        name: 'SystemFeedbackAudio',
        component: () => import('@/views/system/FeedbackAudioConfig.vue'),
        meta: { title: '反馈语音配置' },
      },
      {
        path: 'system/log',
        name: 'SystemLog',
        component: () => import('@/views/system/log.vue'),
        meta: { title: '操作日志' },
      },
      {
        path: 'system/dict',
        name: 'SystemDict',
        component: () => import('@/views/system/dict.vue'),
        meta: { title: '字典管理' },
      },
      {
        path: 'system/version',
        name: 'SystemVersion',
        component: () => import('@/views/system/version.vue'),
        meta: { title: '版本管理' },
      },
    ],
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '权限不足' },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由与权限的映射关系
const ROUTE_PERMISSIONS: Record<string, string> = {
  '/dashboard': 'admin:dashboard:read',
  '/tasks': 'admin:dashboard:read',
  '/question-bank': 'admin:question:read',
  '/content': 'admin:subject:read',
  '/content/audit': 'admin:content:read',
  '/pet/list': 'admin:pet:read',
  '/pet/item': 'admin:pet-item:read',
  '/pet/decoration': 'admin:decoration:read',
  '/achievement/list': 'admin:achievement:read',
  '/achievement/sticker': 'admin:sticker:read',
  '/achievement/title': 'admin:title:read',
  '/ranking': 'admin:dashboard:read',
  '/order': 'admin:order:read',
  '/challenge': 'admin:challenge:read',
  '/challenge/season': 'admin:challenge:read',
  '/system/user': 'admin:user:read',
  '/system/role': 'admin:role:read',
  '/system/config': 'admin:config:read',
  '/system/ai': 'admin:config:read',
  '/system/feedback-audio': 'admin:config:read',
  '/system/log': 'admin:log:read',
  '/system/dict': 'admin:dict:read',
  '/system/version': 'admin:version:read',
}

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('admin_token')

  // 未登录跳转登录页
  if (to.path !== '/login' && !token) {
    next('/login')
    return
  }

  // 已登录访问登录页跳转首页
  if (to.path === '/login' && token) {
    next('/dashboard')
    return
  }

  // 权限检查（动态导入避免循环依赖）
  const requiredPermission = (to.meta.permission as string | undefined) || ROUTE_PERMISSIONS[to.path]
  if (requiredPermission && token) {
    import('@/stores/user').then(async ({ useUserStore }) => {
      const userStore = useUserStore()

      // 如果userInfo未加载，尝试加载
      if (!userStore.userInfo) {
        try {
          const { default: request } = await import('@/utils/request')
          const res: any = await request.get('/user/info')
          if (res?.code === 200 && res.data) {
            userStore.setUserInfo(res.data)
          }
        } catch {
          // 加载失败，放行（避免阻塞用户）
        }
      }

      // 检查权限
      if (userStore.userInfo && !userStore.hasPermission(requiredPermission)) {
        next('/403')
        return
      }
      next()
    })
    return
  }

  next()
})

export default router
