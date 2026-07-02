# 任务

- [x] 扩展 `AdviceGenerationOutput`，增加 `diagnosisCandidates` 结构。
- [x] 更新 `diagnosis-report-v2` 提示词，要求先自由列诊断候选，再映射标准库，再写学生报告。
- [x] 更新 `AdviceGenerationOutputValidator`，校验候选层的标准库 ID、证据引用、库外边界和置信度。
- [x] 补充单元测试：合法候选通过、未知 ID 拒绝、库外候选带 ID 拒绝、证据别名可修复。
- [x] 更新提示词测试，确保正式模板包含候选层和标准库非约束边界。
- [x] 运行 OpenSpec 与后端相关测试。
