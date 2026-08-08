# P1 Agent Runtime 实施记录

更新时间：2026-08-07

## 已实现

- [x] 新增统一 `AgentRuntime`，覆盖 `RAG_QA`、`WRITING`、`RECOMMENDATION`、`REPORT`、`WORKFLOW`。
- [x] 新增 `AgentRun`、`AgentEvent`、`AgentResult`、`AgentToolCall` 及受控状态机。
- [x] 每次状态迁移生成单调递增的 `run.state` 审计事件。
- [x] 增加最大步骤、总超时、Token 预算、取消注册与终态幂等更新。
- [x] `/api/agent/runs` 同步运行、流式运行、取消、状态查询和事件重放接口。
- [x] `/api/agent/chat` 与 `/api/agent/chat/stream` 已接入统一 Runtime。
- [x] RAG 使用 Spring AI 原生 `ChatModel.stream(...)`，取消完整答案后的字符切片伪流式实现。
- [x] SSE 统一发送运行、状态、检索、引用、工具、delta、完成、失败和取消事件。
- [x] Redis Streams 保存短期事件，MySQL 保存运行、事件归档与工具调用审计。
- [x] 工具注册表、风险分级、JSON Schema、Bean Validation、服务端权限、审批、幂等和有限重试。
- [x] `ArticleSearchTools` 移除 `ThreadLocal`，改为无共享状态的结构化结果。
- [x] 前端聊天切换到 `/api/agent/runs/stream` 并适配统一事件协议。
- [x] 增加状态机、事件序列、Schema、工具策略和并发隔离测试。

## 部署前操作

- [ ] 在目标 MySQL 执行 `personal-blog-backend/blog-ai/src/main/resources/db/ai-agent-runtime.sql`。
- [ ] 确认 Redis 可用并设置符合环境容量的 Stream 保留时间。
- [ ] 使用 `src/test/resources/load/agent-runtime-sse.js` 分别执行 100、500、1000 VU 压测。
- [ ] 基于压测结果确定线程池、连接池、SSE 超时和网关超时的生产值。
- [ ] 验证生产模型供应商确实支持原生流式 API；不支持的任务会发出 `degraded` 事件。

## 验证记录

- 后端：`mvn -pl blog-ai -am test`，通过。
- 前端：`npm run build`，通过。
- 尚未执行真实外部模型、MySQL、Redis 联调和 100/500/1000 并发压测。
