const KEY = 'kidslearn_settings'

export function readRuntimeSettings() {
  try {
    const raw = uni.getStorageSync(KEY)
    return typeof raw === 'string' ? JSON.parse(raw) : (raw || {})
  } catch { return {} }
}

export function applyRuntimeSettings(settings = readRuntimeSettings()) {
  if (typeof document === 'undefined') return
  const themes = {
    coral: ['#FF7A59', '#F8FBFF'],
    blue: ['#4A90D9', '#F0F7FF'],
    teal: ['#26A69A', '#F1FBF9'],
    purple: ['#9B59B6', '#F7F1FA']
  }
  const [primary, background] = themes[settings.theme] || themes.coral
  document.documentElement.style.setProperty('--color-primary', primary)
  document.documentElement.style.setProperty('--color-bg', background)
  const reduced = typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches
  document.documentElement.classList.toggle('reduce-motion', reduced)
}
