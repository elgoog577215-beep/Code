## Why

当前外部模型诊断已经能利用当前提交、评测事实、规则信号和历史 verdict，但还没有稳定利用“上一轮 AI 给学生布置了什么学习动作，以及学生后续提交是否体现了这个动作”。这会让 agent 停留在单次诊断，无法真正形成“提示 -> 学生行动 -> 再诊断 -> 调整教学策略”的在线辅导闭环。

本变更的目标是把学习动作执行证据前移到下一次诊断链路中，让外部模型和本地策略都能根据上一轮动作是否观察到、部分观察到、被反证或尚未观察到，调整下一步提示粒度和教师介入建议。

## What Changes

- 扩展诊断 evidence 的历史部分，新增上一轮学习干预和执行证据字段。
- 在构造当前提交的历史 evidence 时，从上一条提交分析中读取 `learningInterventionPlan` 与 `learningActionEvidence`，形成可给模型使用的结构化反馈。
- 扩展 `ModelDiagnosisBrief` 的学习轨迹摘要，让外部模型能看到上一轮学习动作状态，而不是只看到 verdict/tag 变化。
- 调整提示词和标准库策略，要求模型在输出下一步建议时区分 `OBSERVED`、`PARTIALLY_OBSERVED`、`CONTRADICTED`、`NOT_OBSERVED`。
- 增加后端测试，验证学习动作证据会进入 evidence package、model brief 和诊断干预计划。

## Capabilities

### New Capabilities

- `learning-action-evidence-feedback`: 定义上一轮学习动作执行证据进入下一次诊断的 evidence、brief、提示词和策略要求。

### Modified Capabilities

- 无。

## Impact

- 后端诊断 evidence：`DiagnosisEvidencePackage`、`DiagnosisEvidencePackageBuilder`、`SubmissionAnalysisService`。
- 外部模型输入：`ModelDiagnosisBrief`、`ModelDiagnosisBriefBuilder`、`PromptTemplateRegistry`、`StandardLibraryPackBuilder`。
- 诊断策略：`DiagnosticAgentService` 的学习动作证据与干预计划调整逻辑。
- 测试：新增或更新 submission application 相关单元测试。
