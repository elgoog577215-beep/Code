## Context

当前系统已有三层相邻能力：

- `classTeachingStrategySignal` 能把班级风险汇总成全班小讲评、小组复盘或分层支持策略。
- `ClassReviewFeedback` 能记录老师对课堂复盘建议的采纳、调整或忽略。
- `TeacherInterventionImpactAnalyzer` 能判断讲评建议被执行后，后续提交是否改善、错因转移或仍卡同类问题。

缺口在于班级策略本身还没有反馈目标和成效字段。老师可以看到策略，但不能直接对策略做反馈；质量看板也只能判断“策略是否生成”，无法判断“策略是否被课堂执行并带来后续证据”。

## Goals / Non-Goals

**Goals:**

- 为 `classTeachingStrategySignal` 增加稳定 `strategyKey`，允许教师端对策略本身记录反馈。
- 复用 `ClassReviewFeedback`，避免新增数据表。
- 新增 `ClassTeachingStrategyImpactAnalyzer`，基于策略、反馈和后续提交判断策略成效。
- 在教师工作台策略区显示反馈动作和成效摘要。
- 扩展 `CLASS_TEACHING_STRATEGY_LOOP`，把反馈执行和成效纳入质量判断。
- 用测试覆盖策略反馈、改善、仍卡同类问题、无反馈兼容和质量维度。

**Non-Goals:**

- 不替代已有 `ClassReviewSuggestion` 的反馈与成效分析。
- 不新增复杂课堂观察录入表；第一版只用老师反馈和后续提交证据。
- 不强制教师必须反馈策略；无反馈时保持可读的待反馈状态。
- 不用外部模型评估策略成效；第一版用可解释规则。

## Decisions

### Decision 1: 复用 `ClassReviewFeedback`

班级策略反馈使用现有 `recordClassReviewFeedback` 接口。策略生成时输出：

- `strategyKey`: `strategy:<assignmentId>:<status>:<focus>:<focusTag>`
- `focusAbility`
- `focusTag`
- `evidenceRefs`

前端把这些字段映射到 `ClassReviewFeedbackRequest` 的 `suggestionKey`、`targetAbility`、`evidenceTags`。这样后端无需新增表，也能通过 `latestByAssignment` 找到策略反馈。

### Decision 2: 策略成效使用独立 analyzer

新增 `ClassTeachingStrategyImpactAnalyzer`，输出 `ClassTeachingStrategyImpact`：

- `status`: `NO_FEEDBACK`、`DISMISSED`、`WAITING_FOLLOWUP`、`IMPROVED`、`SHIFTED`、`STILL_STUCK`
- `statusLabel`
- `summary`
- `recommendedAction`
- `needsEscalation`
- `feedbackActionType`
- `feedbackAt`
- `followupSubmissionId`
- `followupVerdict`
- `matchedTags`
- `evidenceRefs`

虽然字段接近 `TeacherInterventionImpact`，但策略层需要处理 `strategyKey`、`focusTag`、`sourceSignals` 和策略证据，因此独立 analyzer 更清晰。

### Decision 3: 成效判断遵循提交证据优先

规则：

1. 无反馈：`NO_FEEDBACK`。
2. 忽略：`DISMISSED`。
3. 采纳/调整后无后续提交：`WAITING_FOLLOWUP`。
4. 后续相关提交 AC：`IMPROVED`。
5. 后续提交仍命中策略聚焦标签或证据标签：`STILL_STUCK`，需要升级。
6. 后续提交未 AC 且不再命中原标签：`SHIFTED`。

相关提交第一版按反馈时间之后、同作业内的后续提交判断；若策略有 `focusTag` 或 evidenceTags，则用诊断标签匹配。

### Decision 4: 质量维度从“生成策略”升级为“执行闭环”

`CLASS_TEACHING_STRATEGY_LOOP` 保持原 dimension id，但状态判断加入 impact：

- 无策略：`ACTION_NEEDED`。
- 有可执行策略但无反馈：`WATCH`。
- 已执行但等待后续提交：`WATCH`。
- 后续改善：`HEALTHY`。
- 仍卡同类问题：`ACTION_NEEDED`。
- 已忽略：`WATCH`，建议检查策略生成是否贴合课堂。

## Risks / Trade-offs

- [Risk] 复用 `ClassReviewFeedback` 的命名仍叫 review suggestion。-> Mitigation: 用 `strategy:` 前缀区分策略反馈。
- [Risk] 后续提交相关性可能粗糙。-> Mitigation: 第一版用时间、同作业、标签匹配；输出 evidenceRefs 供教师复核。
- [Risk] 教师端按钮增加页面负担。-> Mitigation: 只在策略区使用紧凑三按钮，和讲评建议反馈保持一致。
- [Risk] 老师不反馈导致成效长期未知。-> Mitigation: 质量维度显示 WATCH，并把反馈作为下一步动作。

## Migration Plan

无需数据库迁移。已有 `class_review_feedback` 继续可用；新策略反馈用 `strategy:` key 写入同表。回滚时前端隐藏策略反馈按钮，后端忽略 strategy impact，不影响既有复盘建议反馈。
