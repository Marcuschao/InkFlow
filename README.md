# Personal Blog

InkFlow - 智能博客系统：文章/日记、评论互动、站内通知、WebSocket 聊天、AI 写作助手。

## 技术栈

| 模块 | 技术 |
|------|------|
| 后端 | Spring Boot 3.5、Java 17、MyBatis-Plus、Spring Security + JWT |
| 前端 | Vue 3、Vite |
| 存储 | MySQL、Redis、MinIO（可选） |
| 消息 | RabbitMQ |
| 可观测 | Actuator、Micrometer Prometheus、Grafana |

## 目录结构

```
personal-blog/
├── personal-blog-backend/   # Spring Boot API
├── personal-blog-frontend/  # Vue 前端
├── docs/                    # 架构与运维文档
└── ops/                     # Prometheus / Grafana 配置
```

## 本地启动

### 依赖服务

需提前启动：MySQL（库 `blog_db`）、Redis、RabbitMQ；可选 MinIO。

### 配置

复制并编辑 `personal-blog-backend/src/main/resources/application-local.yml`（勿将真实密码提交到 Git）。默认 `spring.profiles.active=local`。

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

## 文档

- [架构总览](docs/architecture.md)
- [通知链路](docs/notification-flow.md)
- [文章缓存与限流](docs/article-cache-and-rate-limit.md)
- [聊天可靠性](docs/chat-reliability.md)
- [可观测性（Prometheus/Grafana）](docs/observability.md)

## 监控说明

- **管理后台** `/admin` → 性能监控：即时快照（缓存、限流、JVM、慢接口）
- **Grafana**：历史趋势，配置见 [docs/observability.md](docs/observability.md)

## CI

推送 `personal-blog-backend/**` 变更时，GitHub Actions 执行 `mvn test`（见 `.github/workflows/backend-ci.yml`）。
