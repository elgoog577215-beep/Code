## Why

最近 live-model-eval 已经在 JSON entry 和汇总字段中区分了最终命中、真实模型命中和 fallback 命中，但控制台摘要仍只展示 `issueHits/fineHits` 这类最终命中。ModelScope 429、额度不足或 runtime fallback 时，维护者很容易把本地规则 fallback 的命中误读为外部模型诊断质量。

本轮把这类归因信息继续传到人读摘要和 baseline regression 报告，降低 live eval 解读误差，让后续模型切换、prompt 调整和额度恢复后的对比更可信。

## What Changes

- live-model-eval 控制台摘要显式输出 final/model/fallback 三类 issue/fine 命中。
- live-model-eval baseline regression report 记录当前 report 的 final/model/fallback 命中计数。
- 增加无需 API Key 的结构测试，锁定 fallback-only 命中不会被摘要误读为 model 命中。
- 保持现有 JSON 字段兼容，不移除 `issueTagHitCount` 与 `fineTagHitCount`。

## Capabilities

### New Capabilities
- `live-model-hit-summary-attribution`: 覆盖 live-model-eval 的人读摘要和 baseline regression 报告如何显式区分最终命中、真实模型命中与 fallback 命中。

### Modified Capabilities
- `live-eval-baseline-regression-report`: baseline regression report 增加 live-model-eval 当前命中归因计数。

## Impact

- 测试报告 DTO：`LiveEvalBaselineRegressionReport` 新增可选 model/fallback 命中计数字段。
- 报告工厂：`LiveEvalBaselineRegressionReportFactory#fromModel` 填充当前 live-model-eval 的命中归因计数。
- 评测入口：`ModelDiagnosisEvalTest` 的 live smoke 控制台摘要使用可测试的结构化 summary line。
- 测试：扩展 `ModelDiagnosisEvalTest` 与 `AssistantLiveEvalQualityGateTest`。
