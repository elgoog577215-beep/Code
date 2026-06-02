## 1. OpenSpec

- [x] 1.1 编写 proposal、design、spec 和 tasks，定义 baseline comparability 状态。
- [x] 1.2 运行 `openspec validate add-live-eval-baseline-comparability-status --strict`。

## 2. 报告结构与工厂

- [x] 2.1 扩展 `LiveEvalBaselineRegressionReport`，新增 comparability 字段。
- [x] 2.2 更新 `LiveEvalBaselineRegressionReportFactory`，根据 case 覆盖、recovery status、model/fallback hits 和 violations 生成 comparability。

## 3. 测试与真实验证

- [x] 3.1 扩展结构测试，覆盖 PASSED 但 NOT_COMPARABLE 的 model report。
- [x] 3.2 覆盖 COMPARABLE 或 PARTIAL 的正常对比场景。
- [x] 3.3 运行相关后端测试。
- [x] 3.4 运行真实 live-model-eval smoke + baseline regression report，当前若仍为 429，应确认 `comparabilityStatus=NOT_COMPARABLE`。
- [x] 3.5 运行 secret scan 和 `git diff --check`。
