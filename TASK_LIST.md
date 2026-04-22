# 趣学星球 - 横屏版 Mobile App 任务清单

> 基于原型HTML开发，横屏布局，左侧可收起菜单导航
> 技术栈：UniApp + Vue3 + Pinia + TuniaoUI + SCSS

---

## Phase 1: 项目基础设施

- [x] **1.1** 升级为 Vue3 + Pinia，更新 manifest.json
- [x] **1.2** 创建全局样式体系 (`styles/variables.scss`, `styles/common.scss`)
  - CSS变量主题：Kids模式(珊瑚红)、学习流程(蓝色)、家长模式(青色)、VIP(暗色)
  - 统一圆角、阴影、间距、字体规范
- [x] **1.3** 创建核心布局组件 (`components/AppLayout.vue`)
  - 左侧 Sidebar (220px 可收起至 60px) + 右侧内容区
  - Sidebar：Logo区 + 导航项 + 用户信息区
  - 顶部栏 (64px)
- [x] **1.4** 创建 API 请求层 (`api/request.js`) — uni.request封装、Token注入、错误处理
- [x] **1.5** 创建 API 模块 — auth.js / learn.js / pet.js / achievement.js / user.js
- [x] **1.6** 创建 Pinia Store — user.js / pet.js / learn.js
- [x] **1.7** 更新 pages.json — 注册所有页面路由（无tabBar，纯自定义导航）
- [x] **1.8** 更新 App.vue — 全局样式引入、启动逻辑
- [x] **1.9** 安装配置 TuniaoUI (`@tuniao/tnui-vue3-uniapp`) — easycom配置 + 样式引入

---

## Phase 2: 登录注册

- [x] **2.1** 登录页 (`pages/login/index.vue`)
  - 居中卡片布局（无Sidebar）
  - 左侧品牌区（渐变背景 + Logo + 特性介绍）
  - 右侧：年龄段选择 → 登录/注册表单切换
- [x] **2.2** 注册页 (`pages/login/register.vue`)

---

## Phase 3: 首页 & 主页面架构

- [x] **3.1** 主页面容器 (`pages/main/index.vue`)
  - 混合导航架构：组件切换(v-if) + 子页面导航(uni.navigateTo)
  - 侧边栏 + 顶部栏 + 内容区域
  - 主题切换（Kids/Learn/Parent）
- [x] **3.2** 首页内容组件 (`components/home/HomeContent.vue`)
  - 任务横幅（渐变 + 进度条）
  - 排名横幅（黄色渐变）
  - 学科网格（3列卡片）
  - 快捷入口（宠物 + 成就）
  - 排行榜速览卡片
  - inject('switchTab') 通信机制

---

## Phase 4: 学习中心（蓝色主题）

- [x] **4.1** 学习内容组件 (`components/learn/LearnContent.vue`)
  - 年龄组卡片（3列）+ 学科列表 + 进度条 + 最近学习
- [x] **4.2** 课程列表页 (`pages/learn/courses.vue`)
  - 课程卡片列表（星级评分 + 进度 + VIP标识）
- [x] **4.3** 关卡列表页 (`pages/learn/levels.vue`)
  - 单元分组 + 5列关卡网格（完成/当前/锁定/Boss状态）
- [x] **4.4** 答题页 (`pages/learn/quiz.vue`)
  - 开始屏 → 答题屏(倒计时+2x2选项) → 结果屏(星级+奖励)
  - 答对/答错反馈动画
- [x] **4.5** 结果页 (`pages/learn/result.vue`)

---

## Phase 5: 宠物系统

- [x] **5.1** 宠物内容组件 (`components/pet/PetContent.vue`)
  - 双列布局：宠物展示面板(左) + 背包/装饰(右)
  - 宠物状态（饱食度/心情/精力）
  - 操作按钮（喂食/洗澡/玩耍/换装）
  - 喂食弹窗 (tn-popup)
- [x] **5.2** 换装页 (`pages/pet/dress.vue`)
- [x] **5.3** 商店页 (`pages/pet/shop.vue`)

---

## Phase 6: 成就系统

