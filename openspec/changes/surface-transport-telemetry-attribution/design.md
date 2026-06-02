## Context

当前 `AiQualityOverviewService` 和 `AiQualityTrendService` 已经能基于 `aiInvocation.status/failureStage/failureReason/runtimeMode` 统计外部模型运行失败、部分完成和主导失败类型。`AiInvocation` 新增的 transport telemetry 暂时只停留在响应 DTO 和 live eval report 中，尚未进入质量闭环。

最近真实 report 显示：`transportMode=stream`、`streamChunkCount=0`、`failureReason=INSUFFICIENT_QUOTA`。这说明请求按 stream 发出，但 provider 在返回 SSE 内容前拒绝。这个状态与 `BUDGET_GUARD_OPEN` 不同，后者是本地预算保护直接短路，不应被误判为 provider 已响应。

## Goals / Non-Goals

**Goals:**

- 让 `DiagnosisReportReader` 读取 transport telemetry。
- 让 AI 质量概览的 runtime attribution signal 输出主导 transport mode 和 stream 解析统计。
- 让 AI 质量趋势的 source segment 输出 transport mode 与 no-content/invalid/retry 计数。
- 用测试覆盖 quota stream no-content 和 budget guard short-circuit 的区别。

**Non-Goals:**

- 不改变外部模型调用、预算保护、retry、fallback 或安全策略。
- 不保存 raw SSE 内容、prompt、响应正文或密钥。
- 不新增前端 UI 展示；本轮只更新 API 类型，后续再决定如何展示。

## Decisions

### 将 no-content 定义为 stream 请求无 content chunk

`streamNoContentCount` 的定义是：`transportMode=stream` 且 `streamContentChunkCount=0`。它覆盖 provider 在返回内容前拒绝、连接中断或只返回非内容事件的情况。它不等同于模型质量差，而是传输/响应层缺少可消费内容。

### source segment 不纳入 transportMode 分组键

已有 source segment 按 source/version/status/runtimeMode/failureStage/failureReason 分组。transport telemetry 作为该组内统计字段输出，而不是扩大分组键，避免 source segment 被过度切碎。

### runtime attribution summary 优先补充 transport 解释

主导失败类型仍由 `failureReason/failureStage` 决定，例如 `QUOTA_LIMIT`。transport telemetry 作为补充说明：同样是 quota，`streamNoContentCount>0` 可以提示“请求已发出但 provider 未返回内容 chunk”；`transportMode` 为空则倾向本地预算保护或未发起 HTTP。

## Risks / Trade-offs

- [Risk] 老数据没有 transport 字段。→ reader 返回空值和 0 计数，现有统计保持兼容。
- [Risk] source segment 字段增多。→ 只增加数值摘要，不改变分组数量。
- [Risk] transport 统计被误解为模型质量评分。→ summary 和 recommended action 明确区分 provider/transport 与诊断质量。
