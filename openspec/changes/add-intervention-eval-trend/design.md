## Context

`AiQualityTrendService` 当前读取：

- 作业列表。
- 作业提交与诊断分析。
- 教师诊断校正。

它输出跨作业校正率、低置信率、泄题风险、校正标签和 source segment。它没有读取 `ClassReviewFeedback`，也没有复用教师介入/策略成效逻辑，因此不能统计课堂介入类 eval 候选。

## Goals / Non-Goals

**Goals:**

- 跨作业统计教师讲评建议和班级策略反馈形成的 intervention eval candidate。
- 在总览和每个 assignment point 中输出 improved、shifted、stillStuck、waitingFollowup。
- 趋势 summary 能提示“教学建议/策略类 fixture 可沉淀”。
- 前端类型同步新增字段。
- 测试覆盖跨作业聚合和 assignment point。

**Non-Goals:**

- 不新增前端趋势 UI 页面。
- 不重新实现完整 `ClassTeachingStrategyAnalyzer`，趋势层第一版基于反馈证据标签和后续提交判断。
- 不新增数据库表。

## Decisions

### Decision 1: 趋势层使用轻量规则

趋势层需要跨作业聚合，第一版不重建完整作业概览，而是直接用 `ClassReviewFeedback` + 后续提交 + 诊断标签判断：

1. 采纳/调整后无后续提交：`WAITING_FOLLOWUP`。
2. 后续提交 AC：`IMPROVED`。
3. 后续提交仍命中 feedback evidence tags：`STILL_STUCK`。
4. 后续提交未 AC 且不命中原标签：`SHIFTED`。

这与现有 impact analyzer 语义一致，避免趋势层依赖完整课堂概览构建。

### Decision 2: 只统计采纳/调整

忽略 `DISMISSED`，因为它不代表执行后的 eval 候选。

### Decision 3: 候选定义

`IMPROVED`、`SHIFTED`、`STILL_STUCK` 计入 `interventionEvalCandidateCount`。`WAITING_FOLLOWUP` 单独统计为待证据。

## Risks / Trade-offs

- [Risk] 趋势层轻量规则不包含完整策略上下文。-> Mitigation: 第一版只做聚合计数和 evidence trend；单作业详情仍以完整 overview/readiness 为准。
- [Risk] evidence tags 为空导致无法判断 still stuck。-> Mitigation: 无标签且未 AC 时归为 `SHIFTED`，并在后续草稿导出阶段人工审查。

## Migration Plan

无数据库迁移。新增响应字段默认 0，旧前端兼容。
