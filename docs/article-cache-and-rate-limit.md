# 文章缓存与限流

## 多级缓存

`ArticleCacheService` 读路径：

1. **Caffeine L1**（进程内，配置 `blog.cache.l1-max-size` / `l1-write-ttl-minutes`）
2. **Redis L2**（分布式）
3. **MySQL** + 可选 `ArticleBloomFilter` 防穿透

写/更新文章后，`ContentChangeProducer` 发 MQ，`ContentCacheConsumer` 延迟失效（`blog.cache.evict-delay-ms`）。

指标（Prometheus）：

- `blog_cache_caffeine_hit_total` / `miss_total`
- `blog_cache_redis_hit_total` / `miss_total`

## 请求合并

`ArticleDetailRequestCoalescer` 已接入 `ArticleServiceImpl.getArticleVo`：缓存未命中时，相同 `articleId + lang` 的并发查询共用一个 `CompletableFuture`，降低击穿压力。

## 分布式限流

`RedisTokenBucketRateLimiter`（Redisson）+ `ApiRateLimitFilter`：

| 规则 | 配置项 |
|------|--------|
| 文章详情 | `blog.rate-limit.article-detail-per-minute` |
| 列表 | `article-list-per-minute` |
| GET 通用 | `api-get-per-minute` |
| 写接口 | `api-write-per-minute` |

超限返回 429。指标：`blog_ratelimit_allowed_total`、`blog_ratelimit_rejected_total`。

管理端快照：`GET /api/admin/monitor/rate-limit`、`/cache`。

## 压测建议

对比：无缓存 / 仅 Redis / L1+L2；开启合并前后同一 articleId 并发 P99。结果可记入独立 `docs/benchmark.md`（可选）。
