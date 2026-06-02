## 1. OpenSpec

- [x] 1.1 编写 proposal、design、tasks 和 spec delta。
- [x] 1.2 运行 `openspec validate add-live-eval-baseline-regression-gate --strict`。

## 2. Baseline Regression Gate

- [x] 2.1 新增 `LiveEvalBaselineRegressionGate`。
- [x] 2.2 实现 baseline draft 与当前 entry 的 caseId 匹配。
- [x] 2.3 实现 completed/fallback/safety/signal/evidence/teachingAction/mustKeep 检查。
- [x] 2.4 输出包含 caseId 和退化原因的 violation。

## 3. live eval 接入

- [x] 3.1 在 `AssistantLiveEvalTest` 中读取 `AI_EVAL_BASELINE_REPORT`。
- [x] 3.2 baseline report 存在时执行 regression gate。
- [x] 3.3 baseline report 未配置时保持现有 smoke 行为。

## 4. 测试与验证

- [x] 4.1 扩展无需 API Key 的结构测试，覆盖 baseline 保持、fallback 退化、证据缺失和低质量退化。
- [x] 4.2 运行相关后端测试。
- [x] 4.3 使用上一份真实 assistant report 作为 baseline 运行一次真实 smoke 对比。
- [x] 4.4 运行 OpenSpec 校验和 `git diff --check`。
