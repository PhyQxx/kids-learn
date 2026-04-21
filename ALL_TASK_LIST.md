# 趣学星球 - 全部任务清单

> 按实现优先级排列，完成后标记 `[x]`

---

## Phase 1: Server端基础设施

- [x] **1.1** 创建SQL Schema脚本 (`sql/schema.sql`) — 45张表的建表语句
- [x] **1.2** 创建初始数据脚本 (`sql/init-data.sql`) — 学科、年级、默认宠物、管理员账号等
- [x] **1.3** 创建通用分页请求DTO (`dto/common/PageQueryDTO.java`)
- [x] **1.4** 创建认证相关DTO (`dto/auth/LoginDTO.java`, `RegisterDTO.java`, `TokenVO.java`)
- [x] **1.5** 创建用户相关DTO (`dto/user/UserVO.java`, `UpdateUserDTO.java`, `UpdateChildProfileDTO.java`)
- [x] **1.6** 创建学习相关DTO (`dto/learn/SubmitAnswerDTO.java`, `DailyTaskVO.java`, `LevelResultVO.java`)
- [x] **1.7** 创建宠物相关DTO — 复用Entity，无需额外DTO
- [x] **1.8** 创建成就相关DTO — 复用Entity，无需额外DTO
- [x] **1.9** 创建社交相关DTO — 复用Entity，无需额外DTO
- [x] **1.10** 创建家长中心DTO — 使用Map返回，无需额外DTO
- [x] **1.11** 创建管理后台DTO — 使用Map接收参数，无需额外DTO

---

## Phase 2: Server端用户系统 (P0)

- [x] **2.1** AuthService接口 + 实现类 — 登录校验(BCrypt)、JWT生成、Redis存储Token、刷新Token
- [x] **2.2** AuthController — 登录/注册/刷新Token/登出接口 (`/api/v1/auth/*`)
- [x] **2.3** UserController — 个人信息查询/更新 (`/api/v1/user/*`)

---

## Phase 3: Server端学习引擎 (P0)

- [x] **3.1** LearnService — 每日任务生成、学科/课程/关卡查询
- [x] **3.2** LearnController — 学习相关接口 (`/api/v1/learn/*`)
  - GET /daily-tasks, /subjects, /courses, /levels, /questions
  - POST /submit-answer, /complete-level
  - GET /records (学习记录), /wrong-topics (错题本)
- [x] **3.3** 答题逻辑 — 提交答案、验证、计分、星级评定(解析starThresholds)
- [x] **3.4** 奖励发放逻辑 — 金币/经验/贴纸奖励、自动解锁下一关
- [x] **3.5** 错题自动记录 — wrong_topic 表写入
- [x] **3.6** DailyStats 每日统计自动更新

---

## Phase 4: Server端宠物系统 (P1)

- [x] **4.1** PetService接口 + 实现类 — 宠物状态查询、喂食(消耗道具)、玩耍、换装
- [x] **4.2** PetController — 宠物相关接口 (`/api/v1/pet/*`)
  - GET /status, /shop, /inventory
  - POST /feed, /play, /dress, /shop/buy

---

## Phase 5: Server端成就系统 (P1)

- [x] **5.1** AchievementService接口 + 实现类 — 成就列表、进度查询、分级显示
- [x] **5.2** AchievementController — 成就接口 (`/api/v1/achievement/*`)
  - GET /list, /my-progress, /stickers, /titles
  - POST /receive-reward, /activate-title

---

## Phase 6: Server端排行榜与挑战 (P2)

- [x] **6.1** LeaderboardController — 排行榜接口 (`/api/v1/leaderboard/{type}`)
- [x] **6.2** ChallengeController — 挑战赛接口 (`/api/v1/challenge/*`)
  - POST /create — 创建挑战
  - GET /records — 对战记录

---

## Phase 7: Server端家长中心 (P1)

- [x] **7.1** ParentController — 家长中心接口 (`/api/v1/parent/*`)
  - GET /report — 学习报告(月度统计)
  - GET /time-control — 获取时间控制设置
  - PUT /time-control — 保存时间控制设置
  - GET /family — 家庭组信息

---

## Phase 8: Server端付费系统 (P2)

- [ ] **8.1** SubscriptionService + Controller — 会员套餐查询、订阅
- [ ] **8.2** OrderService + Controller — 订单创建、支付回调

---

## Phase 9: Server端管理后台API (P0)

- [x] **9.1** AdminAuthController — 管理员登录 (`/admin/api/v1/auth/*`)
- [x] **9.2** AdminContentController — 学科/课程/关卡/题目CRUD
- [x] **9.3** AdminGameController — 宠物/道具/装饰/成就/贴纸/称号CRUD
- [x] **9.4** AdminSystemController — Dashboard统计、用户管理、角色管理、系统配置、操作日志

---

## Phase 10: Admin UI 管理后台

