# 趣学星球 (KidsLearn Planet)

儿童游戏化学习平台，通过答题闯关、宠物养成、成就系统等游戏化机制激发学习兴趣。横屏设计，面向平板设备。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端（App） | UniApp + Vue 3 + 图鸟 UI (tnui-vue3-uniapp) |
| 前端（管理后台） | Vue 3 + Vite + TypeScript |
| 后端 | Spring Boot 3.2.5 (Java 17) + MyBatis-Plus 3.5.6 |
| 数据库 | MySQL 8 + Redis |
| 认证 | JWT (jjwt 0.12.5) + 无感Token刷新 |
| 语音合成 | Moss TTS |
| AI 服务 | DeepSeek API（可配置多提供商） |
| 实时通信 | WebSocket (/ws/realtime) |
| API 文档 | Knife4j (OpenAPI 3) |

## 项目结构

```
kids-learn/
├── kidslearn-app/            # UniApp 移动端（Vue 3，横屏设计）
│   ├── pages/                # 30 个页面
│   ├── components/           # 15 个组件（含通用动画、签到、奖励弹窗）
│   ├── api/                  # 9 个 API 模块（统一 request 封装）
│   ├── store/                # 4 个 Store（user, learn, pet, realtime）
│   ├── utils/                # 17 个工具模块（语音、富内容、离线缓存等）
│   └── styles/               # SCSS 样式体系（tokens, variables, mixins）
├── kidslearn-ui/             # 管理后台（Vue 3 + Vite）
├── kidslearn-server/         # 后端服务（Spring Boot 多模块）
│   ├── kidslearn-api/        # 业务 API（10 Controller, 8 Service, 51 Entity）
│   ├── kidslearn-common/     # 公共模块（JWT, 异常处理, Redis 配置）
│   └── kidslearn-generator/  # 代码生成器
└── docs/                     # 设计文档
```

## 功能模块

| 模块 | 说明 | 页面数 |
|------|------|--------|
| 用户系统 | 注册/登录、角色权限（儿童/家长）、新手引导 | 3 |
| 学习引擎 | 多学科题库、答题闯关、自适应难度、分班测试、视频课程 | 8 |
| 错题本 | 自动归集、重做练习、AI 逐题解析 | 1 |
| 宠物系统 | 虚拟养成、喂食互动、进化、换装、商店 | 3 |
| 成就系统 | 徽章收集、里程碑达成、贴纸册 | 2 |
| 挑战模式 | 限时答题挑战赛、结算引擎 | 1 |
| 排行榜 | 全站/好友排名 | 1 |
| 家长中心 | 验证门、学习报告、实时监控、时间管控、家庭组 | 6 |
| 个人中心 | 学习记录、错题本、VIP、设置 | 5 |
| 每日签到 | 连续签到奖励弹窗 | — |
| 订阅/订单 | 会员订阅与支付 | — |

## AI 集成

通过 `AiService` 实现可配置的多提供商 AI 服务：

- **配置方式**: 数据库 `app_config` 表动态配置（提供商、API Key、模型、超时）
- **接口标准**: OpenAI 兼容 (`/v1/chat/completions`)
- **已实现**: 错题 AI 解析（`GET /learn/explain-wrong`）
- **待扩展**: AI 出题、AI 对话辅导、AI 题目生成

## API 概览

后端 API 基础路径：`/api/v1`

| 控制器 | 职责 |
|--------|------|
| AuthController | 登录/注册/Token 刷新 |
| LearnController | 答题学习、错题管理、AI 解析、自适应出题、分班测试 |
| PetController | 宠物养成、互动、换装、商店 |
| ChallengeController | 挑战赛创建/提交/结算 |
| AchievementController | 成就解锁/查询 |
| LeaderboardController | 排行榜数据 |
| ParentController | 家长端学习报告、统计 |
| UserController | 用户信息、孩子档案、新手引导 |
| SubscriptionController | 订阅会员 |
| OrderController | 订单支付 |

### 核心引擎

| 引擎 | 说明 |
|------|------|
| AchievementRuleEngine | 成就规则判断 |
| PetEvolutionEngine | 宠物进化逻辑 |
| ChallengeResultEngine | 挑战赛结算 |
| QuestionRandomizer | 题目随机化 |
| QuestionAudioService | 题目语音生成 (Moss TTS) |
| RankTierCatalog | 段位体系 |
| SubscriptionActivationEngine | 订阅激活 |

## 快速开始

### 环境要求

- Node.js >= 16
- Java 17
- MySQL 8.0
- Redis
- Maven 3.8+
- HBuilderX（前端开发）

### 后端启动

```bash
cd kidslearn-server
# 导入 kidslearn-server/sql/ 下的数据库脚本
# 修改 kidslearn-api/src/main/resources/application.yml 中的数据库和 Redis 配置
mvn spring-boot:run -pl kidslearn-api
```

### 前端启动（App）

```bash
cd kidslearn-app
npm install
# 使用 HBuilderX 打开项目，运行到微信小程序/APP
```

### 管理后台启动

```bash
cd kidslearn-ui
npm install
npm run dev
```

## 数据库

MySQL 数据库，使用 MyBatis-Plus 自动建表。SQL 脚本位于 `kidslearn-server/sql/` 目录。

## 代码规模

| 部分 | 规模 |
|------|------|
| 后端 Java（Controller + Service + Engine） | ~5,100 行 |
| 前端 Vue 页面 + 组件 | ~13,400 行 |
| 前端 JS/工具 | ~1,600 行 |
| 数据实体 / Mapper | 51 个 |
| 页面 | 30 个 |
| 组件 | 15 个 |

## License

Private — All rights reserved.
