## Context

已有能力链条：

- `ClassReviewSuggestion` 给出课堂复盘建议。
- `ClassReviewFeedback` 记录教师采纳、调整或忽略。
- `TeacherInterventionImpactAnalyzer` 判断讲评建议执行后的改善、错因转移或仍卡同类问题。
- `ClassTeachingStrategySignal` 与 `ClassTeachingStrategyImpactAnalyzer` 判断班级策略执行后的成效。
- `exportDiagnosisEvalFixtureDraft` 能把教师校正样本导出为诊断 eval 草稿。

缺口是：课堂介入与班级策略 impact 还不能被导出为 eval 草稿，因此运行证据无法方便地沉淀为回归测试资产。

## Goals / Non-Goals

**Goals:**

- 在现有 fixture draft 响应中新增 `interventionFixtureCount` 和 `interventionFixtures`。
- 为教师讲评建议反馈导出课堂介入 eval 草稿。
- 为 `strategy:` 前缀的班级策略反馈导出策略成效 eval 草稿。
- 草稿包含建议 key、反馈动作、impact 状态、后续提交、证据标签、必须提及内容、禁止项和 eval 目的。
- 教师端预览能看到新增草稿数量和内容。
- 用测试覆盖至少一条改善样本和一条仍卡同类问题样本。

**Non-Goals:**

- 不自动写入测试资源文件；本轮仍只导出草稿供人工审查。
- 不新增表记录 eval 草稿状态。
- 不调用外部模型生成 eval。
- 不替换已有教师校正诊断 eval 草稿格式。

## Decisions

### Decision 1: 复用现有导出端点

继续使用 `/diagnosis-eval-fixture-draft`，因为教师在同一个质量面板里审查“可沉淀样本”。新增字段比新增端点更轻，也能避免前端多一套加载流程。

### Decision 2: 区分两类草稿

响应保留：

- `fixtures`: 教师校正诊断草稿。
- `interventionFixtures`: 课堂介入/策略成效草稿。

这样旧测试与旧调用方不需要重解释 `fixtures` 的含义。

### Decision 3: 只导出有教师反馈的闭环样本

课堂介入 eval 草稿只从已有 `ClassReviewFeedback` 生成。无反馈状态不导出，因为它还不能代表“AI 建议是否执行后有效”的评测样本。

### Decision 4: impact 由现有 analyzer 重新计算

导出时读取同作业提交和分析，再用现有 `TeacherInterventionImpactAnalyzer` 或 `ClassTeachingStrategyImpactAnalyzer` 计算 impact。这样草稿和页面质量状态使用同一套规则。

## Data Shape

新增 `InterventionFixtureDraft`：

- `name`
- `source`: `class-review-intervention-draft` 或 `class-strategy-intervention-draft`
- `suggestionKey`
- `targetAbility`
- `feedbackActionType`
- `impactStatus`
- `impactSummary`
- `followupSubmissionId`
- `followupVerdict`
- `evidenceTags`
- `evidenceRefs`
- `mustMention`
- `mustNotMention`
- `expectedTeachingActions`
- `quality`

`quality` 复用或轻扩展现有 `QualityDraft` 字段，表达 bug pattern、学生误区、预期学生动作和 eval 目的。

## Risks / Trade-offs

- [Risk] 策略反馈复用 `ClassReviewFeedback`，字段名仍叫 suggestion。-> Mitigation: 用 `source` 和 `strategy:` key 区分。
- [Risk] 导出的草稿可能包含过多运行细节。-> Mitigation: 只输出 key、标签、摘要、后续提交 id 和匿名化目的，不输出学生姓名学号。
- [Risk] impact 规则变化会影响草稿内容。-> Mitigation: 用 analyzer 单元测试和导出测试覆盖关键状态。

## Migration Plan

无数据库迁移。上线后旧响应字段继续有效；新前端显示 `interventionFixtureCount` 和 `interventionFixtures`。回滚时忽略新增字段即可。
