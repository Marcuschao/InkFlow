# 聊天可靠性

## 发送与去重

`ChatReliabilityServiceImpl`：

- 客户端传 `clientMsgId`，Redis 键 `processed:msg:` / `dedup:msg:`，TTL 5 分钟
- 重复请求直接返回已缓存 `ChatMessageVo`
- 成功后 WebSocket ACK + `ChatFanoutPublisher`（Redis Stream 可选）

## 失败重试

发送/广播失败写入 `ChatFailedQueue`，定时任务 `ChatFailedQueueScheduler` 重试，耗尽打错误日志。

## 在线与离线

- 在线：`ChatOnlineService` + Redis TTL（`blog.chat.online-ttl-seconds`）
- 离线消息：Redis List `offline:msg:{userId}`

## 归档

热数据在 MySQL；超过 `blog.chat.archive-hot-days` 的消息由 `ChatArchiveService` 批量上传 MinIO 分片，历史接口可回读归档。

## 监控

| 指标 | 含义 |
|------|------|
| `blog_chat_online_users` | 在线用户数 |
| `blog_chat_offline_queue_length` | 离线队列总长 |
| `blog_chat_failed_queue_pending` | 待重试失败条数 |
| `blog_chat_stream_lag` | Stream pending 数 |

管理端：`GET /api/admin/monitor/chat`。
