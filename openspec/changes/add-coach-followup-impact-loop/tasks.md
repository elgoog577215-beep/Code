## 1. 响应结构

- [x] 1.1 在 `AssignmentOverviewResponse` 新增 `CoachFollowupImpactSummary` 字段。
- [x] 1.2 在前端 `AssignmentOverview` 类型新增 `coachFollowupImpactSummary`。

## 2. 作业概览后端汇总

- [x] 2.1 在 `ClassroomService` 基于 `CoachImpactAnalyzer` 的全班 impacts 汇总 Coach 后续提交成效。
- [x] 2.2 输出 impacted、accepted、shifted、sameIssue、verdictChanged、noClearChange、awaitingFollowup 等计数。
- [x] 2.3 输出 dominantOutcome、summary、recommendedAction 和 evidenceRefs。

## 3. AI 质量闭环

- [x] 3.1 在 `AiQualityMetrics` 统计 Coach follow-up impact 成效计数与比例。
- [x] 3.2 在 `AiQualityOverviewService` 新增 `COACH_FOLLOWUP_IMPACT_LOOP` 质量维度。
- [x] 3.3 将该维度纳入 improvement priorities 排序和 evidence refs。

## 4. 教师端展示

- [x] 4.1 在教师工作台展示 Coach 后续成效摘要卡片。
- [x] 4.2 补充卡片样式、移动端约束和暗色主题兼容。
- [x] 4.3 更新浏览器 smoke mock 和选择器检查。

## 5. 验证

- [x] 5.1 扩展后端测试，覆盖追问后通过、错因转移、仍卡同类和等待后续聚合。
- [x] 5.2 扩展 AI 质量测试，覆盖 `COACH_FOLLOWUP_IMPACT_LOOP` 的状态、分数、证据和优先级。
- [x] 5.3 运行 OpenSpec 严格校验、相关后端测试、后端编译、前端 typecheck、浏览器视觉检查和 diff 检查。
