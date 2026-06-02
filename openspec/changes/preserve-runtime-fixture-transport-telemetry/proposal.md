## Why

系统已经能在 live eval report、AI 质量概览、趋势和教师工作台中展示外部模型 transport telemetry。但 `DiagnosisEvalFixtureDraftResponse.RuntimeFixtureDraft` 仍只保留 runtime mode、status、failureStage、failureReason 和 failureType，没有把 `transportMode`、stream chunk 计数、finish reason、fallback retry 一并沉淀。

结果是：教师端看到“ModelScope stream 请求无 content chunk”，但点击“预览草稿”准备沉淀 runtime fixture 时，关键传输证据又丢失了。后续额度恢复后，回归样本无法稳定区分 provider stream no-content、budget guard 本地短路和 stream parser 异常。

## What Changes

- 扩展 `DiagnosisEvalFixtureDraftResponse.RuntimeFixtureDraft`，输出 transport telemetry 字段。
- `ClassroomService` 导出 runtime fixture 草稿时写入 transport mode、stream chunk 计数、finish reason 和 fallback retry。
- runtime fixture 的 `mustMention`、source artifacts 和 quality purpose 补充 transport 证据，让 fixture 审核者知道需要验证哪层。
- 前端 fixture 草稿预览展示 runtime transport chip。
- live eval runtime fixture draft 工厂从 `LiveModelEvalReport.Entry` 保留 transport telemetry。

## Capabilities

### New Capabilities

- `runtime-fixture-transport-telemetry`: runtime fixture 草稿保留外部模型传输归因证据。

### Modified Capabilities

- `add-runtime-failure-eval-drafts`: runtime fixture draft 增加 transport 维度。
- `export-live-eval-runtime-drafts`: live eval runtime draft 输出 transport 维度。
- `surface-transport-attribution-teacher-workbench`: 教师端 fixture 预览消费 transport telemetry。

## Impact

- 后端 DTO：`DiagnosisEvalFixtureDraftResponse.RuntimeFixtureDraft`
- 后端服务：`ClassroomService`
- 前端类型/展示：`types.ts`、`TeacherPage.tsx`
- 测试工具：`LiveEvalRuntimeFixtureDraft`、`LiveEvalRuntimeFixtureDraftFactory`
- 测试：runtime fixture draft、live eval quality gate/runtime draft 工厂相关测试
- 非目标：不改变外部模型调用、预算保护、stream retry 或 prompt。
