## Why

系统已经能记录教师诊断校正、统计 AI 质量并导出 eval 候选，但这些校正目前主要以摘要文本和报表形式存在，尚未稳定反哺后续相似提交的诊断决策。教育 agent 需要把教师反馈升级为可复用的校准记忆，让老师的纠错既能沉淀评测，也能在线约束下一次错因判断。

本变更新增教师校准闭环，把教师校正从“事后统计”升级为“下一次诊断可引用、可降置信、可触发教师复核的结构化信号”。

## What Changes

- 新增 `teacherCalibrationSignal`，表达教师校正模式、原始标签、修正标签、校准强度、证据引用、是否应用到当前诊断和是否需要教师复核。
- 在诊断证据包的学习记忆中保留结构化教师校准模式，而不只保留 `teacherCorrectionSummary` 文本。
- 在模型诊断 brief 中暴露教师校准候选信号、允许标签和 evidenceRefs，让外部模型能看到教师已校准的错因方向。
- 在诊断 agent 中应用教师校准：当当前诊断与强教师校准冲突时降低置信、补充不确定性和教师复核提示；当校准可支持当前判断时补充证据引用。
- 在 AI 质量概览新增 `TEACHER_CALIBRATION_LOOP` 维度，区分“有校正但未校准”“校准已应用”“存在冲突需教师复核”等状态。
- 增加后端测试，覆盖校准信号生成、brief 接入、诊断冲突处理和质量维度。

## Capabilities

### New Capabilities

- `teacher-calibration-loop`: 教师校准闭环，覆盖教师校正模式结构化、后续诊断接入、冲突降置信、教师复核建议、AI 质量维度和验证。

### Modified Capabilities

- 无。

## Impact

- 后端：更新 `DiagnosisEvidencePackage`、`ModelDiagnosisBrief`、`ModelDiagnosisBriefBuilder`、`SubmissionAnalysisService`、`DiagnosticAgentService`、`AiQualityMetrics`、`AiQualityOverviewService` 和相关测试。
- API：`SubmissionAnalysisResponse` 新增可选 `teacherCalibrationSignal` 字段，保持旧调用方兼容。
- 数据：不新增数据库迁移；从现有 `TeacherDiagnosisCorrection` 动态构建校准信号。
- 前端：本轮不强制新增展示；教师端可先通过 AI 质量维度看到闭环状态，后续再扩展提交详情展示。
- 兼容性：新增字段均为可选扩展；没有教师校正历史时行为保持不变。
