## Context

当前外部模型运行链路已经产生 transport telemetry：

- `aiInvocation.transportMode`
- `streamChunkCount`
- `streamContentChunkCount`
- `streamReasoningChunkCount`
- `streamInvalidChunkCount`
- `streamFinishReason`
- `streamFallbackRetryUsed`

这些字段已进入 reader、质量概览、趋势和教师工作台，但 runtime fixture draft 没有完整保留。fixture draft 是从“线上/真实评测中的失败”沉淀成“可回归验证样本”的桥梁，因此它必须携带足够的运行证据。

## Goals / Non-Goals

**Goals:**

- 保留 runtime fixture draft 中的 transport telemetry。
- 让 quota stream no-content fixture 能明确要求回归时验证 stream content chunk。
- 让 budget guard fixture 保持 transport mode 为空，用于区分本地短路。
- 让 stream invalid chunk 能进入 mustMention 和 source artifacts。
- 保持脱敏，不输出 raw SSE、prompt、response、API key 或 header。

**Non-Goals:**

- 不新增数据库字段。
- 不改变当前 runtime fixture 候选筛选条件。
- 不改变 live eval 是否调用真实 ModelScope。
- 不把 transport telemetry 作为模型诊断质量分数。

## Decisions

### transport telemetry 直接挂到 RuntimeFixtureDraft

这些字段属于 runtime fixture 的一等证据，不适合塞进 `failureReason` 字符串里。直接作为结构化字段输出，便于前端展示、JSON 审核和后续 gate 读取。

### mustMention 只放安全摘要

`mustMention` 追加类似 `transport:stream`、`streamContentChunkCount=0`、`streamInvalidChunkCount=2` 的短标记，不加入 raw chunk 或 provider 原始错误。

### budget guard 保持空 transport

如果本地 budget guard 没有发起 HTTP 请求，transport mode 继续为空，stream counters 为 0。这样 fixture 可以证明“不要把本地短路误判为 provider stream 无内容”。

## Risks / Trade-offs

- [Risk] DTO 字段增多。→ 字段均为简单字符串/数字/布尔值，兼容老前端。
- [Risk] fixture 审核者误读 no-content。→ expectedRuntimeAction 和 mustMention 使用“stream content chunk”语言，强调是传输/响应层证据。
- [Risk] live eval assistant 报告没有 transport 字段。→ assistant draft 字段保持空/0，仅 model report draft 填充。
