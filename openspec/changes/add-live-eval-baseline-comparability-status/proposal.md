## Why

当前 live-model-eval baseline regression report 可能同时出现 `status=PASSED` 和 `currentRecoveryStatus=BLOCKED`。这表示“没有发现相对 baseline 的质量违例”，但不表示“真实外部模型质量可比”。在 ModelScope 额度或 stream content chunk 阻塞时，报告需要明确标出不可比，避免把 fallback 命中的标签误读成外部模型质量稳定。

## What Changes

- baseline regression report 新增 `comparabilityStatus`、`comparabilityReasonCount`、`comparabilityReasons`。
- 当 `comparedCaseCount=0`、当前 recovery blocked、当前模型命中为 0 且 fallback 命中存在时，输出 `comparabilityStatus=NOT_COMPARABLE`。
- 当存在 baseline/current 可比 case 且没有 recovery blocked 时，输出 `COMPARABLE`。
- 当存在部分对比但仍有风险时，输出 `PARTIAL`。
- 保持既有 `status=PASSED/FAILED` 语义不变，comparability 只解释“这份回归报告能否代表真实外部模型质量”。
- 补充结构测试、真实报告验证、OpenSpec 校验和 secret scan。

## Capabilities

### New Capabilities

- `live-eval-baseline-comparability-status`: 约束 live eval baseline regression report 必须输出可比性状态和原因。

### Modified Capabilities

无。

## Impact

- 测试报告 DTO：`LiveEvalBaselineRegressionReport`
- 报告工厂：`LiveEvalBaselineRegressionReportFactory`
- 结构测试：`AssistantLiveEvalQualityGateTest`
- 真实报告：`target/ai-eval-reports/live-model-eval-baseline-regression-*.json`
- 验证：相关后端测试、真实 live smoke/baseline regression、OpenSpec strict validate、secret scan、`git diff --check`
