## Context

现有 eval 草稿导出路径已经能处理两类样本：

- 教师诊断校正：从 `TeacherDiagnosisCorrection.evalCandidate=true` 生成诊断 fixture draft。
- 课堂介入成效：从教师采纳或调整课堂复盘建议后，根据后续提交生成 intervention fixture draft。

提示安全事件闭环已经新增了作业级 `promptSafetyIncidentSignal`，但导出接口没有把安全风险转成 fixture draft。结果是教师能看到安全告警，却需要手工整理上下文才能沉淀到评测资源。

## Goals / Non-Goals

**Goals:**

- 在现有诊断 eval 草稿响应中新增安全 fixture draft。
- 支持两类来源：`SubmissionAnalysis.answerLeakRisk=HIGH` 和 `HintSafetyCheck.riskLevel in MEDIUM,HIGH`。
- 安全草稿必须包含风险来源、风险等级、blocked reasons、原始/安全提示摘要、submission/problem/analysis 上下文、mustNotMention 和 evidenceRefs。
- LOW 安全检查不生成 safety fixture。
- 保持现有接口和旧字段兼容。

**Non-Goals:**

- 不自动写入测试资源文件。
- 不新增教师手动标记安全 eval candidate 的数据库字段。
- 不改变 `HintSafetyService` 的检测规则。
- 不新增前端展示，只补类型。

## Decisions

### Decision 1: 复用现有 eval 草稿接口

安全 fixture draft 与诊断/课堂介入 fixture draft 都服务“教师审查后沉淀到 eval 资源”的同一工作流，因此扩展 `DiagnosisEvalFixtureDraftResponse`，避免新增接口和前端加载状态。

### Decision 2: 安全草稿按提交去重并合并来源

同一提交可能同时有高泄题诊断和安全降级记录。导出时按 submissionId 合并为一条 safety fixture，`riskSources` 记录多个来源，`evidenceRefs` 合并诊断证据和 safety check 引用。这样减少重复审查，同时保留来源可解释性。

### Decision 3: 安全草稿不要求 expected issue tag

安全 fixture 的核心目标不是错因分类，而是验证输出不会泄题。因此字段聚焦 `mustNotMention`、`expectedSafetyAction`、`safeHintPreview`、`riskLevel` 和 `evidenceRefs`。

### Decision 4: 原始提示只做短摘要

`HintSafetyCheck.originalHint` 可能含有完整代码或答案倾向。响应只输出截断预览，且 `sourceMaterial.anonymizationNote` 明确需要人工审查后再写入 eval 资源，避免把高风险内容直接扩散。

## Risks / Trade-offs

- [Risk] 仅按提交合并会丢失多次安全检查的时间顺序。-> Mitigation: evidence refs 保留多个 `hint_safety_check:<id>`，摘要说明是安全事件草稿而非完整审计日志。
- [Risk] 高风险诊断没有 `HintSafetyCheck` 时缺少原始提示。-> Mitigation: 使用 `SubmissionAnalysis` 的 headline/scenario/reportJson evidence refs 生成草稿，仍可作为安全回归样本。
- [Risk] 导出的原始提示预览可能仍包含敏感答案片段。-> Mitigation: 截断长度较短，并在 mustNotMention 与匿名说明中强调人工审查。
- [Risk] 响应体变大。-> Mitigation: safety fixtures 默认风险优先并限制数量。
