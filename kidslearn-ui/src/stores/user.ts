import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const userInfo = ref<any>(null)

  // 从userInfo中获取权限列表
  const permissions = computed(() => {
    return userInfo.value?.permissions || userInfo.value?.rolePermissions || []
  })

  const isAdmin = computed(() => {
    return userInfo.value?.userType === 3 || userInfo.value?.isAdmin === true
  })

  function setToken(val: string) {
    token.value = val
    localStorage.setItem('admin_token', val)
  }

  function setUserInfo(info: any) {
    userInfo.value = info
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_refresh_token')
  }

  /**
   * 检查是否拥有指定权限
   * @param permission 权限码，如 'admin:dashboard:read'
   */
  function hasPermission(permission: string): boolean {
    // 超级管理员拥有所有权限（兜底保护：userType=3 且无 permissions 时也放行）
    if (isAdmin.value && (!userInfo.value?.permissions || userInfo.value.permissions.length === 0)) {
      return true
    }
    if (permissions.value.includes('admin:*')) return true

    // 检查精确匹配
    if (permissions.value.includes(permission)) return true

    // 检查通配符匹配，如 admin:subject:* 可匹配 admin:subject:read
    const parts = permission.split(':')
    if (parts.length >= 2) {
      const wildcard = parts.slice(0, -1).join(':') + ':*'
      if (permissions.value.includes(wildcard)) return true
    }

    return false
  }

  /**
   * 检查是否拥有任一权限
   */
  function hasAnyPermission(perms: string[]): boolean {
    return perms.some(p => hasPermission(p))
  }

  return {
    token, userInfo, permissions, isAdmin,
    setToken, setUserInfo, logout,
    hasPermission, hasAnyPermission
  }
})
