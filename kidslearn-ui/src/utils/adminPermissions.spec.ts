import {
  ROLE_PERMISSION_PRESETS,
  formatPermissionCodes,
  mergePermissionCodes,
  parsePermissionText
} from './adminPermissions'

const presetKeys: string[] = ROLE_PERMISSION_PRESETS.map((preset) => preset.key)
const parsedCodes: string[] = parsePermissionText('admin:dashboard:read\nadmin:content:*')
const mergedText: string = mergePermissionCodes('admin:dashboard:read', ['admin:dashboard:read', 'admin:user:read'])
const formattedText: string = formatPermissionCodes(parsedCodes)

void presetKeys
void mergedText
void formattedText
