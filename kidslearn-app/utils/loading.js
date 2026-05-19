import { reactive } from 'vue'

export const loadingState = reactive({
  show: false,
  title: '加载中',
  mascot: '🌍'
})

export const showLoading = (title = '加载中', mascot = '🌍') => {
  loadingState.show = true
  loadingState.title = title
  loadingState.mascot = mascot
}

export const hideLoading = () => {
  loadingState.show = false
}
