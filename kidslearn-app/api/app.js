import { get } from './request'

/**
 * 检查应用更新
 * @param {string} platform - 平台 (android/ios)
 * @param {number} versionCode - 当前版本编号
 */
export function checkAppUpdate(platform, versionCode) {
  return get('/public/app/check-update', { platform, versionCode })
}
