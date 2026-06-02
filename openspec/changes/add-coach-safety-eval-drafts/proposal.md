## Why

Coach 模型追问安全拒绝已经可以落库、进入班级 Coach 质量汇总，并有静态安全拒绝 fixture 回归测试。但真实课堂中的 `SAFETY_REJECTED` prompt 还不能导出为教师可审查的 eval 草稿，导致线上安全事件无法顺畅沉淀为新的安全 fixture。

教育 agent 的安全闭环需要从“发现风险”继续走到“沉淀样本”。当前提示安全事件已经能进入 `safetyFixtures` 草稿，Coach 模型安全拒绝应复用同一出口，让教师在一次 fixture 预览中同时看到学生提示安全风险和 Coach 模型安全风险。

## What Changes

- `exportDiagnosisEvalFixtureDraft` 在生成 `safetyFixtures` 时纳入同作业下 `CoachPrompt.modelFailureReason=SAFETY_REJECTED` 的记录。
- Coach 安全拒绝草稿复用 `DiagnosisEvalFixtureDraftResponse.SafetyFixtureDraft`，新增 risk source、blocked reason、evidenceRefs、sourceMaterial 和 quality 元信息。
- 对同一 submissionId 的高泄题诊断、提示安全降级和 Coach 安全拒绝进行合并，避免教师看到重复草稿。
- 草稿不暴露原始不安全模型草稿全文，只保留安全 fallback 预览、风险来源和 evidenceRefs。
- 教师端不新增独立 UI，继续复用现有安全 fixture 草稿预览。

## Capabilities

### New Capabilities

- `coach-safety-eval-drafts`: 覆盖 Coach 模型安全拒绝事件如何导出为可人工审查、可沉淀的安全 eval 草稿。

### Modified Capabilities

- 无。

## Impact

- 后端服务：`ClassroomService.exportDiagnosisEvalFixtureDraft` 的安全草稿来源扩展为包含 Coach prompt 安全拒绝。
- 后端仓储：读取作业内提交对应的 `CoachPrompt` 记录。
- DTO：复用现有 `SafetyFixtureDraft`，不新增 API 字段。
- 前端：无需改动，现有 `safetyFixtures` 预览会自动显示新增草稿。
- 测试：扩展 `ClassroomServiceCorrectionTest` 覆盖 Coach 安全拒绝草稿和合并逻辑。
