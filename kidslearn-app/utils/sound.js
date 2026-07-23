/**
 * 趣学星球 - 音效管理器 (Sound Manager)
 * 负责全局音效的加载与播放
 */

class SoundManager {
  constructor() {
    this.contexts = {}
    this.enabled = true
    
    // 预定义音效路径
    this.sounds = {
      tap: '/static/audio/click.mp3',
      success: '/static/audio/correct.mp3',
      fail: '/static/audio/wrong.mp3',
      reward: '/static/audio/coin.mp3',
      popup: '/static/audio/popup.mp3',
      levelUp: '/static/audio/level_up.mp3'
    }
  }

  /**
   * 播放指定音效
   * @param {string} name 音效名称 (tap, success, fail, etc.)
   */
  play(name) {
    let enabled = this.enabled
    try {
      const raw = uni.getStorageSync('kidslearn_settings')
      const settings = typeof raw === 'string' ? JSON.parse(raw) : raw
      if (settings && settings.soundEnabled === false) enabled = false
    } catch {}
    if (!enabled || !this.sounds[name]) return

    try {
      // 在 UniApp 中使用 InnerAudioContext
      let ctx = this.contexts[name]
      if (!ctx) {
        ctx = uni.createInnerAudioContext()
        ctx.src = this.sounds[name]
        this.contexts[name] = ctx
      }
      
      // 重置到开头并播放
      ctx.stop()
      ctx.play()
    } catch (e) {
      console.warn(`SoundManager: 播放音效 [${name}] 失败`, e)
    }
  }

  /**
   * 开启/关闭音效
   */
  setEnabled(enabled) {
    this.enabled = enabled
  }

  /**
   * 销毁所有音频实例
   */
  destroy() {
    Object.values(this.contexts).forEach(ctx => ctx.destroy())
    this.contexts = {}
  }
}

export const soundManager = new SoundManager()
export default soundManager
