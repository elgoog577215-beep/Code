## Why

live-model-eval report 已经能输出 `recoveryStatus=RECOVERED/BLOCKED/NOT_APPLICABLE`，但 baseline regression report 仍只记录 case 覆盖、violation 和命中计数。当前 ModelScope 429 时，baseline regression 报告无法直接说明“本次比较失败或无 baseline 不是模型质量退化，而是外部模型尚未恢复”。

本轮把当前 live-model-eval 的 recovery status 传播到 baseline regression report，让回归报告可以同时表达质量对比结果和外部模型恢复状态。

## What Changes

- `LiveEvalBaselineRegressionReport` 新增当前 recovery status、检查数、阻塞原因数和阻塞原因摘要。
- `LiveEvalBaselineRegressionReportFactory#fromModel(...)` 从当前 `LiveModelEvalReport` 复制 recovery status 字段。
- assistant baseline regression report 保持现有行为，不填 model recovery 字段。
- 扩展结构测试和真实 smoke baseline regression 验证。

## Capabilities

### New Capabilities
- `baseline-regression-recovery-status`: 覆盖 live-model-eval baseline regression report 如何携带当前外部模型恢复状态。

### Modified Capabilities
- `live-eval-baseline-regression-report`: model baseline regression report 需要暴露 current recovery status。
- `live-model-recovery-status`: recovery status 需要能被 baseline regression report 消费。

## Impact

- 测试 DTO：`LiveEvalBaselineRegressionReport` 新增 current recovery fields。
- 测试工厂：`LiveEvalBaselineRegressionReportFactory#fromModel` 填充 current recovery fields。
- 测试：扩展 `AssistantLiveEvalQualityGateTest#baselineRegressionReportFactorySummarizesModelComparison`。
- 真实验证：使用当前 429 live-model-eval report 作为 baseline 跑 smoke，确认 regression report 写出 `currentRecoveryStatus=BLOCKED`。
