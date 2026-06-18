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

| 变量 | 生产（根路径部署） | 说明 |
|------|-------------------|------|
| `VITE_APP_BASE_URL` | `/` | 前端路由与静态资源前缀 |
| `VITE_APP_API_BASE_URL` | `/api` | API 前缀 |
| `VITE_APP_WS_BASE_URL` | 留空 | WebSocket 走同源 `/ws` |

后端 `blog` 配置（`application-dev.yml`）需与域名一致：

```yaml
blog:
  site-url: http://domain.com
  site-base-path:          # 根路径部署留空
  oauth:
    redirect-uri: http://domain.com/login/oauth2/code/github
    frontend-callback-url: http://domain.com/oauth/callback
minio:
  public-base-url: http://domain.com/minio
```

GitHub OAuth App 的 **Authorization callback URL** 必须与 `redirect-uri` 完全一致。

数据库站点地址：

```sql
UPDATE blog_site SET site_url = 'http://domain.com' WHERE id = 1;
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

访问：`http://127.0.0.1:5173/`

## 生产部署

线上入口：`http://domain.com`（80 端口，根路径部署）。

### 构建与发布

```bash
cd personal-blog-frontend
npm run build
# 将 dist/* 部署到 nginx 静态目录根（如 /usr/share/nginx/html/）
```

```bash
cd personal-blog-backend
mvn -pl blog-gateway,blog-auth,blog-content,blog-ai -am package -DskipTests
# 上传 jar 并重启各服务（profile: dev）
```

### Nginx 要点

- `listen 80`，前端 SPA：`location / { try_files $uri $uri/ /index.html; }`
- 反向代理：`/api/`、`/oauth2/`、`/login/oauth2/`、`/ws`、`/upload/`、`/minio/`
- 旧链接兼容：`/pblog/*` → 301 到 `/*`
- 52147 / 52148 可 301 到 80 端口

### 上线检查

- [ ] 安全组放行 80
- [ ] GitHub OAuth 回调已更新
- [ ] `blog-auth` 已重启并加载新 `redirect-uri`
- [ ] `blog_site.site_url` 已更新
- [ ] 首页、登录、GitHub OAuth、图片/头像加载正常

## 文档

- [架构总览](docs/architecture.md)
- [通知链路](docs/notification-flow.md)
- [聊天可靠性](docs/chat-reliability.md)
- [可观测性](docs/observability.md)

## CI

推送 `personal-blog-backend/**` 变更时执行 `mvn test`（`.github/workflows/backend-ci.yml`）。
