<template>
  <AppLayout theme="kids" title="账号安全" :show-back="true" active-nav="/pages/mine/index">
    <view class="security-page">
      <view class="security-card">
        <text class="card-title">修改密码</text>
        <input class="field" v-model="passwordForm.oldPassword" type="password" password placeholder="当前密码" />
        <input class="field" v-model="passwordForm.newPassword" type="password" password placeholder="新密码（6-50位）" />
        <input class="field" v-model="passwordForm.confirm" type="password" password placeholder="确认新密码" />
        <button class="primary-action" @tap="submitPassword">保存新密码</button>
      </view>

      <view class="security-card">
        <text class="card-title">换绑手机号</text>
        <text class="card-desc">当前手机号：{{ maskedPhone }}</text>
        <input class="field" v-model="phoneForm.phone" type="number" maxlength="11" placeholder="新手机号" />
        <view class="code-row">
          <input class="field code-field" v-model="phoneForm.code" type="number" maxlength="6" placeholder="验证码" />
          <button class="minor-action" :disabled="countdown > 0" @tap="sendPhoneCode">{{ countdown > 0 ? `${countdown}s` : '发送验证码' }}</button>
        </view>
        <input class="field" v-model="phoneForm.password" type="password" password placeholder="账号密码" />
        <button class="primary-action" @tap="submitPhone">确认换绑</button>
      </view>

      <view class="security-card">
        <text class="card-title">修改家长PIN</text>
        <input class="field" v-model="pinForm.currentPin" type="number" password maxlength="6" placeholder="当前6位PIN" />
        <input class="field" v-model="pinForm.newPin" type="number" password maxlength="6" placeholder="新6位PIN" />
        <input class="field" v-model="pinForm.confirm" type="number" password maxlength="6" placeholder="确认新PIN" />
        <button class="primary-action" @tap="submitPin">保存新PIN</button>
      </view>

      <view class="security-card">
        <view class="card-heading"><text class="card-title">登录设备</text><text class="refresh-link" @tap="loadDevices">刷新</text></view>
        <view v-if="devices.length === 0" class="empty-text">暂无设备记录</view>
        <view v-for="device in devices" :key="device.deviceId" class="device-row">
          <view><text class="device-name">{{ device.name }}</text><text v-if="device.current" class="current-tag">当前</text></view>
          <text v-if="!device.current" class="danger-link" @tap="removeDevice(device)">退出</text>
        </view>
      </view>

      <view class="security-card danger-card">
        <text class="card-title">停用账号</text>
        <text class="card-desc">账号数据暂不删除；停用后所有设备立即退出。</text>
        <button class="danger-action" @tap="confirmDeactivate">停用当前账号</button>
      </view>
      <view class="security-card">
        <text class="card-title">导出账号数据</text>
        <text class="card-desc">通过家长PIN验证后导出账号、学习、错题、通知与挑战记录，不包含密码和PIN哈希。</text>
        <button class="minor-action" @tap="exportData">验证并导出</button>
      </view>
    </view>
  </AppLayout>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { onMounted } from 'vue'
import AppLayout from '@/components/AppLayout.vue'
import { useUserStore } from '@/store/user'
import { changeParentPin } from '@/api/auth'
import { changePhone, deactivateAccount, exportAccountData, getDevices, revokeDevice, sendPhoneChangeCode, updatePassword } from '@/api/user'

