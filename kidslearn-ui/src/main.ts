import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import { initTokenSync } from '@/utils/request'
import './styles/index.css'

const app = createApp(App)

// 全局错误处理
app.config.errorHandler = (err, instance, info) => {
  console.error('全局错误:', err, info)
  // 可以在这里上报错误到监控系统
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 启动 token 无感续期：注册跨标签页同步监听，并在已登录时启动主动刷新定时器。
// 必须在 pinia 装载之后、app.mount 之前调用（依赖 useUserStore）。
initTokenSync()

app.mount('#app')
