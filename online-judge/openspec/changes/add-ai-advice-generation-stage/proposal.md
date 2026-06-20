## Why

搜索定位阶段已经能把大标准库压缩成相关小分支，但最终学生看到的诊断仍主要依赖旧的 `diagnosis-and-teaching` 输出结构，不能稳定表达“题目理解、代码理解、基础层问题、提高层建议、下一步行动”的完整链路。

本变更把第二次模型调用升级为“完整诊断与建议生成阶段”：让外部 LLM 基于题目、代码、判题结果和精选标准库做整体判断，再输出有主次、有证据、有顺序、可校验的学生建议。

## What Changes

- 新增 `diagnosis-and-advice-v1` prompt，作为搜索定位之后的完整建议生成模型阶段。
- 新增 `AdviceGenerationOutput` 结构，表达题目理解、代码意图、行为差距、基础层建议、提高层建议和下一步计划。
- 新增 advice 输出校验器，校验标准库 ID、证据引用、建议完整性、置信度和答案泄露风险。
- 将 advice 输出兼容映射到现有 `StudentFeedback`，先不要求前端大改。
- 扩展 `SubmissionAnalysisResponse` 和 `AiInvocation`，记录 advice 阶段输出与 trace。
- advice 阶段失败时回退现有诊断/规则反馈，不阻断判题。

## Capabilities

### New Capabilities

- `ai-advice-generation-stage`: 定义搜索定位后的完整诊断与建议生成阶段、结构化输出、后端校验、兼容映射和可观测要求。

### Modified Capabilities

- 无。

## Impact

- 后端：影响 `ExternalModelAgentRuntime`、`PromptTemplateRegistry`、`AiReportService`、模型输出校验、诊断响应 DTO。
- API/类型：扩展 `SubmissionAnalysisResponse` 和 `AiInvocation` 的兼容字段。
- 测试：新增 advice 输出校验、映射、链路回退测试，并回归搜索定位与现有 AI 诊断测试。