- [x] **10.1** 创建API服务层 — `src/api/request.ts` (统一API封装)
- [x] **10.2** 修改登录页 — 真实API (`src/views/login/index.vue`)
- [x] **10.3** 修改Dashboard — 真实统计API (`src/views/dashboard/index.vue`)
- [x] **10.4** 完善学科管理 — CRUD (`src/views/content/subject.vue`)
- [x] **10.5** 完善课程管理 — CRUD (`src/views/content/course.vue`)
- [x] **10.6** 完善关卡管理 — CRUD (`src/views/content/level.vue`)
- [x] **10.7** 完善题目管理 — CRUD + 动态选项编辑器 (`src/views/content/question.vue`)
- [x] **10.8** 完善宠物管理 — CRUD + 进化配置 (`src/views/pet/list.vue`)
- [x] **10.9** 完善道具管理 — CRUD (`src/views/pet/item.vue`)
- [x] **10.10** 完善装饰管理 — CRUD (`src/views/pet/decoration.vue`)
- [x] **10.11** 完善成就管理 — CRUD (`src/views/achievement/list.vue`)
- [x] **10.12** 完善贴纸管理 — CRUD (`src/views/achievement/sticker.vue`)
- [x] **10.13** 完善称号管理 — CRUD (`src/views/achievement/title.vue`)
- [x] **10.14** 完善用户管理 — 列表 + 搜索 + 状态切换 (`src/views/system/user.vue`)
- [x] **10.15** 完善角色管理 — CRUD (`src/views/system/role.vue`)
- [x] **10.16** 完善系统配置 — 编辑 (`src/views/system/config.vue`)
- [x] **10.17** 完善操作日志 — 只读列表 (`src/views/system/log.vue`)

---

## Phase 11: Mobile App (UniApp)

### 基础设施
- [x] **11.1** 创建API请求封装 (`api/request.js`) — uni.request封装、Token注入、错误处理
- [x] **11.2** 创建API模块 — `api/auth.js` (登录注册)
- [x] **11.3** 创建API模块 — `api/learn.js` (学习相关)
- [x] **11.4** 创建API模块 — `api/pet.js` (宠物相关)
- [x] **11.5** 创建API模块 — `api/achievement.js` (成就相关)
- [x] **11.6** 创建API模块 — `api/user.js` (用户相关)
- [x] **11.7** 创建Pinia Store — `store/user.js`, `store/pet.js`, `store/learn.js`
- [x] **11.8** 创建Tab Bar图标资源 (`static/tabbar/*.png`) — 10个图标
- [x] **11.9** 更新 pages.json — 注册25个页面路由

### 登录注册
- [x] **11.10** 完善登录页 — 年龄段选择 + 注册/登录表单 + API对接 (`pages/login/index.vue`)
- [x] **11.11** 新增注册页 (`pages/login/register.vue`)

### 首页
- [x] **11.12** 完善首页 — 对接API获取用户信息、宠物状态、今日任务 (`pages/index/index.vue`)

### 学习中心
- [x] **11.13** 完善学习中心页 — 对接学科列表API (`pages/learn/index.vue`)
- [x] **11.14** 新增课程列表页 (`pages/learn/courses.vue`)
- [x] **11.15** 新增关卡列表页 (`pages/learn/levels.vue`)
- [x] **11.16** 新增答题页 (`pages/learn/quiz.vue`)
- [x] **11.17** 新增结果页 (`pages/learn/result.vue`)

### 宠物
- [x] **11.18** 完善宠物页 — 状态查询、喂食、玩耍、背包道具 (`pages/pet/index.vue`)
- [x] **11.19** 新增换装页 (`pages/pet/dress.vue`)
- [x] **11.20** 新增商店页 (`pages/pet/shop.vue`)

### 成就
- [x] **11.21** 完善成就页 — 成就列表、进度条、领取奖励、Tab筛选 (`pages/achievement/index.vue`)
- [x] **11.22** 新增贴纸册页 (`pages/achievement/sticker.vue`)

### 个人中心
- [x] **11.23** 完善个人中心 — 用户信息、统计、菜单导航 (`pages/mine/index.vue`)
- [x] **11.24** 新增学习记录页 (`pages/mine/records.vue`)
- [x] **11.25** 新增错题本页 (`pages/mine/wrong.vue`)
- [x] **11.26** 新增VIP页 (`pages/mine/vip.vue`)
- [x] **11.27** 新增设置页 — 昵称修改、称号管理、退出登录 (`pages/mine/settings.vue`)

### 家长中心
- [x] **11.28** 新增家长验证页 (`pages/parent/gate.vue`) — 算术题验证
- [x] **11.29** 完善家长中心首页 — 功能导航 + 描述 (`pages/parent/index.vue`)
- [x] **11.30** 新增学习报告页 (`pages/parent/report.vue`)
- [x] **11.31** 新增时间管理页 (`pages/parent/time-control.vue`)
- [x] **11.32** 新增家庭组管理页 (`pages/parent/family.vue`)

### 社交功能
- [x] **11.33** 新增排行榜页 (`pages/ranking/index.vue`)
- [x] **11.34** 新增挑战赛页 (`pages/challenge/index.vue`)

---

## 完成统计

| 类别 | 总任务 | 已完成 | 进度 |
|------|--------|--------|------|
| Server基础设施 (Phase 1) | 11 | 11 | 100% |
| Server用户系统 (Phase 2) | 3 | 3 | 100% |
| Server学习引擎 (Phase 3) | 6 | 6 | 100% |
| Server宠物系统 (Phase 4) | 2 | 2 | 100% |
| Server成就系统 (Phase 5) | 2 | 2 | 100% |
| Server排行榜/挑战 (Phase 6) | 2 | 2 | 100% |
| Server家长中心 (Phase 7) | 1 | 1 | 100% |
| Server付费系统 (Phase 8) | 2 | 0 | 0% |
| Server管理后台API (Phase 9) | 4 | 4 | 100% |
| Admin UI管理后台 (Phase 10) | 17 | 17 | 100% |
| Mobile App (Phase 11) | 34 | 34 | 100% |
| **总计** | **84** | **82** | **97.6%** |

> 剩余未完成: Phase 8 付费系统 (P2优先级，可后续迭代)
