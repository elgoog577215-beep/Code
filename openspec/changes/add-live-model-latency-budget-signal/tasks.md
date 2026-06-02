## 1. OpenSpec

- [x] 1.1 编写 proposal、design、spec 和 tasks。
- [x] 1.2 运行 `openspec validate add-live-model-latency-budget-signal --strict`。

## 2. Report latency signal

- [x] 2.1 扩展 `LiveModelEvalReport` 顶层字段：`latencyBudgetMs`、`latencyBudgetExceededCount`。
- [x] 2.2 扩展 `LiveModelEvalReport.Entry` 字段：`latencyBudgetMs`、`latencyBudgetExceeded`。
- [x] 2.3 更新 `ModelDiagnosisEvalTest`，从 `AI_EVAL_LATENCY_BUDGET_MS` 读取预算并写入 entry/report。

## 3. Runtime draft 与 baseline

- [x] 3.1 更新 `LiveEvalRuntimeFixtureDraftFactory`，慢响应导出 `SLOW_RESPONSE` draft。
- [x] 3.2 更新 `LiveEvalQualityBaselineDraftFactory`，跳过 latency budget exceeded 的成功样本，并在 baseline mustKeep 中保留预算健康信号。
- [x] 3.3 更新 `LiveEvalBaselineRegressionGate`，检测当前 entry 的 latency budget regression。

## 4. 验证

- [x] 4.1 扩展 `AssistantLiveEvalQualityGateTest` 覆盖慢响应 draft、baseline 跳过和 regression。
- [x] 4.2 扩展 `ModelDiagnosisEvalTest` 的无 live model 结构测试覆盖默认 latency budget。
- [x] 4.3 运行相关后端测试。
- [x] 4.4 运行 OpenSpec strict 校验。
- [x] 4.5 运行 `git diff --check` 和密钥扫描。
- [x] 4.6 运行一条真实 live model smoke，对比 latency budget signal。
