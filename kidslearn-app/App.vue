<script>
export default {
  onLaunch() {
    console.log('趣学星球 App Launch')
    // 检查登录状态
    const token = uni.getStorageSync('token')
    if (!token) {
      uni.reLaunch({ url: '/pages/login/index' })
    } else {
      uni.reLaunch({ url: '/pages/main/index' })
    }
  },
  onShow() {
    console.log('App Show')
  },
  onHide() {
    console.log('App Hide')
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
