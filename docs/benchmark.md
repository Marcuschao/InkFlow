# 压测指南

## 前置

- 后端以 **`local,benchmark`** 启动（`application-benchmark.yml` 关闭 `blog.rate-limit`；`stat-view` 限流场景用 `local` 即可）
- MySQL/Redis/RabbitMQ 可用
- 存在已发布文章 ID（默认 `1`）
- 可选：Prometheus 抓取 `http://127.0.0.1:8081/actuator/prometheus`

## 工具

使用 [k6](https://k6.io/)，脚本在 `scripts/benchmark/k6/`。

## 场景

| 编号 | 脚本 | 目的 |
|------|------|------|
| 1 | `article-detail.js` | 文章详情读路径、缓存与请求合并 |
| 2 | `article-list.js` | 列表分页 |
| 3 | `stat-view.js` | 写路径限流（`blog_ratelimit_rejected_total`） |

### 对比建议

1. **冷启动**：重启应用后立刻压 `article-detail`（低命中）
2. **热缓存**：同参数再跑一轮（看 `blog_cache_caffeine_hit_total` / `blog_cache_redis_hit_total`）
3. **请求合并**：`VUS=80+` 固定同一 `ARTICLE_ID`，观察 DB 连接与 P99
4. **限流**：提高 `stat-view` 的 `VUS`，观察拒绝计数

## 指标（Prometheus）

| 指标 | 含义 |
|------|------|
| `rate(blog_cache_caffeine_hit_total[5m])` | L1 命中速率 |
| `rate(blog_cache_redis_hit_total[5m])` | L2 命中速率 |
| `histogram_quantile(0.95, rate(blog_http_server_requests_seconds_bucket[5m]))` | API P95 |
| `rate(blog_ratelimit_rejected_total[5m])` | 限流拒绝 |

## 结果记录模板

| 场景 | VUs | 时长 | P95(ms) | P99(ms) | 错误率 | 备注 |
|------|-----|------|---------|---------|--------|------|
| 详情-冷 | 50 | 30s | 22.8 | 62.3 | 0% | DISTINCT_IP=1 |
| 详情-热 | 50 | 30s | 34.5 | — | 0% | DISTINCT_IP=1，二轮 |
| 详情-高并发同 ID | 80 | 30s | 10.8 | — | 0% | `8082` + `local,benchmark`，~740 RPS |
| 详情-满负载 | 50 | 30s | 3.3* | 9.1* | 99.2% | 默认脚本，触发 429 |
| 列表 | 30 | 30s | 16.1 | — | 0% | DISTINCT_IP=1 |
| stat-view 限流 | 120 | 30s | 4.3* | — | 99.6% | 约 300 次 2xx，验证限流 |

## 说明

- 压测不接入 GitHub Actions，仅在本地/预发执行
- `ArticleDetailRequestCoalescer` 已在缓存未命中时生效，高并发同 ID 可降低重复查库
