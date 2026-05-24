## 1. OpenSpec 文档

- [x] 1.1 写入 proposal、design、spec 和 tasks，明确学习动作证据反馈闭环。

## 2. Evidence 与 Brief

- [x] 2.1 扩展 `HistoryEvidence`，加入上一轮学习干预与动作执行证据字段。
- [x] 2.2 在 `SubmissionAnalysisService.buildHistoryEvidence` 中读取上一条分析的学习动作信息。
- [x] 2.3 在 `ModelDiagnosisBriefBuilder` 中把学习动作反馈写入 `learningTrajectorySummary`。

## 3. 策略与提示词

- [x] 3.1 在 `DiagnosticAgentService` 中根据上一轮动作状态调整干预计划与证据引用。
- [x] 3.2 更新提示词和标准库策略，明确四类动作状态的使用方式。

## 4. 测试与验证

- [x] 4.1 补充 evidence、brief、诊断策略相关单元测试。
- [x] 4.2 运行 OpenSpec strict validate。
- [x] 4.3 运行后端 AI targeted tests，并根据结果修正。
