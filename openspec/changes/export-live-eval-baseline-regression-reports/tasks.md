## 1. OpenSpec

- [x] 1.1 编写 proposal、design、spec 和 tasks。
- [x] 1.2 运行 `openspec validate export-live-eval-baseline-regression-reports --strict`。

## 2. Baseline regression report 结构

- [x] 2.1 新增 `LiveEvalBaselineRegressionReport`，记录 source、baseline/current report 路径、case 覆盖数、状态和 violations。
- [x] 2.2 新增 report factory，从 assistant/model 当前 report 和 baseline drafts 计算 summary。

## 3. Live eval 写出对比报告

- [x] 3.1 在 assistant live eval baseline gate 中写出 `assistant-live-eval-baseline-regression-*.json`。
- [x] 3.2 在 live-model-eval baseline gate 中写出 `live-model-eval-baseline-regression-*.json`。
- [x] 3.3 确保 regression report 在断言失败前写出。
- [x] 3.4 baseline 环境变量未配置时保持现有行为。

## 4. 测试与验证

- [x] 4.1 增加无需 API Key 的结构测试，覆盖 assistant/model regression report summary。
- [x] 4.2 运行相关后端测试。
- [x] 4.3 使用上一份真实 live-model-eval report 作为 baseline 跑一次真实 smoke，并确认写出 regression report。
- [x] 4.4 运行 OpenSpec 校验和 `git diff --check`。
