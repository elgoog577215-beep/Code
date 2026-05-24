## 排查结论

agent 能力升级后，必须保证“能力版本可观测”。当前问题是：  
`decisionProtocol` 已经改变了诊断 prompt 的语义，但 `PromptTemplateRegistry` 和 `AiReportService` 仍记录旧版本：

- `diagnosis-judge-v1`
- `diagnosis-and-teaching-v1`
- `diagnosis-judge-v1+teaching-hint-v1`

这样会导致：

- live eval 无法区分旧 prompt 和新 prompt。
- 教师端 AI 质量趋势按 promptVersion 聚合时会混淆不同策略。
- 后续回滚、A/B、复盘都缺少版本证据。

## 方案

采用最小兼容升级：

- 新增 `DIAGNOSIS_JUDGE_V2 = diagnosis-judge-v2`。
- 新增 `DIAGNOSIS_AND_TEACHING_V2 = diagnosis-and-teaching-v2`。
- 运行时 `ExternalModelAgentRuntime.prepare` 使用 v2。
- `AiReportService` 的 runtime promptVersion 常量同步升级。
- 保留 v1 模板常量与可获取能力，避免历史测试或兼容入口受影响。

`teaching-hint-v1` 不升级，因为这轮没有改变教学表达阶段的语义，只改变诊断裁决阶段和 single-call 诊断部分。

## 验收标准

- runtime plan 默认使用 `diagnosis-judge-v2` 和 `diagnosis-and-teaching-v2`。
- staged 成功输出 `promptVersion=diagnosis-judge-v2+teaching-hint-v1`。
- single-call 成功输出 `promptVersion=diagnosis-and-teaching-v2`。
- prompt contract 测试覆盖 v2 中的 `decisionProtocol`。
- targeted tests 和 OpenSpec strict validate 通过。
- 节制 live eval 通过，并在报告中显示 v2 promptVersion。
