## Why

当前外接大模型诊断链路已经具备证据包、标准库裁剪、模型调用、安全校验和 live eval，但学生端体验仍偏“内部诊断报告”：主错因、次错因、教学提示和运行状态混在一起，学生很难直接理解“我这次先改什么”和“通过后还能怎么提升”。

真实在线判题学习场景需要把内部复杂度收进后端，把学生看到的反馈整理成两条清楚路径：当前错误点和继续提升点。这样既保留外接模型的自主分析能力，又让输出更像编程老师，而不是传统 OJ 的冷冰冰报错。

## What Changes

- 新增学生端双通道反馈结构 `studentFeedback`，作为 `SubmissionAnalysisResponse` 的兼容新增字段。
- 将当前失败原因整理为 `blockingIssues`，将非阻断但有教育价值的提升方向整理为 `improvementOpportunities`。
- 保留现有诊断标签、evidenceRefs、AI invocation、fallback 和安全状态，供教师端、后台和评测系统使用。
- 扩展 `StandardLibraryPack`，让外接模型同时获得诊断标准库和提升点标准库。
- 升级外接模型单次调用输出协议，要求先判断当前错误点，再给继续提升点。
- 增加学生反馈质量评测指标和 complex-live 代表样本期望，衡量外接模型是否能同时做好错因诊断和提升建议。

## Capabilities

### New Capabilities

- `student-facing-dual-channel-ai-feedback`: 学生提交后系统 SHALL expose a student-facing dual-channel AI feedback object separating blocking issues from improvement opportunities.

### Modified Capabilities

- `external-model-intelligence-eval`: 外接模型 live eval SHALL include student feedback quality metrics when complex live cases contain student-facing expectations.

## Impact

- 后端 DTO：兼容新增 `studentFeedback` 及其嵌套结构。
- 外接模型协议：新增/升级结构化输出字段，但保留旧诊断字段和 fallback 语义。
- 标准库：新增提升点 taxonomy 和学生反馈决策规则。
- 评测：复用 14 条 `complex-live-*` 代表样本，新增学生反馈质量指标。
- 前端：学生端可优先读取 `studentFeedback`；旧字段继续可用。
