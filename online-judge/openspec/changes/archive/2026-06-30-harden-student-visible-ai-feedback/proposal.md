## Why

当前 AI 诊断链路已经能稳定跑通，但自动评测主要看命中率、fallback 和安全状态，不能充分代表学生真实看到的反馈质量。人工审查发现：部分文案仍然太像直接改法，提高层有时偏弱，测试报告还会把内部 trace 拼进 `outputDetail`，容易掩盖学生端真实体验。

本变更的目标是把“学生真实可见反馈质量”变成系统门禁，而不是依赖每次临时人工翻报告。

## What Changes

- 新增学生可见反馈质量检查：围绕 `studentReport.basicLayerText`、`improvementLayerText`、`nextActionText` 评估是否太直接、是否混入内部痕迹、是否过长、提高层是否太弱。
- 调整 live eval 报告：保留原始 `outputDetail`，同时新增只包含学生端真实可见内容的字段，方便人工审查。
- 调整诊断提示词：把“下一步行动”明确限制为调试动作、手推动作、验证动作，避免给可复制修改步骤。
- 将学生可见质量结果纳入测试报告统计，但不引入新外部依赖、不新增第二套 Agent。

## Capabilities

### New Capabilities
- `student-visible-ai-feedback-quality`: 定义学生真实可见 AI 反馈的导出、质量检查与评测门禁。

### Modified Capabilities
- `single-agent-ai-diagnosis`: 单 Agent 诊断输出需要优先服务学生可见报告质量，结构化字段不得污染学生端表达。
- `ai-diagnosis-quality-loop`: 质量闭环需要纳入学生可见文案审查，而不只统计标签命中率和 fallback。

## Impact

- 影响测试评测代码：`AssistantLiveEvalTest`、`AssistantLiveEvalReport` 及相关质量门禁测试。
- 影响提示词：`PromptTemplateRegistry` 中正式诊断报告模板。
- 不改学生端 API 结构，不新增数据库表，不更换模型，不恢复双 Agent 链路。
