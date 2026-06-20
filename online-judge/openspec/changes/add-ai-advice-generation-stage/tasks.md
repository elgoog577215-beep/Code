## 1. OpenSpec 与结构准备

- [x] 1.1 完成 proposal/design/spec/tasks 并通过 OpenSpec 校验
- [x] 1.2 确认现有搜索定位链路和 `StudentFeedback` 映射入口

## 2. Advice 输出模型

- [x] 2.1 新增 `AdviceGenerationOutput` 及内部嵌套结构
- [x] 2.2 扩展 `SubmissionAnalysisResponse`，增加 `caseUnderstanding`、`basicLayerAdvice`、`improvementLayerAdvice`
- [x] 2.3 扩展 `AiInvocation`，增加 advice 阶段状态、失败原因、数量和 prompt 版本字段

## 3. Prompt 与运行时接入

- [x] 3.1 新增 `diagnosis-and-advice-v1` prompt
- [x] 3.2 将 advice prompt 接入 `ExternalModelAgentRuntime`
- [x] 3.3 调整 `AiReportService` 单次完整诊断链路，优先解析 advice 输出并保留旧输出回退

## 4. 校验与映射

- [x] 4.1 新增 `AdviceGenerationOutputValidator`
- [x] 4.2 校验标准库 ID、证据引用、空建议、置信度和安全风险
- [x] 4.3 新增 advice 到 `StudentFeedback` 的映射逻辑
- [x] 4.4 advice 失败时回退现有诊断反馈并记录 trace

## 5. 测试与验证

- [x] 5.1 增加 advice 输出校验单元测试
- [x] 5.2 增加 advice 到学生反馈映射测试
- [x] 5.3 增加链路测试：搜索定位成功后 advice prompt 使用精选标准库
- [x] 5.4 增加链路测试：非法 advice 输出回退且 trace 可见
- [x] 5.5 运行相关后端测试和 OpenSpec validate
