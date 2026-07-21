<script>
import { useRealtimeStore } from '@/store/realtime'
import { useUserStore } from '@/store/user'
import { getPostAuthRedirectUrl } from '@/utils/onboardingFlow.mjs'
import checkUpdate from '@/utils/update.mjs'

export default {
  onLaunch() {
    console.log('趣学星球 App Launch')
    // 检查应用更新（静默检查，不显示提示）
    // #ifdef APP-PLUS
    checkUpdate({ showToast: false })
    // #endif
    // 检查登录状态
    const token = uni.getStorageSync('token')
    if (!token) {
      uni.reLaunch({ url: '/pages/login/index' })
    } else {
      useRealtimeStore().connect()
      // 检查新手引导是否完成
      const userStore = useUserStore()
      uni.reLaunch({ url: getPostAuthRedirectUrl(userStore.userInfo) })
    }
  },
  onShow() {
    console.log('App Show')
    const token = uni.getStorageSync('token')
    if (token) {
      useRealtimeStore().connect()
    }
  },
  onHide() {
    console.log('App Hide')
    useRealtimeStore().close()
  }
}
</script>

<style>
@import '@tuniao/tn-style/dist/uniapp/index.css';
</style>

<style lang="scss">
@import '@/styles/tokens.scss';
@import '@/styles/common.scss';
@import '@/styles/planet-pages.scss';

/* 页面切换动画 */
uni-page-head {
  display: none !important;
}

/* 页面进入动画 */
@keyframes pageSlideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

/* 页面退出动画 */
@keyframes pageSlideOut {
  from {
    transform: translateX(0);
    opacity: 1;
  }
  to {
    transform: translateX(-30%);
    opacity: 0.6;
  }
}

/* 应用页面动画 */
.page-enter-active {
  animation: pageSlideIn 0.3s ease-out;
}

.page-leave-active {
  animation: pageSlideOut 0.3s ease-in;
}

/* 淡入淡出动画 */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 弹出动画 */
@keyframes popIn {
  from {
    transform: scale(0.9);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

.pop-enter-active {
  animation: popIn 0.2s ease-out;
}
</style>