const userStore = useUserStore()
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const phoneForm = reactive({ phone: '', code: '', password: '' })
const pinForm = reactive({ currentPin: '', newPin: '', confirm: '' })
const devices = ref([])
const countdown = ref(0)
let timer
const maskedPhone = computed(() => userStore.userInfo?.phone?.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') || '未绑定')
onMounted(loadDevices)
onBeforeUnmount(() => { if (timer) clearInterval(timer) })

async function submitPassword() {
  if (passwordForm.newPassword.length < 6 || passwordForm.newPassword.length > 50) return toast('新密码长度必须为6-50位')
  if (passwordForm.newPassword !== passwordForm.confirm) return toast('两次输入的新密码不一致')
  await run(() => updatePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword }), '密码修改成功')
}
async function sendPhoneCode() {
  if (!/^1[3-9]\d{9}$/.test(phoneForm.phone)) return toast('请输入正确的新手机号')
  try {
    await sendPhoneChangeCode(phoneForm.phone)
    countdown.value = 60
    timer = setInterval(() => { countdown.value -= 1; if (countdown.value <= 0) { clearInterval(timer); timer = null } }, 1000)
    toast('验证码已发送', 'success')
  } catch (error) { toast(error?.msg || error?.message || '发送失败') }
}
async function submitPhone() {
  if (!/^\d{6}$/.test(phoneForm.code)) return toast('请输入6位验证码')
  await run(() => changePhone(phoneForm), '手机号换绑成功')
}
async function submitPin() {
  if (!/^\d{6}$/.test(pinForm.currentPin) || !/^\d{6}$/.test(pinForm.newPin)) return toast('PIN必须是6位数字')
  if (pinForm.newPin !== pinForm.confirm) return toast('两次输入的新PIN不一致')
  await run(() => changeParentPin(pinForm.currentPin, pinForm.newPin), '家长PIN修改成功')
}
async function loadDevices() { try { devices.value = await getDevices() || [] } catch { devices.value = [] } }
async function removeDevice(device) { await run(() => revokeDevice(device.deviceId), '设备已退出'); await loadDevices() }
function confirmDeactivate() {
  uni.showModal({ title: '确认停用账号', content: '请输入账号密码和家长PIN后停用。账号数据不会被删除。', editable: true, placeholderText: '格式：账号密码,家长PIN', success: async result => {
    if (!result.confirm) return
    const [password, parentPin] = (result.content || '').split(',').map(value => value.trim())
    if (!password || !/^\d{6}$/.test(parentPin || '')) return toast('请输入“账号密码,6位家长PIN”')
    try { await deactivateAccount({ password, parentPin }); userStore.logout() } catch (error) { toast(error?.msg || error?.message || '停用失败') }
  } })
}
function exportData() {
  uni.showModal({ title: '导出账号数据', editable: true, placeholderText: '请输入6位家长PIN', success: async result => {
    if (!result.confirm) return
    try {
      const data = await exportAccountData(result.content || '')
      uni.setClipboardData({ data: JSON.stringify(data, null, 2), success: () => toast('数据已复制到剪贴板', 'success') })
    } catch (error) { toast(error?.msg || error?.message || '导出失败') }
  } })
}
async function run(action, success) { try { await action(); toast(success, 'success') } catch (error) { toast(error?.msg || error?.message || '操作失败') } }
function toast(title, icon = 'none') { uni.showToast({ title, icon }) }
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.security-page { max-width: 820px; margin: 0 auto; padding: 20px; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
.security-card { padding: 20px; border-radius: 20px; background: #fff; box-shadow: 0 8px 24px rgba(70,90,110,.08); display: flex; flex-direction: column; gap: 11px; }
.card-heading { display: flex; justify-content: space-between; }
.card-title { color: $text; font-size: 17px; font-weight: 850; }
.card-desc,.empty-text { color: $text-light; font-size: 12px; line-height: 1.5; }
.field { height: 44px; padding: 0 13px; border-radius: 13px; background: #F5F7FA; box-sizing: border-box; }
.code-row { display: flex; gap: 9px; }.code-field { flex: 1; }.minor-action { width: 124px; height: 44px; line-height: 44px; padding: 0; margin: 0; color: #315EBA; background: #EEF3FF; font-size: 12px; }
.primary-action,.danger-action { height: 44px; line-height: 44px; margin: 2px 0 0; border-radius: 13px; color: #fff; background: $primary; font-size: 14px; font-weight: 800; }
.device-row { min-height: 42px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #EEF1F5; }.device-name { color: $text; font-size: 14px; }.current-tag { margin-left: 8px; color: #168F85; font-size: 11px; }.danger-link { color: #D93025; font-size: 13px; }.refresh-link { color: #315EBA; font-size: 12px; }.danger-card { border: 1px solid #FFE1DC; }.danger-action { background: #D93025; }
@include respond-sm { .security-page { grid-template-columns: 1fr; padding: 12px; } }
</style>
