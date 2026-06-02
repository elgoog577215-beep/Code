## 1. OpenSpec

- [x] 1.1 编写 proposal、design、tasks 和 spec delta。
- [x] 1.2 运行 `openspec validate add-live-model-eval-baseline-regression-gate --strict`。

## 2. report 与 baseline 草稿

- [x] 2.1 扩展 `LiveModelEvalReport.Entry`，新增实际 issue/fine/evidence refs 字段。
- [x] 2.2 在 `ModelDiagnosisEvalTest` 写 entry 时填充实际标签和证据 refs。
- [x] 2.3 更新 `LiveEvalQualityBaselineDraftFactory`，让 model baseline mustKeep/evidenceRefs 使用实际结构化字段。

## 3. model baseline regression gate

- [x] 3.1 扩展 `LiveEvalBaselineRegressionGate`，新增 live-model-eval evaluate 方法。
- [x] 3.2 检查 fallback、JSON、issue/fine 命中、证据、安全和结构化 mustKeep。
- [x] 3.3 在 `ModelDiagnosisEvalTest` 中读取 `AI_EVAL_MODEL_BASELINE_REPORT` 并执行 gate。
- [x] 3.4 baseline report 未配置时保持现有 smoke 行为。

## 4. 测试与验证

- [x] 4.1 扩展无需 API Key 的结构测试，覆盖 model baseline 保持、退化和缺失 case。
- [x] 4.2 运行相关后端测试。
- [x] 4.3 使用上一份真实 live-model-eval report 作为 baseline 运行一次真实 smoke 对比。
- [x] 4.4 运行 OpenSpec 校验和 `git diff --check`。
