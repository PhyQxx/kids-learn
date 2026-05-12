# 趣学星球 (KidsLearn Planet)

儿童游戏化学习平台，通过答题闯关、宠物养成、成就系统等游戏化机制激发学习兴趣。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端（App） | UniApp + Vue 3 + 图鸟 UI |
| 前端（管理后台） | Vue 3 + Vite + TypeScript |
| 后端 | Spring Boot 3.2.5 + MyBatis-Plus |
| 数据库 | MySQL 8 + Redis |
| 语音合成 | Moss TTS |
| AI 解析 | DeepSeek API |

## 项目结构

```
kidslearn-app/       # UniApp 移动端（Vue 3）
kidslearn-ui/        # 管理后台（Vue 3 + Vite）
kidslearn-server/    # 后端服务（Spring Boot 多模块）
  ├── kidslearn-api/       # 业务 API
  ├── kidslearn-common/    # 公共模块
  └── kidslearn-generator/ # 代码生成器
prototype/           # 原型设计
docs/                # 设计文档
```

## 功能模块

- **用户系统** — 注册/登录、角色权限（儿童/家长）
- **学习引擎** — 多学科题库、答题提交、自适应难度
- **错题本** — 自动归集错题、重做练习、AI 逐题解析
- **宠物系统** — 虚拟宠物养成、喂食互动、状态管理
- **成就系统** — 徽章收集、里程碑达成
- **挑战模式** — 限时答题挑战
- **排行榜** — 全站/好友排名
- **家长端** — 学习报告、消费管理
- **每日签到** — 连续签到奖励
- **订阅/订单** — 会员订阅与支付

## 快速开始

### 环境要求

- Node.js >= 16
- Java 17
- MySQL 8.0
- Redis
- Maven 3.8+

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

## API 概览

后端 API 基础路径：`/api/v1`

主要控制器：

| 控制器 | 职责 |
|--------|------|
| AuthController | 认证授权 |
| LearnController | 答题学习、错题管理、AI 解析 |
| PetController | 宠物养成 |
| ChallengeController | 挑战模式 |
| AchievementController | 成就系统 |
| LeaderboardController | 排行榜 |
| ParentController | 家长端 |
| UserController | 用户管理 |
| SubscriptionController | 订阅会员 |
| OrderController | 订单支付 |

## 数据库

MySQL 数据库，使用 MyBatis-Plus 自动建表。SQL 脚本位于 `kidslearn-server/sql/` 目录。

## License

Private — All rights reserved.
