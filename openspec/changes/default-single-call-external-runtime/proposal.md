## Why

当前 ModelScope 默认模型 `deepseek-ai/DeepSeek-V4-Pro` 已确认适合流式调用，但历史 live eval 暴露出 staged 两阶段调用容易在第二阶段触发额度或限流回退。系统已经具备 `single-call` runtime，却仍默认走两次模型调用，并且 `aiInvocation` 只记录 promptVersion/status/fallbackUsed，难以直接区分运行模式、失败阶段和失败原因。

本变更把低预算单次外部模型 runtime 推为默认，并把运行模式与失败归因沉淀成结构化字段，让真实外部模型调用更省额度、更容易评测、更容易解释。

## What Changes

- 将 `ai.external-runtime-mode` 默认值从 `staged` 调整为 `single-call`，保留配置回滚到 staged 的能力。
- 为 `SubmissionAnalysisResponse.AiInvocation` 增加结构化字段：`runtimeMode`、`failureStage`、`failureReason`。
- 外部模型完成、部分完成和回退时，都写入运行模式；失败时写入失败阶段和失败原因。
- `DiagnosticAgentService` 与 `DiagnosisReportReader` 保留并读取这些新增字段，避免 agent 包装后丢失外部模型归因。
- live eval report 优先使用 `aiInvocation` 的结构化失败字段，而不是只从中文 `uncertainty` 文本中解析。
- README 记录默认 single-call、回滚方式和真实 live eval 所需变量。

## Capabilities

### New Capabilities
- `external-runtime-default-observability`: 约束外部模型 runtime 默认使用低预算 single-call，并结构化记录运行模式、失败阶段和失败原因。

### Modified Capabilities

无。

## Impact

- 后端配置：`online-judge/src/main/resources/application.yml`
- 后端 DTO 与读模型：`SubmissionAnalysisResponse.AiInvocation`、`DiagnosisReportReader`
- 外部模型调用链：`AiReportService`、`DiagnosticAgentService`
- live eval 报告：`ModelDiagnosisEvalTest`、`AssistantLiveEvalTest`
- 测试：外部 runtime、诊断报告读取、live eval 归因相关测试
- 文档：`README.md`
