## Why

真实 ModelScope 对照实验显示：`AI_EVAL_MAX_OUTPUT_TOKENS=384` 能把单条 smoke 压到约 33.6 秒，但 stream 以 `finish_reason=length` 结束，JSON 被截断，最终 fallback。当前系统把这类失败归为 `EMPTY_RESPONSE` / `UNKNOWN_RUNTIME_FAILURE`，会掩盖真正原因：输出 token 预算不足。

## What Changes

- 新增 `OUTPUT_TRUNCATED` 运行失败归因，用于表达 provider 因 `finish_reason=length` 截断输出。
- 当解析或校验失败且最近一次调用的 `streamFinishReason=length` 时，fallback failureReason 使用 `OUTPUT_TRUNCATED`。
- live-model-eval runtime fixture draft 将该类失败归为 `OUTPUT_TRUNCATED`，行动建议指向提高输出 token 预算、收缩 schema/上下文或切换分阶段调用。
- OpenSpec 与结构测试覆盖该归因，真实 smoke 使用低 token 预算验证可观测信号。

## Capabilities

### New Capabilities

- `output-truncated-runtime-signal`: 外部模型输出因 token/length 截断时形成结构化运行归因。

### Modified Capabilities

- `export-live-eval-runtime-drafts`: runtime draft 支持 `OUTPUT_TRUNCATED` failureType 和行动建议。
- `add-runtime-attribution-action-signal`: 运行归因分类增加输出截断类别。
- `add-external-model-stream-telemetry`: stream finish reason 被用于归因，而不只是展示。

## Impact

- 后端枚举/归因：`ModelStageFailureReason`
- 外部模型调用链：`AiReportService`
- 评测报告草稿：`LiveEvalRuntimeFixtureDraftFactory`
- 教师端 runtime fixture draft 归因：`ClassroomService`
- AI 质量概览归因：`AiQualityOverviewService`
- 测试：`AiReportServiceExternalRuntimeTest`、`AssistantLiveEvalQualityGateTest`
- 非目标：不自动改变默认 `maxOutputTokens`，不重试真实 provider，不保存 raw provider payload。
