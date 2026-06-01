## Why

单作业 AI 质量概览已经能把课堂介入和班级策略成效纳入 eval readiness，但跨作业趋势仍只统计诊断校正、低置信和泄题风险。这样老师无法从长期视角看到教学建议类 eval 资产是否在增长，也看不到“仍卡同类问题”是否在跨作业累积。

教育 agent 的评测闭环需要跨作业趋势，否则每次只能在单作业里判断是否可沉淀，无法形成持续迭代的方向。

## What Changes

- 扩展 `AiQualityTrendResponse`，新增跨作业课堂介入 eval 候选计数、等待后续计数和成效状态计数。
- 每个 `AssignmentQualityPoint` 也输出对应介入 eval 指标。
- `AiQualityTrendService` 读取 `ClassReviewFeedback`，基于后续提交和诊断标签统计 `IMPROVED`、`SHIFTED`、`STILL_STUCK`、`WAITING_FOLLOWUP`。
- 趋势 summary 在存在课堂介入 eval 候选时提示沉淀教学建议/策略类 fixture。
- 前端 API 类型同步新增字段。
- 增加趋势服务测试，覆盖跨作业介入候选和仍卡同类问题。

## Capabilities

### New Capabilities

- `intervention-eval-trend`: 跨作业统计课堂介入和班级策略成效形成的 eval 候选趋势。

### Modified Capabilities

- `ai-quality-trend`: 趋势不再只覆盖诊断校正，也覆盖教学建议成效候选。

## Impact

- 后端：更新 `AiQualityTrendResponse`、`AiQualityTrendService` 和测试。
- API：`/api/teacher/ai-quality/trend` 新增兼容字段。
- 前端：更新 TypeScript 类型；现有 UI 可逐步使用新字段。
- 数据：无数据库迁移，复用 `ClassReviewFeedback`。
