## Context

当前质量看板已有：

- `TEACHER_INTERVENTION_LOOP`：判断讲评建议反馈后的改善、转移或仍卡同类问题。
- `CLASS_TEACHING_STRATEGY_LOOP`：判断班级策略反馈后的执行成效。
- `DiagnosisEvalFixtureDraftResponse.interventionFixtures`：导出课堂介入/策略成效 eval 草稿。
- `EvalReadiness`：仅统计教师校正 eval candidate、纠错数量和风险样本。

缺口是 readiness 与 intervention fixtures 脱节。质量看板应该告诉老师“不只有教师校正能沉淀，课堂策略成效也可以沉淀为回归评测”。

## Goals / Non-Goals

**Goals:**

- `EvalReadiness` 增加 `interventionCandidateCount`。
- `EvalReadiness` 增加 `interventionImpactCounts`，区分 improved、shifted、stillStuck、waitingFollowup。
- `EvalReadiness` 增加 `recommendedAction`，根据诊断校正与课堂介入草稿给出下一步。
- 有已执行且可判断的教师介入或策略 impact 时，readiness 可为 `READY`。
- 只有等待后续证据时，readiness 可为 `PARTIAL`。
- 前端展示课堂介入候选数量和 recommendedAction。

**Non-Goals:**

- 不自动创建或保存 eval fixture 文件。
- 不新增新的 readiness endpoint。
- 不改变已有 `qualityDimensions` 排序。

## Decisions

### Decision 1: readiness 使用现有 impact 汇总

在 `AiQualityOverviewService.buildOverview` 已经计算 `teacherInterventionImpacts` 和 `classTeachingStrategySignal`。readiness 直接消费这些结构，不再重新查库或重新计算。

### Decision 2: 可沉淀状态定义

`IMPROVED`、`SHIFTED`、`STILL_STUCK` 都算可沉淀候选，因为它们分别覆盖成功、错因迁移和失败升级三类关键教学场景。

`WAITING_FOLLOWUP` 只算部分 readiness，因为还缺少后续证据。

### Decision 3: 保持诊断 eval 优先但不遮蔽介入 eval

如果同时有教师校正 eval candidate 和介入候选，summary 同时说明两类数量。推荐动作优先指导“同时沉淀诊断和介入 fixture”。

## Risks / Trade-offs

- [Risk] readiness 字段变多，前端过载。-> Mitigation: 主面板只显示数量和推荐动作，细节仍在 JSON/quality dimension 中。
- [Risk] 有 impact 不代表草稿一定人工可用。-> Mitigation: 使用“候选”措辞，并保留人工审查要求。

## Migration Plan

无数据库迁移。新增字段为可选兼容字段。旧前端忽略新增字段不会影响现有页面。
