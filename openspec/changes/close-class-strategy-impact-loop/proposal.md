## Why

班级教学策略已经能告诉老师“这节课怎么组织”，但目前策略生成后没有独立的执行反馈和成效判断。教育 agent 不能停在给建议；它需要知道老师是否采纳策略、后续学生证据是否改善、是否仍卡在原问题，才能形成真正的课堂评测闭环。

本变更关闭班级策略成效闭环，把 `classTeachingStrategySignal` 从“课堂策略建议”升级为“策略建议 + 教师反馈 + 后续学习成效”的结构化状态。

## What Changes

- 为 `classTeachingStrategySignal` 新增 `strategyKey` 和 `impact`，用于标识策略反馈目标并表达执行后成效。
- 复用现有 `ClassReviewFeedback` 反馈存储，不新增数据库表；教师端可对班级策略本身记录采纳、调整或忽略。
- 新增 `ClassTeachingStrategyImpactAnalyzer`，基于策略聚焦标签、教师反馈和反馈后的提交判断策略状态：等待反馈、已忽略、等待后续证据、已有改善、错因转移、仍卡同类问题。
- 将策略成效接入教师作业概览，老师能在策略区看到执行后的影响和下一步建议。
- 将策略成效接入 AI 质量概览，扩展 `CLASS_TEACHING_STRATEGY_LOOP` 对执行反馈和后续成效的判断。
- 增加后端测试和前端类型/展示验证，覆盖反馈记录、成效判断、质量维度和教师端展示。

## Capabilities

### New Capabilities

- `class-strategy-impact-loop`: 班级教学策略成效闭环，覆盖策略反馈标识、教师反馈记录、后续提交成效判断、质量维度和前端展示。

### Modified Capabilities

- 无。

## Impact

- 后端：新增策略成效 analyzer，更新 `AssignmentOverviewResponse`、`ClassroomService`、`ClassReviewFeedbackService`、`AiQualityMetrics`、`AiQualityOverviewService` 和测试。
- API：复用现有课堂反馈接口；前端发送策略 `suggestionKey`、聚焦能力、示例题和证据标签即可。
- 前端：教师策略区新增采纳/调整/忽略动作和成效摘要。
- 数据：不新增迁移；策略反馈写入现有 `class_review_feedback`。
- 兼容性：没有策略反馈时返回“待教师反馈”，旧调用方可忽略新增 impact 字段。
