## 1. OpenSpec

- [x] 1.1 创建 proposal/design/spec/tasks，说明 live-model-eval 命中归因摘要能力。
- [x] 1.2 运行 `openspec validate surface-live-model-hit-summary --strict`。

## 2. 实现

- [x] 2.1 在 `ModelDiagnosisEvalTest` 中提取可测试的 live-model-eval summary line，并输出 final/model/fallback 命中计数。
- [x] 2.2 扩展 `LiveEvalBaselineRegressionReport`，新增 current final/model/fallback issue/fine 命中计数。
- [x] 2.3 在 `LiveEvalBaselineRegressionReportFactory#fromModel` 中填充 current report 的命中归因计数，assistant report 保持现有行为。

## 3. 测试

- [x] 3.1 扩展 `ModelDiagnosisEvalTest#liveModelReportSummarizesLatencyBudgetWithoutLiveModel`，断言人读摘要不会把 fallback-only 命中算成 model 命中。
- [x] 3.2 扩展 `AssistantLiveEvalQualityGateTest#baselineRegressionReportFactorySummarizesModelComparison`，断言 regression report 携带 current final/model/fallback 命中计数。
- [x] 3.3 运行相关后端测试。

## 4. 真实验证与安全

- [x] 4.1 运行 live-model-eval smoke；若外部额度/429 触发 fallback，确认摘要显示 model 命中与 fallback 命中分离。
- [x] 4.2 运行 secret scan，确认没有写入 API Key。
