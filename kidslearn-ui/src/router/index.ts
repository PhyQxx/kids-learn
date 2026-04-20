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
      // 内容管理
      {
        path: 'content/subject',
        name: 'Subject',
        component: () => import('@/views/content/subject.vue'),
        meta: { title: '学科管理' },
      },
      {
        path: 'content/course',
        name: 'Course',
        component: () => import('@/views/content/course.vue'),
        meta: { title: '课程管理' },
      },
      {
        path: 'content/level',
        name: 'Level',
        component: () => import('@/views/content/level.vue'),
        meta: { title: '关卡管理' },
      },
      {
        path: 'content/question',
        name: 'Question',
        component: () => import('@/views/content/question.vue'),
        meta: { title: '题目管理' },
      },
      // 游戏化管理
      {
        path: 'pet/list',
        name: 'PetList',
        component: () => import('@/views/pet/list.vue'),
        meta: { title: '宠物管理' },
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
      // 成就管理
      {
        path: 'achievement/list',
        name: 'AchievementList',
        component: () => import('@/views/achievement/list.vue'),
        meta: { title: '成就管理' },
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
      // 系统管理
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
        path: 'system/log',
        name: 'SystemLog',
        component: () => import('@/views/system/log.vue'),
        meta: { title: '操作日志' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('admin_token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
