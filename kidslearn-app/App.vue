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
</style>
