# 趣学星球 - 横屏版 Mobile App 任务清单

> 基于原型HTML开发，横屏布局，左侧可收起菜单导航

---

## Phase 1: 项目基础设施

- [ ] **1.1** 升级为 Vue3 + Pinia，更新 manifest.json
- [ ] **1.2** 创建全局样式体系 (`styles/variables.scss`, `styles/common.scss`)
  - CSS变量主题：Kids模式(珊瑚红)、学习流程(蓝色)、家长模式(青色)、VIP(暗色)
  - 统一圆角、阴影、间距、字体规范
- [ ] **1.3** 创建核心布局组件 (`components/AppLayout.vue`)
  - 左侧 Sidebar (220px 可收起至 60px) + 右侧内容区
  - Sidebar：Logo区 + 导航项 + 用户信息区
  - 顶部栏 (64px)
- [ ] **1.4** 创建 API 请求层 (`api/request.js`) — uni.request封装、Token注入、错误处理
- [ ] **1.5** 创建 API 模块 — auth.js / learn.js / pet.js / achievement.js / user.js
- [ ] **1.6** 创建 Pinia Store — user.js / pet.js / learn.js
- [ ] **1.7** 更新 pages.json — 注册所有页面路由（无tabBar，纯自定义导航）
- [ ] **1.8** 更新 App.vue — 全局样式引入、启动逻辑

---

## Phase 2: 登录注册

- [ ] **2.1** 登录页 (`pages/login/index.vue`)
  - 居中卡片布局（无Sidebar）
  - 左侧品牌区（渐变背景 + Logo + 特性介绍）
  - 右侧：年龄段选择 → 登录/注册表单切换
- [ ] **2.2** 注册页 (`pages/login/register.vue`)

---

## Phase 3: 首页

- [ ] **3.1** 首页 (`pages/index/index.vue`)
  - 任务横幅（渐变 + 进度条）
  - 排名横幅（黄色渐变）
  - 学科网格（3列卡片）
  - 快捷入口（宠物 + 成就）
  - 排行榜速览卡片

---

## Phase 4: 学习中心（蓝色主题）

- [ ] **4.1** 年级选择页 (`pages/learn/index.vue`)
  - 年龄组卡片（3列）+ 学科列表 + 进度条
- [ ] **4.2** 课程列表页 (`pages/learn/courses.vue`)
  - 课程卡片列表（星级评分 + 进度 + VIP标识）
- [ ] **4.3** 关卡列表页 (`pages/learn/levels.vue`)
  - 单元分组 + 5列关卡网格（完成/当前/锁定/Boss状态）
- [ ] **4.4** 答题页 (`pages/learn/quiz.vue`)
  - 开始屏 → 答题屏(倒计时+2x2选项) → 结果屏(星级+奖励)
  - 答对/答错反馈动画
- [ ] **4.5** 结果页 (`pages/learn/result.vue`)

---

## Phase 5: 宠物系统

- [ ] **5.1** 宠物主页 (`pages/pet/index.vue`)
  - 双列布局：宠物展示面板(左) + 背包/装饰(右)
  - 宠物状态（饱食度/心情/精力）
  - 操作按钮（喂食/洗澡/玩耍/换装）
- [ ] **5.2** 换装页 (`pages/pet/dress.vue`)
- [ ] **5.3** 商店页 (`pages/pet/shop.vue`)

---

## Phase 6: 成就系统

- [ ] **6.1** 成就列表页 (`pages/achievement/index.vue`)
  - 成就统计摘要 + Tab筛选 + 2列成就卡片
  - 稀有度区分（金/银/铜/传说）+ 进度条
- [ ] **6.2** 贴纸册页 (`pages/achievement/sticker.vue`)

---

## Phase 7: 社交功能

- [ ] **7.1** 排行榜页 (`pages/ranking/index.vue`)
  - 领奖台 + Tab切换(好友/班级/全国) + 排名列表
- [ ] **7.2** 挑战赛页 (`pages/challenge/index.vue`)
  - 赛季横幅 + 挑战卡片(2列) + 段位卡片 + 对战历史

---

## Phase 8: 家长中心（青色主题）

- [ ] **8.1** 家长验证页 (`pages/parent/gate.vue`)
  - 全屏居中模态（密码/短信验证）
- [ ] **8.2** 家长中心首页 (`pages/parent/index.vue`)
  - 孩子信息卡 + 学习报告 + 时间控制 + 使用记录
- [ ] **8.3** 学习报告页 (`pages/parent/report.vue`)
- [ ] **8.4** 时间管理页 (`pages/parent/time-control.vue`)
- [ ] **8.5** 家庭组管理页 (`pages/parent/family.vue`)

---

## Phase 9: 个人中心

- [ ] **9.1** 个人中心页 (`pages/mine/index.vue`)
  - 用户信息 + 统计概览 + 功能菜单
- [ ] **9.2** 学习记录页 (`pages/mine/records.vue`)
- [ ] **9.3** 错题本页 (`pages/mine/wrong.vue`)
- [ ] **9.4** VIP会员页 (`pages/mine/vip.vue`) — 暗色主题
- [ ] **9.5** 设置页 (`pages/mine/settings.vue`)

---

## 完成统计

| 类别 | 总任务 | 已完成 | 进度 |
|------|--------|--------|------|
| 基础设施 (Phase 1) | 8 | 0 | 0% |
| 登录注册 (Phase 2) | 2 | 0 | 0% |
| 首页 (Phase 3) | 1 | 0 | 0% |
| 学习中心 (Phase 4) | 5 | 0 | 0% |
| 宠物系统 (Phase 5) | 3 | 0 | 0% |
| 成就系统 (Phase 6) | 2 | 0 | 0% |
| 社交功能 (Phase 7) | 2 | 0 | 0% |
| 家长中心 (Phase 8) | 5 | 0 | 0% |
| 个人中心 (Phase 9) | 5 | 0 | 0% |
| **总计** | **33** | **0** | **0%** |
