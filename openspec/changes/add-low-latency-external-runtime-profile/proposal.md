## Why

最新真实 ModelScope live eval 显示，外部模型已能命中错因和细粒度标签，但响应时延仍显著超出 35s budget：`live-model-eval-20260601-204316.json` 两个 case 均 `MODEL_COMPLETED`，但 latency 分别约 105.7s 和 52.5s，reasoning chunk 分别 564 和 266。当前系统能归因慢响应，但还缺少一个可配置、可观测、可回退的低延迟 runtime profile 来减少输入/输出体积并对比效果。

## What Changes

- 新增 `low-latency` 外部 runtime profile，默认不改变现有行为，只有显式配置或 eval 环境变量启用。
- 在 low-latency profile 下生成 compact `ModelDiagnosisBrief` 和 compact `StandardLibraryPack`，减少长文本、长规则、冗余说明和候选列表体积。
- 请求遥测记录 request 字节数、profile 名称和 compact 标记，并导出到 `aiInvocation` 与 live eval report。
- live eval report/runtime draft 能用 requestBytes、latencyMs、reasoning/content chunk 对比 profile 效果。
- 结构测试确保 compact profile 不绕过标签、证据和提示安全校验。

## Capabilities

### New Capabilities

- `low-latency-external-runtime-profile`: 外部模型 runtime 可启用低延迟 profile，压缩上下文与输出契约，并记录请求体积/响应遥测。

### Modified Capabilities

- `live-model-eval-reporting`: live eval 增加 profile/request size 遥测，用于对比延迟优化效果。

## Impact

- 外部模型调用链：`AiReportService`
- runtime 输入构造：`ExternalModelAgentRuntime`、`ModelDiagnosisBriefBuilder`、`StandardLibraryPackBuilder`
- DTO/报告：`SubmissionAnalysisResponse.AiInvocation`、`LiveModelEvalReport`
- 评测：`AiReportServiceExternalRuntimeTest`、`ModelDiagnosisEvalTest`、runtime draft factory 测试
- 非目标：不默认更换模型、不泄露 prompt 或 raw response、不降低安全/证据校验。
