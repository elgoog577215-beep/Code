## Context

`AiReportService` 当前支持 stream 与 non-stream 调用，并在 non-stream 返回无内容时可回退到 stream。SSE 解析会跳过不可解析 chunk，也会忽略 reasoning chunk。这个行为适合保护最终 JSON 内容，但不适合诊断 ModelScope 调用质量：live eval 只能看到最终 completed/fallback，无法知道失败是否由 transport/stream parsing 引起。

本设计在不改变模型 prompt、不改变业务判断的前提下，为最后一次模型调用沉淀轻量 telemetry。

## Goals / Non-Goals

**Goals:**

- 在 `AiInvocation` 中记录最后一次模型调用的传输模式和流式解析摘要。
- 在 `live-model-eval` report 中暴露同样字段，方便真实 smoke 与 baseline 对比。
- 保证 reasoning chunk 不进入最终 JSON content，但能被计数。
- 保证 non-stream fallback 到 stream 时可见。
- 结构测试不依赖真实 ModelScope key。

**Non-Goals:**

- 不记录原始响应正文、chunk 内容、API key 或完整 prompt。
- 不改变重试、预算保护、fallback 和校验规则。
- 不把 telemetry 接入前端展示；本轮先让诊断结果和 eval report 有结构化数据。

## Decisions

### 使用 ThreadLocal 保存最近一次调用 telemetry

`chatCompletion` 是多个调用路径的共同入口，`modelInvocation` 是结果 DTO 汇总点。使用 `ThreadLocal<ExternalModelCallTelemetry>` 可以避免给大量内部方法扩参数，同时与同步 HTTP 调用模型一致。每次 `chatCompletion` 开始时重置，调用完成或失败后保留本线程最后一次 telemetry，供当前 response 构造读取。

### telemetry 只记录摘要字段

为了避免泄露模型输出、prompt 或密钥，telemetry 只记录：

- `transportMode`: `stream` 或 `non-stream`
- `streamChunkCount`
- `streamContentChunkCount`
- `streamReasoningChunkCount`
- `streamInvalidChunkCount`
- `streamFinishReason`
- `streamFallbackRetryUsed`

这些字段足以回答“有没有 chunk”“是不是只有 reasoning”“是否发生解析异常”“是否从 non-stream 回退到 stream”，同时不会保存敏感内容。

### 最终内容仍只来自 content/text

streaming 解析继续只拼接 `delta.content`、`message.content` 或 `text`。`reasoning_content` 只计数，不进入最终 content，避免 reasoning 污染 JSON 解析。

## Risks / Trade-offs

- [Risk] 多阶段 staged runtime 只保留最后一次调用 telemetry。→ 本轮先让现有 invocation 保持简单；需要逐阶段 telemetry 时再新增 stage 列表。
- [Risk] 真实模型响应变化导致字段为空。→ 字段为 nullable/0 值，不影响旧消费方；结构测试覆盖常见 SSE 格式。
- [Risk] ThreadLocal 遗留状态。→ 每次 `chatCompletion` 开始显式 reset，telemetry 只在同一次请求线程内读取。
