import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  // access token 过期时间戳（毫秒）。与后端 TOKEN_EXPIRE 对齐，0 表示未知。
  const tokenExpireAt = ref(Number(localStorage.getItem('admin_token_expire') || 0))
  const userInfo = ref<any>(null)

  // 从userInfo中获取权限列表
  const permissions = computed(() => {
    return userInfo.value?.permissions || userInfo.value?.rolePermissions || []
  })

  const isAdmin = computed(() => {
    return userInfo.value?.userType === 3 || userInfo.value?.isAdmin === true
  })

  function setToken(val: string, expireAt?: number) {
    token.value = val
    localStorage.setItem('admin_token', val)
    // 未传入过期时间时，按后端默认有效期（7200s）估算，留 10s 余量
    const expire = expireAt ?? (Date.now() + (7200 - 10) * 1000)
    tokenExpireAt.value = expire
    localStorage.setItem('admin_token_expire', String(expire))
  }

  /**
   * 统一写入 token 对（登录/刷新成功后调用）
   * @param accessToken 新 access token
   * @param refreshToken 新 refresh token（可选，滑动续期时仅换 access）
   * @param expireAt 过期时间戳（毫秒），不传则按默认有效期估算
   */
  function setTokens(accessToken: string, refreshToken?: string, expireAt?: number) {
    setToken(accessToken, expireAt)
    if (refreshToken) {
      localStorage.setItem('admin_refresh_token', refreshToken)
    }
  }

  function setUserInfo(info: any) {
    userInfo.value = info
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    tokenExpireAt.value = 0
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_token_expire')
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
    token, tokenExpireAt, userInfo, permissions, isAdmin,
    setToken, setTokens, setUserInfo, logout,
    hasPermission, hasAnyPermission
  }
})
