## Why

学生推荐已经包含 `learningHypothesis`、`expectedCompletionSignal`、strategy、risk 和 fallback action，也能记录曝光、点击、进入题目和后续提交。但目前推荐效果主要停留在漏斗统计、AC 计数和同类错因复发，系统还不能逐条判断“这条推荐的行动契约是否被兑现、失败原因是什么、下一轮应该怎么调整”。

要让推荐成为教育 agent 的行动闭环，而不是导航入口，需要把每个 recommendation token 的后续行为转成结构化行动证据：完成、未提交、同类错因未解决、策略需降级、需要教师介入或迁移已验证，并让下一轮推荐和 AI 质量概览消费这些证据。

## What Changes

- 新增推荐行动证据分析器，按 recommendation token 汇总曝光、点击、进入题目、后续提交和诊断标签。
- `RecommendationEffectivenessResponse` 新增 `actionEvidenceSignals`，输出每条推荐或聚合后的 action outcome、reason、recommendedAdjustment、teacherAttention、evidenceRefs。
- `RecommendationEffectivenessService` 使用行动证据信号替代仅靠 clicked/submitted/sameFocus 的散点判断，并保留现有指标兼容。
- `StudentRecommendationService` 在下一轮推荐时消费行动证据，识别未完成契约、同类错因未解决和需要降级复盘的状态。
- AI 质量概览的 `RECOMMENDATION_LOOP` 维度纳入行动证据状态和 evidence refs。
- 前端 API 类型补充兼容字段，后续教师端可以展示推荐行动证据。
- 增加后端测试覆盖兑现、未提交、同类错因未解决、教师介入建议和 assignment scope。

## Capabilities

### New Capabilities

- `recommendation-action-evidence-loop`: 覆盖推荐行动契约的结构化 outcome、证据引用、下一轮推荐调整和 AI 质量评测。

### Modified Capabilities

无。

## Impact

- 后端：新增 `RecommendationActionEvidenceAnalyzer`，更新 `RecommendationEffectivenessService`、`StudentRecommendationService`、`AiQualityMetrics`、`AiQualityOverviewService` 和相关测试。
- DTO/API：`RecommendationEffectivenessResponse` 新增兼容字段 `actionEvidenceSignals`。
- 前端：更新 `types.ts` 兼容新字段；本轮不强制新增展示。
- 数据：无数据库迁移；复用现有 `StudentRecommendationEvent` 中的 token、strategy、hypothesis、completion signal、follow-up verdict/tag 字段。
