## Why

当前 AI 诊断链已经能处理单次提交证据、标准库裁剪、外部模型输出校验和学习动作反馈，但学生长期画像、轨迹统计、教师端能力分析仍主要在诊断链路之外消费。继续新增“长期记忆”时，如果另起一套记忆系统，会导致事实源分裂、提示词旁路、教师端统计重复和后期维护困难。

本变更的目标是在现有诊断主链路上扩展学生学习记忆：复用已有提交、诊断报告、学习轨迹、学习动作反馈和教师修正，把它们压缩为可追溯的 `StudentLearningMemorySnapshot`，并作为证据进入 `DiagnosisEvidencePackage`、`ModelDiagnosisBrief`、`StandardLibraryPack` 和外部模型提示词。

## What Changes

- 新增学生学习记忆快照能力，快照从现有提交、诊断、学习动作反馈、教师修正和能力画像中派生，不新建旁路事实源。
- 将学习记忆快照接入诊断证据包和模型 brief，让外部模型在分析当前代码前看到学生的重复错因、能力焦点、提示响应效果和教师复核信号。
- 更新标准库与 prompt 规则，明确“当前提交直接证据优先，长期记忆只能作为辅助证据”，避免画像误导当前诊断。
- 梳理现有画像、轨迹、教师统计、推荐和 AI 诊断之间的重复边界，形成主链路统一策略，后续逐步把旁路统计收敛到同一诊断证据和报告读取接口。
- 增加测试，验证记忆能进入诊断 brief，能影响候选标签和教学动作，但不会覆盖当前提交证据。

## Capabilities

### New Capabilities

- `student-learning-memory`: 学生长期学习记忆快照进入 AI 诊断主链路，支持重复错因、能力焦点、干预效果、教师复核信号和证据引用。

### Modified Capabilities

- 无。

## Impact

- 后端 AI 诊断链：`SubmissionAnalysisService`、`DiagnosisEvidencePackage`、`ModelDiagnosisBriefBuilder`、`StandardLibraryPackBuilder`、`PromptTemplateRegistry`、`DiagnosticAgentService`。
- 现有画像/轨迹读取：复用 `DiagnosisReportReader`、`AbilitySignalAnalyzer`、学习动作反馈字段和教师修正数据，不新建独立长期记忆表。
- 测试：新增或更新诊断证据包、brief、标准库、外部模型 runtime 和历史证据相关测试。
- 数据兼容：只新增 JSON 字段和内存聚合快照，不进行数据库迁移，不改变已有接口的必填字段。
