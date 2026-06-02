## Why

外部模型运行归因已经能进入 AI 质量概览和趋势，但 quota、budget guard、结构校验失败、部分完成等真实运行问题还不能沉淀为 eval 草稿。这样 live eval 或课堂运行中暴露的问题只能被“看见”，还不能被教师审查后转成回归样本。

本轮把运行归因继续接入 fixture 草稿导出，让外接 ModelScope 后的失败/半成功样本进入可验证、可迭代、可沉淀的教育 agent 闭环。

## What Changes

- `DiagnosisEvalFixtureDraftResponse` 新增 `runtimeFixtureCount` 和 `runtimeFixtures`。
- `exportDiagnosisEvalFixtureDraft` 从作业提交的 `aiInvocation` 中筛选 `MODEL_RUNTIME_FALLBACK`、`fallbackUsed=true` 和 `MODEL_PARTIAL_COMPLETED` 样本，生成 runtime fixture 草稿。
- runtime 草稿包含运行状态、runtimeMode、failureStage、failureReason 摘要、归因类别、推荐动作、evidenceRefs、mustMention/mustNotMention 和 quality 元信息。
- 草稿不输出 API Key、token 或 provider 原始敏感错误全文，只保留截断后的运行归因摘要。
- 教师端 Fixture 草稿预览新增 runtime 计数、简要列表和 JSON 导出内容。
- 后端测试覆盖 quota/budget/partial 等运行样本的草稿生成和脱敏要求。

## Capabilities

### New Capabilities

- `runtime-failure-eval-drafts`: 覆盖外部模型运行失败和部分完成样本如何导出为可人工审查、可沉淀的 eval fixture 草稿。

### Modified Capabilities

- 无。

## Impact

- 后端 DTO：`DiagnosisEvalFixtureDraftResponse` 增加 runtime 草稿字段和嵌套 DTO。
- 后端服务：`ClassroomService.exportDiagnosisEvalFixtureDraft` 新增 runtime 草稿生成逻辑，复用 `DiagnosisReportReader.aiInvocation(...)`。
- 前端类型：`DiagnosisEvalFixtureDraft` 增加 runtime fixture 类型。
- 教师端：Fixture 草稿预览显示 runtime 计数、失败类别和推荐动作。
- 测试：扩展 `ClassroomServiceCorrectionTest`；涉及前端类型检查。
