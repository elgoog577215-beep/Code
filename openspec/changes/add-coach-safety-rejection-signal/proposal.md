## Why

Coach 模型追问已经有安全门：不安全模型草稿会被拒绝并回退到规则追问。但这个事件目前主要停留在日志和临时 `failureReason` 上，保存后的 `CoachPrompt` 和交互摘要无法稳定表达“本轮模型追问曾因安全被拒绝”。

教育 agent 需要区分“学生回答越界”和“AI 追问草稿越界”。前者是学生学习状态，后者是模型安全质量事件；如果后者不结构化，教师端和评测闭环就无法追踪 Coach 追问的安全可靠性。

## What Changes

- 在 `CoachPrompt` 上新增兼容字段，保存模型草稿来源下的失败原因和模型报告的 `answerLeakRisk`。
- 在 `CoachPromptResponse` 中返回这些字段，便于学生端/教师端后续展示或调试。
- `CoachPromptService` 在保存 prompt 时持久化 `CoachAgentService.CoachDraft.failureReason` 与 `answerLeakRisk`。
- `CoachInteractionAnalyzer` 汇总 Coach 交互时识别 `SAFETY_REJECTED`，输出结构化安全拒绝计数、最近原因、证据引用和教师关注标记。
- 用后端测试覆盖安全拒绝落库、响应字段和交互摘要。

## Capabilities

### New Capabilities

- `coach-safety-rejection-signal`: 覆盖 Coach 模型追问被安全门拒绝时的持久化、响应暴露和交互摘要信号。

### Modified Capabilities

- 无。

## Impact

- 后端领域模型：`CoachPrompt` 新增 nullable 字段；当前项目使用 Hibernate `ddl-auto:update`，不需要手写迁移。
- 后端 DTO/服务：`CoachPromptResponse`、`CoachPromptService`、`CoachInteractionSummaryResponse`、`CoachInteractionAnalyzer`。
- 测试：`CoachPromptServiceTest`、`CoachInteractionAnalyzer` 相关覆盖。
- 前端：本轮不修改展示，只保持 API 兼容；字段可被后续 UI 消费。
