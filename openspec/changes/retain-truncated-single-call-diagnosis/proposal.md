## Why

低 token 真实 ModelScope smoke 显示：single-call 输出在 `finish_reason=length` 截断时，`diagnosisDecision` 已经完整且命中了预期错因，但后续 `teachingHint` 未完整，系统仍把整单降级为规则兜底。这会浪费真实外部模型已经产出的教育判断，也会让 live eval 的 fallback 数高估“模型完全没用上”。

## What Changes

- single-call runtime 在根 JSON 截断且 `streamFinishReason=length` 时，尝试从原始内容中提取完整 `diagnosisDecision` 子对象。
- 只有当提取出的诊断裁决通过标准库标签、证据引用和安全校验时，才保留为 `MODEL_PARTIAL_COMPLETED`。
- 被截断或缺失的 `teachingHint` 继续由本地安全教学模板补齐，并把失败原因保留为 `OUTPUT_TRUNCATED`。
- live model eval 报告增加 partial 计数，区分完全完成、部分保留、规则兜底/运行兜底。

## Capabilities

### New Capabilities

- `truncated-single-call-diagnosis-retention`: single-call 输出截断时保留已完整且校验通过的外部模型错因裁决。

### Modified Capabilities

- `live-model-eval-reporting`: live eval 汇总区分 partial completion 与 full completion/fallback。

## Impact

- 外部模型调用链：`AiReportService`
- 截断 JSON 子对象提取：仅在内存中解析，不保存 raw provider payload
- live eval 报告结构：`LiveModelEvalReport`
- 评测：`AiReportServiceExternalRuntimeTest`、`ModelDiagnosisEvalTest`
