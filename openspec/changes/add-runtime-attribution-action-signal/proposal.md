## Why

当前系统已经能记录并展示外部模型运行失败、部分完成、失败阶段和失败原因，但 AI 质量概览的“模型运行”维度仍只给出泛化建议。教师和维护者看到 `INSUFFICIENT_QUOTA`、`BUDGET_GUARD_OPEN`、`SAFETY_RISK` 或校验失败时，还需要自己把失败原因翻译成下一步动作。

本变更把外部模型运行归因进一步沉淀为可行动信号，让系统能说明主导失败类型、证据样本和推荐处理动作，从而更直接提升真实外部模型参与后的可维护性和迭代效率。

## What Changes

- AI 质量概览新增 `runtimeAttributionSignal`，汇总模型完成、部分完成、运行失败、主导失败类别、主导失败原因、失败阶段、摘要、推荐动作和证据引用。
- 根据 `aiInvocation.failureReason`、`failureStage`、`status` 和 `fallbackUsed` 将失败归类为 `QUOTA_LIMIT`、`BUDGET_GUARD`、`SAFETY_REJECTED`、`VALIDATION_FAILED`、`TIMEOUT`、`PROVIDER_ERROR`、`PARTIAL_COMPLETION` 或 `UNKNOWN_RUNTIME_FAILURE`。
- `MODEL_RUNTIME` 质量维度和 `improvementPriorities` 消费该信号，输出更具体的行动建议。
- 教师工作台 AI 质量摘要展示运行归因信号，让“模型失败”不只是数字，而是可处理的下一步。
- 更新后端测试和前端类型检查，验证额度不足、预算保护或安全/校验失败可以被归因并进入建议链路。

## Capabilities

### New Capabilities

- `runtime-attribution-action-signal`: 约束 AI 质量概览必须把外部模型运行归因转成可行动改进信号。

### Modified Capabilities

无。

## Impact

- 后端 DTO：`AiQualityOverviewResponse`
- 后端服务：`AiQualityOverviewService`
- 后端测试：`AiQualityOverviewServiceTest`
- 前端类型：`frontend/src/shared/api/types.ts`
- 教师工作台：`frontend/src/features/teacher/TeacherPage.tsx`
- 验证：OpenSpec strict validate、相关后端测试、前端 typecheck、`git diff --check`
