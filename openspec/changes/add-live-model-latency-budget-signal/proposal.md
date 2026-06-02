## Why

最新真实 `live-model-eval` smoke 中，外部模型成功完成并命中错因、证据和安全要求，但单条样本耗时约 53.5 秒。当前 report 只统计 failureReason 中的 TIMEOUT，导致“模型成功但超过评测耗时预算”的风险不会进入 summary、runtime draft 或后续回归判断。

## What Changes

- live-model-eval report 增加 latency budget 字段，用于记录本次评测的慢响应预算和慢响应数量。
- 每条 live-model-eval entry 增加 `latencyBudgetMs` 与 `latencyBudgetExceeded`，区分成功、fallback 和慢响应。
- runtime fixture draft 工厂把慢成功样本导出为 `SLOW_RESPONSE`，给出降低上下文、调整 max tokens 或复核 stream reasoning 体积的建议。
- quality baseline 只接收未超 latency budget 的成功样本，避免把“质量对但过慢”的输出沉淀为健康 baseline。
- baseline regression gate 能发现后续同 case 超过 latency budget 的退化。

## Capabilities

### New Capabilities

- `live-model-latency-budget-signal`: live-model-eval 记录并使用外部模型延迟预算信号。

### Modified Capabilities

- `export-live-eval-runtime-drafts`: live-model-eval runtime draft 增加慢响应导出条件和行动建议。
- `add-live-model-eval-baseline-regression-gate`: model baseline regression gate 增加 latency budget 退化检查。
- `export-live-eval-quality-baselines`: model quality baseline 只导出未超 latency budget 的成功样本。

## Impact

- 测试报告 DTO：`LiveModelEvalReport`
- 评测链路：`ModelDiagnosisEvalTest`
- 测试工具：`LiveEvalRuntimeFixtureDraftFactory`、`LiveEvalQualityBaselineDraftFactory`、`LiveEvalBaselineRegressionGate`
- 测试：`AssistantLiveEvalQualityGateTest` 与 `ModelDiagnosisEvalTest` 相关结构测试
- 非目标：不改变真实 HTTP 超时实现、不降低默认模型能力、不修改 API Key 或 provider 配置。
