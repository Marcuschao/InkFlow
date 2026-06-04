# 可观测性

## 指标端点

后端启动后（profile `local` / `dev`）：

- 健康检查：`http://127.0.0.1:8081/actuator/health`
- Prometheus：`http://127.0.0.1:8081/actuator/prometheus`

主端口 8080 的 `/actuator/**` 仍受 JWT 保护；抓取请用 **8081**。

## 业务指标一览

| 指标 | 类型 |
|------|------|
| `blog_cache_caffeine_*` / `blog_cache_redis_*` | Counter |
| `blog_ratelimit_allowed_total` / `rejected_total` | Counter |
| `blog_http_server_requests_*` | Timer（含 endpoint 标签） |
| `blog_http_slow_total` | Counter（≥500ms） |
| `blog_chat_*` | Gauge |
| `http_server_requests_*` | Spring Boot 默认 |
| `jvm_*` | JVM 默认 |

## traceId

- HTTP：请求头 `X-Trace-Id`（可传入），响应头回写；日志 pattern 含 `[traceId]`
- MQ：发布时写入 AMQP 头 `X-Trace-Id`，消费恢复 MDC，监听结束清理

```bash
curl -H "X-Trace-Id: demo-001" http://localhost:8080/api/articles/1
# 日志中搜索 demo-001
```

## Prometheus（本机）

配置文件：[ops/prometheus/prometheus.yml](../ops/prometheus/prometheus.yml)

```bash
# 下载 Prometheus 后
prometheus --config.file=ops/prometheus/prometheus.yml
```

若 Prometheus 跑在 Docker 内、应用在宿主机，将 target 改为 `host.docker.internal:8081`（Windows/macOS）。

## Grafana

1. 启动 Grafana（本机安装或单容器）：

```bash
docker run -d --name grafana -p 3000:3000 \
  -v "%cd%/ops/grafana/provisioning:/etc/grafana/provisioning" \
  -v "%cd%/ops/grafana/dashboards:/var/lib/grafana/dashboards" \
  grafana/grafana
```

Linux/macOS 将 `%cd%` 换为 `$(pwd)`。

2. 数据源默认指向 `http://127.0.0.1:9090`（见 `ops/grafana/provisioning/datasources/prometheus.yml`）
3. 大盘 `Personal Blog Overview`（`ops/grafana/dashboards/blog-overview.json`）

## 与 Admin 监控页

| 方式 | 用途 |
|------|------|
| `/admin` 性能监控 | 当前快照、慢接口 TopN |
| Grafana | 历史曲线、告警规则 |

## 后续

- OpenTelemetry + Jaeger 分布式追踪（当前未接入）
