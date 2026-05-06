<script>
import { useRealtimeStore } from '@/store/realtime'
import { useUserStore } from '@/store/user'

export default {
  onLaunch() {
    console.log('趣学星球 App Launch')
    // 检查登录状态
    const token = uni.getStorageSync('token')
    if (!token) {
      uni.reLaunch({ url: '/pages/login/index' })
    } else {
      useRealtimeStore().connect()
      // 检查新手引导是否完成
      const userStore = useUserStore()
      const step = userStore.onboardingStep
      if (step > 0 && step < 3) {
        uni.reLaunch({ url: '/pages/onboarding/index' })
      } else {
        uni.reLaunch({ url: '/pages/main/index' })
      }
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
