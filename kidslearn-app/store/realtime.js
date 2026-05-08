import { defineStore } from 'pinia'
import { ref } from 'vue'
import { BASE_URL } from '@/api/request'
import { usePetStore } from '@/store/pet'
import { useUserStore } from '@/store/user'
import {
  buildRealtimeUrl,
  parseRealtimeMessage,
  REALTIME_MESSAGE_TYPES,
  reduceBalanceMessage,
  shouldReconnect
} from '@/utils/realtime.mjs'
import {
  normalizeMonitorSnapshot,
  reduceMonitorEvent
} from '@/utils/parentMonitor.mjs'

export const useRealtimeStore = defineStore('realtime', () => {
  const connected = ref(false)
  const connecting = ref(false)
  const lastMessageAt = ref('')
  const parentMonitor = ref(normalizeMonitorSnapshot())
  const socketTask = ref(null)
  const reconnectTimer = ref(null)
  const closedByUser = ref(false)

  function connect() {
    const token = uni.getStorageSync('token')
    if (!token || connected.value || connecting.value) {
      return
    }

    clearReconnectTimer()
    closedByUser.value = false
    connecting.value = true

    const task = uni.connectSocket({
      url: buildRealtimeUrl(BASE_URL, token),
      complete: () => {}
    })

    socketTask.value = task

    task.onOpen(() => {
      connected.value = true
      connecting.value = false
      send({ type: 'PING' })
    })

    task.onMessage((res) => {
      handleMessage(res.data)
    })

    task.onError(() => {
      connected.value = false
      connecting.value = false
    })

    task.onClose(() => {
      connected.value = false
      connecting.value = false
      socketTask.value = null
      scheduleReconnect()
    })
  }

  function close() {
    closedByUser.value = true
    clearReconnectTimer()
    if (socketTask.value) {
      socketTask.value.close({ code: 1000, reason: 'client close' })
      socketTask.value = null
    }
    connected.value = false
    connecting.value = false
  }

  function send(data) {
    if (!socketTask.value || !connected.value) {
      return
    }
    socketTask.value.send({
      data: typeof data === 'string' ? data : JSON.stringify(data)
    })
  }

  function handleMessage(rawData) {
    const message = parseRealtimeMessage(rawData)
    if (!message) {
      return
    }
    lastMessageAt.value = message.timestamp || new Date().toISOString()

    if (message.type === REALTIME_MESSAGE_TYPES.PET_STATUS_UPDATE) {
      usePetStore().setPetInfo(message.payload)
      return
    }

    if (message.type === REALTIME_MESSAGE_TYPES.USER_BALANCE_UPDATE) {
      const userStore = useUserStore()
      const next = reduceBalanceMessage(userStore.userInfo, message.payload)
      if (next) {
        userStore.setUserInfo(next)
      }
      return
    }

    if (
      message.type === REALTIME_MESSAGE_TYPES.CHILD_ACTIVITY_UPDATE ||
      message.type === REALTIME_MESSAGE_TYPES.PARENT_MONITOR_UPDATE
    ) {
      parentMonitor.value = reduceMonitorEvent(parentMonitor.value, message)
    }
  }

  function setParentMonitor(snapshot) {
    parentMonitor.value = normalizeMonitorSnapshot(snapshot)
  }

  function scheduleReconnect() {
    const token = uni.getStorageSync('token')
    if (!shouldReconnect({ closedByUser: closedByUser.value, token })) {
      return
    }
    clearReconnectTimer()
    reconnectTimer.value = setTimeout(() => {
      reconnectTimer.value = null
      connect()
    }, 3000)
  }

  function clearReconnectTimer() {
    if (reconnectTimer.value) {
      clearTimeout(reconnectTimer.value)
      reconnectTimer.value = null
    }
  }

  return {
    connected,
    connecting,
    lastMessageAt,
    parentMonitor,
    connect,
    close,
    send,
    handleMessage,
    setParentMonitor
  }
})
