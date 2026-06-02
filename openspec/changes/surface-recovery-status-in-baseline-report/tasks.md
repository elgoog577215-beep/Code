## 1. OpenSpec

- [x] 1.1 创建 proposal/design/spec/tasks，定义 baseline regression recovery status。
- [x] 1.2 运行 `openspec validate surface-recovery-status-in-baseline-report --strict`。

## 2. 实现

- [x] 2.1 扩展 `LiveEvalBaselineRegressionReport`，新增 current recovery status 字段。
- [x] 2.2 更新 `LiveEvalBaselineRegressionReportFactory#fromModel`，复制当前 `LiveModelEvalReport` 的 recovery status。
- [x] 2.3 保持 `fromAssistant` 不填 model recovery 字段。

## 3. 测试与验证

- [x] 3.1 扩展结构测试，覆盖 model regression report 携带 `currentRecoveryStatus=BLOCKED`。
- [x] 3.2 运行相关后端测试。
- [x] 3.3 运行真实 live-model-eval smoke + baseline regression report，当前若仍为 429，应确认 regression report 写出 `currentRecoveryStatus=BLOCKED`。
- [x] 3.4 运行 secret scan 和 diff check。
