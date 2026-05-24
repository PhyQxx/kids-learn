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
│   ├── pages/                # App 页面
│   ├── components/           # 通用组件（含通用动画、签到、奖励弹窗）
│   ├── api/                  # API 模块（统一 request 封装）
│   ├── store/                # Store（user, learn, pet, realtime）
│   ├── utils/                # 17 个工具模块（语音、富内容、离线缓存等）
│   └── styles/               # SCSS 样式体系（tokens, variables, mixins）
├── kidslearn-ui/             # 管理后台（Vue 3 + Vite）
├── kidslearn-server/         # 后端服务（Spring Boot 多模块）
│   ├── kidslearn-api/        # 业务 API
│   ├── kidslearn-common/     # 公共模块（JWT, 异常处理, Redis 配置）
│   └── kidslearn-generator/  # 代码生成器
└── docs/                     # 设计文档
```

## 功能模块

| 模块 | 说明 | 页面数 |
|------|------|--------|
| 用户系统 | 注册/登录、单账号学习档案、新手引导 | 3 |
| 学习引擎 | 多学科题库、答题闯关、自适应难度、分班测试、视频课程 | 8 |
| 错题本 | 自动归集、重做练习、AI 逐题解析 | 1 |
| 宠物系统 | 虚拟养成、喂食互动、进化、换装、商店 | 3 |
| 成就系统 | 徽章收集、里程碑达成、贴纸册 | 2 |
| 挑战模式 | 限时答题挑战赛、结算引擎 | 1 |
| 排行榜 | 全站/好友排名 | 1 |
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
| UserController | 用户信息、学习档案、新手引导 |
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

```powershell
cd kidslearn-server
# Windows PowerShell 示例
$env:JAVA_HOME="C:\Users\61759\.jdks\ms-17.0.18"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:SPRING_PROFILES_ACTIVE="dev"
$env:DB_URL="jdbc:mysql://127.0.0.1:3306/kids_learn?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="123456"
mvn.cmd -pl kidslearn-api -am spring-boot:run
```

生产环境使用 `prod` profile，并通过环境变量提供真实配置：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`REDIS_HOST`、`JWT_SECRET`。不要把生产数据库密码或 JWT 密钥写入仓库配置文件。

### 前端启动（App）

```bash
cd kidslearn-app
npm install
npm run test
# 使用 HBuilderX 打开项目，运行到微信小程序/APP
```

### 管理后台启动

```bash
cd kidslearn-ui
npm install
npm run test
npm run build
npm run dev
```

后端测试：

```bash
cd kidslearn-server
mvn.cmd -pl kidslearn-api -am test
```

## 数据库

MySQL 数据库名默认为 `kids_learn`。当前项目按既有库结构运行；如本地没有完整初始化脚本，请从现有 `kids_learn` 数据库导出结构和基础数据后再启动服务。

数据库增量脚本放在 `docs/db/migrations/`。管理后台角色权限依赖 `admin_role.permissions`，真实库首次升级请执行 `docs/db/migrations/2026-05-24-admin-role-permissions.sql`。

## 当前账号模型

当前实现已统一为一套普通账户：不再区分家长账号和儿童账号，也不再使用登录参数区分账号类型。`child_profile`、`parent_profile` 等历史表如仍存在，应视为同一账户下的学习档案或联系人资料，不作为独立登录身份。

管理后台账号通过 `is_admin` 标识；角色权限由角色的 `permissions` 字段控制，支持 `*`、`admin:*`、`admin:<模块>:*`、`admin:<模块>:<动作>`，动作包括 `read`、`write`、`delete`。

## 代码规模

| 部分 | 规模 |
|------|------|
| 后端 Java（Controller + Service + Engine） | 持续迭代 |
| 前端 Vue 页面 + 组件 | 持续迭代 |
| 前端 JS/工具 | 持续迭代 |
| 数据实体 / Mapper | 持续迭代 |
| 页面 | 持续迭代 |
| 组件 | 持续迭代 |

## License

Private — All rights reserved.
