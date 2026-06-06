# Personal Blog

InkFlow - 智能博客系统：文章/日记、评论互动、全文搜索、站内通知、WebSocket 聊天、AI 写作助手与博客问答、Web Push、PWA 离线支持。

## 功能概览

### 读者端

| 模块 | 说明 |
|------|------|
| 文章 & 日记 | Markdown 渲染、标签/归档、阅读历史、修订历史对比 |
| 搜索 | Meilisearch 全文检索，搜索建议 |
| 互动 | 点赞、收藏、评论、关注作者、内容举报 |
| 通知 | 站内消息中心，RabbitMQ 异步投递 + WebSocket 实时推送 |
| 聊天 | HTTP 发送 + WebSocket 广播，离线消息队列 |
| AI | 文章页博客问答（RAG）、编辑器 AI 辅助、AI 周报生成 |
| 账号 | 注册/登录、GitHub OAuth、个人主页、作者写作模式 |
| PWA | Service Worker 离线缓存，可安装到桌面 |
| SEO | RSS、Sitemap、robots.txt |

### 管理后台（`/admin`）

文章管理、文章审核、评论审核、内容举报、敏感词、日记、翻译、内容保鲜、友链、站点设置、操作日志、数据看板、周报/保鲜报告归档、数据库备份、Web Push 推送、消息监控（RabbitMQ Stream）、性能监控（缓存/限流/JVM/慢接口）、AI 周报、聊天管理与在线监控。

## 技术栈

| 模块 | 技术 |
|------|------|
| 后端 | Spring Boot 3.5、Java 17、MyBatis-Plus、Spring Security + JWT、Spring AI |
| 前端 | Vue 3、Vite、Naive UI、Pinia、Chart.js、vite-plugin-pwa |
| 存储 | MySQL、Redis、MinIO（可选，头像/日记/备份/聊天归档等） |
| 搜索 | Meilisearch |
| 消息 | RabbitMQ |
| 认证 | JWT + GitHub OAuth2 |
| 可观测 | Actuator、Micrometer Prometheus、Grafana |

## 目录结构

```
personal-blog/
├── personal-blog-backend/   # Spring Boot API
├── personal-blog-frontend/  # Vue 前端
├── docs/                    # 架构与运维文档
└── scripts/benchmark/       # k6 压测脚本
```

## 本地启动

### 依赖服务

需提前启动：MySQL（库 `blog_db`）、Redis、RabbitMQ、Meilisearch（默认 `http://127.0.0.1:7700`）；可选 MinIO。

### 配置

复制并编辑 `personal-blog-backend/src/main/resources/application-local.yml`（勿将真实密码/API Key 提交到 Git）。默认 `spring.profiles.active=local`。

关键配置项：

| 配置 | 说明 |
|------|------|
| `blog.search.*` | Meilisearch 地址与 API Key |
| `blog.oauth.*` | GitHub OAuth Client ID/Secret 与前端回调地址 |
| `spring.ai.openai.*` | AI 接口（兼容 OpenAI 协议，如 DeepSeek） |
| `blog.push.*` | Web Push VAPID 密钥 |
| `minio.*` | 对象存储（`enabled: false` 可关闭） |
| `blog.notify-mail-enabled` | 邮件通知开关 |

前端开发环境变量见 `personal-blog-frontend/.env.development`：

```
VITE_APP_BASE_URL=/pblog/
VITE_APP_API_BASE_URL=/pblog/api
VITE_APP_WS_BASE_URL=/pblog
```

### 后端

```bash
cd personal-blog-backend
mvn spring-boot:run
```

- API：`http://localhost:8080`
- 管理/指标端口：`http://localhost:8081/actuator/prometheus`

### 前端

```bash
cd personal-blog-frontend
npm install
npm run dev
```

- 开发地址：`http://localhost:5173/pblog/`
- 生产构建：`npm run build`（自动生成 PWA 图标）

## 文档

- [架构总览](docs/architecture.md)
- [通知链路](docs/notification-flow.md)
- [文章缓存与限流](docs/article-cache-and-rate-limit.md)
- [聊天可靠性](docs/chat-reliability.md)
- [可观测性（Prometheus/Grafana）](docs/observability.md)
- [压测指南（k6）](docs/benchmark.md)

## 监控说明

- **管理后台** `/admin/monitor` → 性能监控：即时快照（缓存、限流、JVM、慢接口）
- **管理后台** `/admin/stream` → RabbitMQ 消息监控
- **Grafana**：历史趋势，配置见 [docs/observability.md](docs/observability.md)

## CI

推送 `personal-blog-backend/**` 变更时，GitHub Actions 执行 `mvn test`（见 `.github/workflows/backend-ci.yml`）。
