## 1. OpenSpec

- [x] 1.1 创建 proposal/design/spec/tasks，定义 live-model-eval recovery status。
- [x] 1.2 运行 `openspec validate add-live-model-recovery-status --strict`。

## 2. 报告结构与摘要

- [x] 2.1 扩展 `LiveModelEvalReport`，新增 recovery status summary 字段。
- [x] 2.2 在 `ModelDiagnosisEvalTest.summarizeReport(...)` 中根据 entries/runtime drafts 计算 recovery status。
- [x] 2.3 更新 `liveModelSummaryLine(...)`，输出 recovery status 与 blocked reason count。

## 3. 测试

- [x] 3.1 扩展 `ModelDiagnosisEvalTest#liveModelReportSummarizesLatencyBudgetWithoutLiveModel`，覆盖 RECOVERED 与 BLOCKED 条件。
- [x] 3.2 新增或扩展结构测试覆盖 NOT_APPLICABLE。
- [x] 3.3 运行相关后端测试。

## 4. 真实验证与安全

- [x] 4.1 运行真实 live-model-eval smoke；当前若仍为 429，应确认 `recoveryStatus=BLOCKED`。
- [x] 4.2 运行 secret scan 和 diff check。
