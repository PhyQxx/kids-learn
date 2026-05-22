import { checkAppUpdate } from '@/api/app'
import manifest from '@/manifest.json'

/**
 * 获取APP版本信息（APP-PLUS环境使用plus.runtime）
 */
function getAppVersionInfo() {
  return new Promise((resolve) => {
    // #ifdef APP-PLUS
    plus.runtime.getProperty(plus.runtime.appid, (info) => {
      resolve({
        versionName: info.version || manifest.versionName,
        versionCode: parseInt(info.versionCode) || parseInt(manifest.versionCode) || 0
      })
    })
    // #endif
    // #ifndef APP-PLUS
    resolve({
      versionName: manifest.versionName || '',
      versionCode: parseInt(manifest.versionCode) || 0
    })
    // #endif
  })
}

/**
 * 检查应用更新
 * @param {Object} options
 * @param {boolean} options.showToast - 是否显示检查过程中的提示 (用于手动触发)
 * @param {Function} options.onDownloadStart - 下载开始回调
 * @param {Function} options.onProgress - 下载进度回调 (progress) => {}
 * @param {Function} options.onDownloadComplete - 下载完成回调
 */
export default async function checkUpdate(options = {}) {
  const {
    showToast = false,
    onDownloadStart = null,
    onProgress = null,
    onDownloadComplete = null
  } = options

  // #ifdef APP-PLUS
  if (showToast) {
    uni.showToast({ icon: 'none', title: '正在检查更新···' })
  }

  try {
    const localInfo = await getAppVersionInfo()
    const platform = uni.getSystemInfoSync().platform === 'ios' ? 'ios' : 'android'
    const updateInfo = await checkAppUpdate(platform, localInfo.versionCode)

    if (showToast) {
      uni.hideToast()
    }

    if (!updateInfo || !updateInfo.versionCode) {
      if (showToast) {
        uni.showModal({
          title: '更新提示',
          content: '当前已是最新版本 v' + localInfo.versionName,
          showCancel: false
        })
      }
      return
    }

    const isForceUpdate = updateInfo.forceUpdate === 1
    uni.showModal({
      title: '发现新版本 v' + updateInfo.versionName,
      content: updateInfo.updateLog || '发现新版本，是否立即更新？',
      showCancel: !isForceUpdate,
      confirmText: '立即更新',
      success: (result) => {
        if (result.confirm) {
          downloadAndUpdate(updateInfo.downloadUrl, onDownloadStart, onProgress, onDownloadComplete)
        } else if (result.cancel && isForceUpdate) {
          plus.runtime.quit()
        }
      }
    })
  } catch (e) {
    console.error('检查更新失败', e)
    if (showToast) {
      uni.hideToast()
      uni.showToast({ icon: 'none', title: '检查更新失败' })
    }
  }
  // #endif

  // #ifndef APP-PLUS
  if (showToast) {
    uni.showModal({
      title: '更新提示',
      content: '当前平台不支持应用内更新',
      showCancel: false
    })
  }
  // #endif
}

/**
 * 下载并安装更新包
 */
function downloadAndUpdate(url, onDownloadStart, onProgress, onDownloadComplete) {
  if (!url) {
    uni.showToast({ icon: 'none', title: '下载地址为空' })
    return
  }

  if (onDownloadStart) {
    onDownloadStart()
  } else {
    uni.showLoading({ title: '正在下载更新...' })
  }

  // #ifdef APP-PLUS
  const downloadTask = plus.downloader.createDownload(url, {}, (d, status) => {
    if (status === 200) {
      console.log('下载更新成功：' + d.filename)
      installUpdate(d.filename)
      if (onDownloadComplete) {
        onDownloadComplete()
      } else {
        uni.hideLoading()
      }
    } else {
      console.log('下载更新失败！status=' + status)
      uni.showToast({ icon: 'none', title: '下载更新失败！' })
      if (onDownloadComplete) {
        onDownloadComplete()
      } else {
        uni.hideLoading()
      }
    }
  })

  if (onProgress) {
    downloadTask.addEventListener('statechanged', (task) => {
      if (task.state === 3) {
        const progress = parseInt((parseFloat(task.downloadedSize) / parseFloat(task.totalSize)) * 100)
        onProgress(progress)
      }
    })
  }

  downloadTask.start()
  // #endif
}

/**
 * 安装更新包（WGT和APK通用）
 */
function installUpdate(path) {
  // #ifdef APP-PLUS
  plus.nativeUI.showWaiting('安装更新中...')
  plus.runtime.install(path, { force: true }, () => {
    plus.nativeUI.closeWaiting()
    console.log('安装更新成功！')
    plus.nativeUI.alert('更新完成！', () => {
      plus.runtime.restart()
    })
  }, (e) => {
    plus.nativeUI.closeWaiting()
    console.log('安装更新失败！[' + e.code + ']：' + e.message)
    uni.showToast({ icon: 'none', title: '安装失败：' + e.message })
  })
  // #endif
}
