export interface PermissionPreset {
  key: string
  name: string
  description: string
  permissions: string[]
}

export const ROLE_PERMISSION_PRESETS: PermissionPreset[] = [
  {
    key: 'super-admin',
    name: '超级管理员',
    description: '拥有所有后台模块和操作权限',
    permissions: ['admin:*']
  },
  {
    key: 'dashboard-viewer',
    name: '数据查看',
    description: '查看首页控制台和运营日志',
    permissions: ['admin:dashboard:read', 'admin:log:read']
  },
  {
    key: 'content-manager',
    name: '内容运营',
    description: '维护课程、关卡、题目和年级内容',
    permissions: [
      'admin:dashboard:read',
      'admin:subject:*',
      'admin:course:*',
      'admin:grade-level:*',
      'admin:level:*',
      'admin:question:*',
      'admin:practice:*'
    ]
  },
  {
    key: 'game-manager',
    name: '游戏化运营',
    description: '维护宠物、道具、装饰、成就、贴纸和称号',
    permissions: [
      'admin:dashboard:read',
      'admin:pet:*',
      'admin:pet-item:*',
      'admin:decoration:*',
      'admin:achievement:*',
      'admin:sticker:*',
      'admin:title:*'
    ]
  },
  {
    key: 'system-manager',
    name: '系统管理',
    description: '维护用户、角色、配置、字典和版本',
    permissions: [
      'admin:dashboard:read',
      'admin:user:*',
      'admin:role:*',
      'admin:config:*',
      'admin:dict:*',
      'admin:version:*',
      'admin:log:read'
    ]
  }
]

export function parsePermissionText(value: string): string[] {
  return value
    .split(/[\s,;，；]+/)
    .map((code) => code.trim())
    .filter(Boolean)
}

export function formatPermissionCodes(codes: string[]): string {
  return Array.from(new Set(codes)).join('\n')
}

export function mergePermissionCodes(currentValue: string, codes: string[]): string {
  return formatPermissionCodes([...parsePermissionText(currentValue), ...codes])
}