- [x] **6.1** 成就内容组件 (`components/achievement/AchievementContent.vue`)
  - 成就统计摘要 + Tab筛选 + 2列成就卡片
  - 稀有度区分（金/银/铜/传说）+ 进度条
- [x] **6.2** 贴纸册页 (`pages/achievement/sticker.vue`)

---

## Phase 7: 社交功能

- [x] **7.1** 排行榜内容组件 (`components/ranking/RankingContent.vue`)
  - 领奖台 + Tab切换(好友/班级/全国) + 排名列表
- [x] **7.2** 挑战赛页 (`pages/challenge/index.vue`)
  - 赛季横幅 + 挑战卡片(2列) + 段位卡片 + 对战历史

---

## Phase 8: 家长中心（青色主题）

- [x] **8.1** 家长验证页 (`pages/parent/gate.vue`)
  - 全屏居中模态（密码/短信验证）
- [x] **8.2** 家长内容组件 (`components/parent/ParentContent.vue`)
  - 孩子信息卡 + 学习报告 + 时间控制 + 使用记录
- [x] **8.3** 学习报告页 (`pages/parent/report.vue`)
- [x] **8.4** 时间管理页 (`pages/parent/time-control.vue`)
- [x] **8.5** 家庭组管理页 (`pages/parent/family.vue`)

---

## Phase 9: 个人中心

- [x] **9.1** 个人中心页 (`pages/mine/index.vue`)
  - 用户信息 + 统计概览 + 功能菜单
- [x] **9.2** 学习记录页 (`pages/mine/records.vue`)
- [x] **9.3** 错题本页 (`pages/mine/wrong.vue`)
- [x] **9.4** VIP会员页 (`pages/mine/vip.vue`) — 暗色主题独立布局
- [x] **9.5** 设置页 (`pages/mine/settings.vue`)

---

## Phase 10: 后续优化

- [x] **10.1** 全面使用 TuniaoUI 组件替换自定义 CSS
  - 进度条: 替换为 tn-line-progress（HomeContent/LearnContent/PetContent/AchievementContent/quiz）
  - 按钮: 替换为 tn-button（login/quiz/parent-gate）
  - Tab 切换: 替换为 tn-tabs（parent-gate 验证方式切换）
- [x] **10.2** API 接口对接 — 所有内容组件已接入真实 API（Promise.allSettled + mock fallback）
  - HomeContent: getDailyTasks + getSubjects + getPetStatus + getMyRank + getMyProgress
  - LearnContent: getSubjects + getRecords + getCourses
  - PetContent: getPetStatus + getInventory + feedPet + playPet
  - RankingContent: getRanking + getMyRank（支持 Tab 切换刷新）
  - AchievementContent: getAchievements + getMyProgress + receiveReward（支持 Tab 切换刷新）
  - ParentContent: getReport + getTimeControl + getUsageLog
  - 新增 API 模块: api/ranking.js, api/parent.js
- [x] **10.3** 动画效果完善 — 加载状态(tn-loading)、交错动画(stagger-list)、骨架屏(shimmer)、新动画(fadeIn/bounceIn/slideInLeft/slideInRight)
- [x] **10.4** 响应式适配 — 侧边栏自适应、网格布局折叠、媒体查询断点(sm/md/lg)
- [x] **10.5** 性能优化 — scroll-view 增强(enhanced/bounces)、组件按需加载(loadedTabs)、v-show 缓存组件实例

---

## 完成统计

| 类别 | 总任务 | 已完成 | 进度 |
|------|--------|--------|------|
| 基础设施 (Phase 1) | 9 | 9 | 100% |
| 登录注册 (Phase 2) | 2 | 2 | 100% |
| 首页架构 (Phase 3) | 2 | 2 | 100% |
| 学习中心 (Phase 4) | 5 | 5 | 100% |
| 宠物系统 (Phase 5) | 3 | 3 | 100% |
| 成就系统 (Phase 6) | 2 | 2 | 100% |
| 社交功能 (Phase 7) | 2 | 2 | 100% |
| 家长中心 (Phase 8) | 5 | 5 | 100% |
| 个人中心 (Phase 9) | 5 | 5 | 100% |
| 后续优化 (Phase 10) | 5 | 5 | 100% |
| **总计** | **40** | **40** | **100%** |
