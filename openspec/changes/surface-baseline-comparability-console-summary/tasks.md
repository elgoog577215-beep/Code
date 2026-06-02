## 1. 摘要格式

- [x] 1.1 为 `LiveEvalBaselineRegressionReport` 增加 baseline regression 摘要方法。
- [x] 1.2 摘要包含 status、comparability、reason count、compared cases 和 violation count。

## 2. 控制台接入

- [x] 2.1 在 `AssistantLiveEvalTest` 写出 baseline regression report 后打印摘要。
- [x] 2.2 在 `ModelDiagnosisEvalTest` 写出 baseline regression report 后打印摘要。

## 3. 验证

- [x] 3.1 扩展 `AssistantLiveEvalQualityGateTest` 覆盖 `PASSED + NOT_COMPARABLE` 摘要。
- [x] 3.2 运行相关后端测试。
- [x] 3.3 运行 OpenSpec strict validate、secret scan 和 `git diff --check`。
