# Personal Blog

InkFlow - 智能博客系统：文章/日记、评论互动、全文搜索、站内通知、WebSocket 聊天、AI 写作助手、Web Push、PWA。

## 技术栈

| 模块 | 技术 |
|------|------|
| 后端 | Spring Boot 3.5、Spring Cloud Gateway、Nacos、OpenFeign、MyBatis-Plus、JWT |
| 前端 | Vue 3、Vite、Naive UI、Pinia |
| 存储 | MySQL、Redis、MinIO、Elasticsearch |
| 消息 | RabbitMQ |

## 目录结构

```
personal-blog/
├── personal-blog-backend/
│   ├── blog-gateway/    # 8081 网关
│   ├── blog-auth/       # 8082 认证/用户
│   ├── blog-content/    # 8083 内容/聊天/通知
│   ├── blog-ai/         # 8084 AI 服务
│   └── blog-common/     # 公共库（打进各服务 jar）
├── personal-blog-frontend/
└── docs/
```

## 配置说明

**`application.yml`** 只含端口、路由等通用配置，已提交 Git。

**`application-dev.yml` / `application-local.yml`** 含密钥与地址，**不提交 Git**（见 `.gitignore`）。

各模块提供模板：

```
blog-*/src/main/resources/application-dev.yml.example
blog-*/src/main/resources/application-local.yml.example
```

### 首次初始化

**Windows：**

```powershell
cd personal-blog-backend
.\scripts\init-config.ps1
```

**Linux / macOS：**

```bash
cd personal-blog-backend
bash scripts/init-config.sh
```

或手动复制：

```bash
cp blog-auth/src/main/resources/application-local.yml.example \
   blog-auth/src/main/resources/application-local.yml
# 其余 gateway / auth / content / ai 同理
```

然后编辑 `application-dev.yml`（线上）或 `application-local.yml`（本地），将 `CHANGE_ME_*`、`YOUR_*` 替换为真实值。

| Profile | 用途 | 修改文件 |
|---------|------|----------|
| `dev`（默认） | 服务器部署 | `application-dev.yml` |
| `local` | 本机开发 | `application-local.yml`，并改 `application.yml` 中 `spring.profiles.active: local` |

**注意：** 各服务 `spring.security.jwt.secret` 必须一致。

### 前端环境变量

```bash
cp personal-blog-frontend/.env.development.example personal-blog-frontend/.env.development
cp personal-blog-frontend/.env.production.example personal-blog-frontend/.env.production
```

## 本地启动

### 依赖

MySQL（`blog_db`）、Redis、RabbitMQ、Nacos（`127.0.0.1:8848`）；可选 Elasticsearch、MinIO。

### 后端

```bash
cd personal-blog-backend
# 使用 local profile 时先改各模块 application.yml: spring.profiles.active: local
mvn -pl blog-gateway,blog-auth,blog-content,blog-ai -am package -DskipTests

java -jar blog-gateway/target/blog-gateway-*.jar
java -jar blog-auth/target/blog-auth-*.jar
java -jar blog-content/target/blog-content-*.jar
java -jar blog-ai/target/blog-ai-*.jar
```

| 服务 | 端口 |
|------|------|
| Gateway | 8081 |
| Auth | 8082 |
| Content | 8083 |
| AI | 8084 |

API 入口：`http://127.0.0.1:8081/api`

### 前端

```bash
cd personal-blog-frontend
npm install
npm run dev
```

访问：`http://127.0.0.1:5173/pblog/`

## 文档

- [架构总览](docs/architecture.md)
- [通知链路](docs/notification-flow.md)
- [聊天可靠性](docs/chat-reliability.md)
- [可观测性](docs/observability.md)

## CI

推送 `personal-blog-backend/**` 变更时执行 `mvn test`（`.github/workflows/backend-ci.yml`）。
