## 1. 外部诊断部分成功路径

- [x] 1.1 在 `AiReportService` 中新增教学阶段失败后的部分完成构建路径。
- [x] 1.2 用本地安全模板补齐学生提示、干预计划、教师备注和报告。
- [x] 1.3 在 `uncertainty` 中保留教学阶段失败阶段与原因。

## 2. 评测报告适配

- [x] 2.1 将 `MODEL_PARTIAL_COMPLETED` 视为 completed output。
- [x] 2.2 在报告中保留部分成功的 failureStage 和 failureReason。

## 3. 验证

- [x] 3.1 增加教学阶段 API 失败后保留模型诊断的单元测试。
- [x] 3.2 调整教学阶段安全拒绝测试，确认不再整条规则回退。
- [x] 3.3 运行相关 AI 诊断与助手评测测试。
