## Why

课堂介入和班级策略成效现在已经能导出 eval 草稿，但 AI 质量看板的 `evalReadiness` 仍主要基于教师诊断校正和低置信风险样本。这样老师会看到“有课堂介入草稿可沉淀”，但质量看板的评测沉淀状态不会把它算进去。

为了让教育 agent 具备更完整的评测闭环，质量看板需要把“教师介入/策略成效草稿”也纳入 readiness：不仅知道诊断是否可回归，也知道教学建议是否可回归。

## What Changes

- 扩展 `AiQualityOverviewResponse.EvalReadiness`，新增课堂介入 eval 候选数量、状态计数、推荐动作和证据引用。
- `AiQualityOverviewService` 从教师介入 impact 和班级策略 impact 中计算 intervention eval readiness。
- 有教师介入或班级策略成效时，即使没有教师校正诊断样本，也能进入 `READY` 或 `PARTIAL` 的评测沉淀状态。
- 教师端 AI 质量面板展示课堂介入可沉淀数量，避免老师误以为只能沉淀诊断校正样本。
- 增加测试覆盖只有课堂介入成效、没有教师校正时的 readiness。

## Capabilities

### New Capabilities

- `intervention-eval-readiness`: 质量看板把课堂介入/班级策略成效纳入评测沉淀 readiness。

### Modified Capabilities

- `ai-quality-feedback-loop`: `evalReadiness` 不再只看教师诊断校正样本。

## Impact

- 后端：更新 `AiQualityOverviewResponse`、`AiQualityMetrics`、`AiQualityOverviewService` 和测试。
- API：`evalReadiness` 新增字段，旧调用方可忽略。
- 前端：教师端评测沉淀摘要增加课堂介入候选数量。
- 数据：无数据库迁移。
