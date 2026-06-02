## Why

上一轮已把外部模型 stream telemetry 写入 `aiInvocation` 和 live eval report，但 AI 质量概览与趋势仍只消费 `failureReason/failureStage`。这会让“ModelScope stream 请求已发出但 provider 因额度拒绝”与“本地 budget guard 直接短路未发请求”在教师端归因里不够可见。

本变更把 transport telemetry 接入运行归因信号，让真实外部模型调用瓶颈从 report 文件进一步进入质量闭环。

## What Changes

- 扩展 `DiagnosisReportReader.AiInvocationSnapshot`，读取 `transportMode`、stream chunk 计数、finish reason 和 fallback retry 标记。
- 扩展 `AiQualityOverviewResponse.RuntimeAttributionSignal`，输出主导 transport mode、stream no-content 数、invalid chunk 数和 fallback retry 数。
- `AiQualityOverviewService` 在运行归因 summary / recommendedAction / evidenceRefs 中消费 transport telemetry。
- 扩展 `AiQualityTrendResponse.SourceQualitySegment`，按 source segment 暴露 transport mode、stream no-content、invalid chunk 和 fallback retry 计数。
- 更新前端共享类型以保持 API 契约完整。
- 增加无需真实 ModelScope key 的后端测试，覆盖 quota stream no-content 和 budget guard short-circuit 的区分。

## Capabilities

### New Capabilities

- `transport-telemetry-attribution`: 覆盖 AI 质量概览和趋势如何消费外部模型 transport telemetry。

### Modified Capabilities

- `runtime-attribution-action-signal`: 运行归因行动信号增加 transport 维度。
- `ai-quality-trend-runtime-attribution`: 趋势 source segment 增加 transport 维度。

## Impact

- 解析层：`DiagnosisReportReader`
- 后端质量服务：`AiQualityOverviewService`、`AiQualityTrendService`
- DTO：`AiQualityOverviewResponse`、`AiQualityTrendResponse`
- 前端类型：`frontend/src/shared/api/types.ts`
- 测试：AI 质量概览和趋势相关测试
- 生产调用链：不改变外部模型调用、prompt、fallback 或安全策略
