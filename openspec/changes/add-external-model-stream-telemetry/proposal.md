## Why

当前 live eval 能判断外部模型是否完成、是否 fallback、标签和证据是否命中，但缺少对 ModelScope 响应传输层的结构化观测。流式响应如果出现 reasoning-only、content 为空、chunk 不可解析、非流式回退到流式等情况，结果只会表现为“无内容”或“运行失败”，不利于区分模型质量问题和响应解析问题。

本变更为外部模型调用新增 stream telemetry，让每次诊断结果和 live eval report 都能看到真实调用采用了 stream 还是 non-stream、是否发生 fallback retry、SSE chunk 是否异常，以及最终 content 由哪个通道产出。

## What Changes

- 在 `SubmissionAnalysisResponse.AiInvocation` 中新增可选 `transportMode`、`streamChunkCount`、`streamContentChunkCount`、`streamReasoningChunkCount`、`streamInvalidChunkCount`、`streamFinishReason`、`streamFallbackRetryUsed` 字段。
- `AiReportService` 在 `chatCompletion` / SSE 解析过程中收集本次线程内最后一次调用的 telemetry，并写入 model invocation。
- 流式解析区分 content chunk 与 reasoning chunk；reasoning 不作为最终 JSON content，但计数会被记录。
- 非流式响应无 content 且启用 streaming fallback 时，记录 `streamFallbackRetryUsed=true`。
- `live-model-eval` report entry 增加 transport telemetry 字段，真实 smoke 能输出解析层观测。
- 增加无需 API Key 的结构测试，覆盖流式 chunk 统计、reasoning-only 失败归因和非流式回退到流式。

## Capabilities

### New Capabilities

- `external-model-stream-telemetry`: 覆盖外部模型调用的 stream/non-stream 传输与解析 telemetry。

### Modified Capabilities

- `live-model-eval-report`: live-model-eval report entry 增加外部模型响应传输 telemetry，辅助区分模型质量退化与传输/解析问题。

## Impact

- 生产 DTO：`SubmissionAnalysisResponse.AiInvocation` 仅新增 nullable 字段，保持旧响应兼容。
- 外部模型调用：`AiReportService` 增加调用 telemetry 收集，不改变 prompt、诊断判定或 fallback 规则。
- 评测 DTO：`LiveModelEvalReport.Entry` 增加 telemetry 字段。
- 测试：`AiReportServiceExternalRuntimeTest`、`ModelDiagnosisEvalTest`。
- 生产数据库：不新增表、不迁移。
