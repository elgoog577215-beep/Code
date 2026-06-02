## 1. 配置与 DTO

- [x] 1.1 将 `ai.external-runtime-mode` 默认值改为 `single-call`，并在 README 说明 staged 回滚方式。
- [x] 1.2 为 `SubmissionAnalysisResponse.AiInvocation` 增加 `runtimeMode`、`failureStage`、`failureReason` 字段。
- [x] 1.3 扩展 `DiagnosisReportReader.AiInvocationSnapshot`，兼容读取新增字段。

## 2. 外部模型 runtime 归因

- [x] 2.1 调整 `AiReportService` 的 invocation 构建逻辑，成功、部分完成、回退都写入 runtimeMode。
- [x] 2.2 外部模型失败、校验失败和部分完成时写入 failureStage/failureReason。
- [x] 2.3 `DiagnosticAgentService` 包装 invocation 时保留新增字段。

## 3. live eval 报告

- [x] 3.1 `ModelDiagnosisEvalTest` 生成 failureReason 时优先读取结构化字段。
- [x] 3.2 `AssistantLiveEvalTest` 生成 failureReason 时优先读取结构化字段。

## 4. 测试与验证

- [x] 4.1 补充/更新单次调用默认行为、staged 回滚、结构化归因读取测试。
- [x] 4.2 运行 OpenSpec strict validate。
- [x] 4.3 运行相关后端测试。
- [x] 4.4 运行受控 live eval，记录默认 single-call 的真实模型结果。
- [x] 4.5 运行 `git diff --check`。
