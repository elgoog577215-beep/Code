## Why

Coach 追问已经能判断学生回答是否形成证据，也能在学生行展示单次追问后的同题影响。但教师工作台和 AI 质量概览还缺少班级级闭环：老师无法快速判断 Coach 追问是否真的推动了后续提交改善，还是仍卡同类问题、等待后续证据或只是错因转移。

要把 AI 做成教育 agent，不能只评估“问得好不好”或“学生答得像不像理解了”，还要把追问后的真实提交变化沉淀成可观察、可评测、可迭代的质量信号。

## What Changes

- 在 `AssignmentOverviewResponse` 新增 `coachFollowupImpactSummary` 结构化字段。
- `ClassroomService` 基于 `CoachImpactAnalyzer` 的全班 impacts 汇总 Coach 追问后的后续提交成效。
- 输出 impacted、accepted、shifted、sameIssue、verdictChanged、noClearChange、awaitingFollowup 等计数，以及 `dominantOutcome`、summary、recommendedAction 和 evidenceRefs。
- 教师工作台展示 Coach 后续成效摘要，帮助老师判断继续自动追问、收集后续提交、还是升级教师介入。
- AI 质量概览新增 `COACH_FOLLOWUP_IMPACT_LOOP` 维度，把 Coach 追问后的真实后续提交成效纳入质量评测。
- 增加后端测试、前端类型/展示和浏览器 smoke 验证。

## Capabilities

### New Capabilities

- `coach-followup-impact-loop`: 覆盖 Coach 追问后的同题后续提交成效汇总、教师可见状态、AI 质量维度和测试验证。

### Modified Capabilities

无。

## Impact

- 后端：更新 `AssignmentOverviewResponse`、`ClassroomService`、`AiQualityMetrics`、`AiQualityOverviewService` 和相关测试。
- 前端：更新 `types.ts`、`TeacherPage.tsx`、`styles.css` 和 `browser-smoke.mjs`。
- API：`/api/teacher/assignments/{assignmentId}/overview` 新增兼容字段；`/api/teacher/assignments/{assignmentId}/ai-quality` 新增兼容质量维度。
- 数据：无数据库迁移。
