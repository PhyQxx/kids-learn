import { get, post, del, BASE_URL } from './request'

/**
 * 检查应用更新
 * @param {string} platform - 平台 (android/ios)
 * @param {number} versionCode - 当前版本编号
 */
export function checkAppUpdate(platform, versionCode) {
  return get('/public/app/check-update', { platform, versionCode })
}

/**
 * 获取反馈语音配置
 */
export function getFeedbackAudioConfig() {
  return get('/public/feedback-audio/config')
}

// ==================== 管理端：版本发布 ====================

/**
 * 上传升级包文件（wgt/apk/ipa），返回可访问 URL
 * @param {File} file - 通过 <input type="file"> 或 uni.chooseImage 拿到的文件
 */
export function uploadAppPackage(filePath) {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: BASE_URL + '/admin/file/upload-package',
      filePath,
      name: 'file',
      header: { Authorization: 'Bearer ' + uni.getStorageSync('token') },
      success: (res) => {
        try {
          const body = JSON.parse(res.data)
          if (body.code === 200 || body.code === 0) {
            resolve(body.data)
          } else {
            reject(new Error(body.message || '上传失败'))
          }
        } catch (e) {
          reject(new Error('解析响应失败'))
        }
      },
      fail: reject
    })
  })
}

/**
 * 保存/发布版本记录
 * @param {Object} data - { id?, platform, versionName, versionCode, downloadUrl, updateLog, forceUpdate, packageType }
 */
export function saveAppVersion(data) {
  return post('/admin/version/save', data)
}

/**
 * 版本列表（分页）
 */
export function listAppVersions(page = 1, pageSize = 20) {
  return get('/admin/version/list', { page, pageSize })
}

/**
 * 删除版本记录
 */
export function deleteAppVersion(id) {
  return del('/admin/version/' + id)
}
