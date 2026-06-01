## Context

当前系统已经有两层 Coach 能力：

- `CoachAnswerQualityAnalyzer` 判断学生是否把追问回答成最小样例、输出对比、变量轨迹、复杂度估算或迁移解释。
- `CoachImpactAnalyzer` 已能为单个 coached submission 判断同题后续提交状态，例如 `FOLLOWUP_ACCEPTED`、`ISSUE_SHIFTED`、`SAME_ISSUE`、`AWAITING_FOLLOWUP`。

缺口在聚合和质量闭环：教师端只能逐个学生看 `latestCoachImpact`，AI 质量概览也只评估 Coach 回答是否可验证，没有把追问后的真实提交变化变成班级级质量指标。这样会出现一个盲点：学生可能答得像理解了，但下一次提交仍卡同类问题；或者追问有效推动通过，却没有被系统沉淀为可复用证据。

## Goals / Non-Goals

**Goals:**

- 新增班级级 `coachFollowupImpactSummary`，汇总 Coach 追问后的同题后续提交成效。
- 输出 accepted、shifted、sameIssue、verdictChanged、noClearChange、awaitingFollowup 等计数。
- 输出 dominantOutcome、summary、recommendedAction 和 evidenceRefs。
- 在教师工作台展示 Coach 后续成效摘要。
- 在 AI 质量概览新增 `COACH_FOLLOWUP_IMPACT_LOOP` 维度，把追问后的后续提交成效纳入质量评测和改进优先级。
- 用后端测试和浏览器 smoke 覆盖新增闭环。

**Non-Goals:**

- 不重写 Coach 问题生成逻辑。
- 不改变 `CoachImpactAnalyzer` 的单条影响判断语义。
- 不新增数据库表。
- 不把观察性改善等同于因果证明；文案保留“观察性成效”边界。

## Decisions

### Decision 1: 复用 CoachImpactAnalyzer，不重复推断后续提交

`CoachImpactAnalyzer` 已经封装了“回答追问后下一次同题提交”的判断逻辑。新的班级汇总只消费它的输出，避免在 `ClassroomService` 或 `AiQualityOverviewService` 里重复实现同题后续提交查找。

### Decision 2: 作业概览显示班级即时成效，AI 质量概览评估系统闭环质量

`coachFollowupImpactSummary` 放在 assignment overview，服务教师当前课堂判断；`COACH_FOLLOWUP_IMPACT_LOOP` 放在 AI 质量概览，服务系统质量迭代。两者共享状态定义，但面向不同问题：

- 教师看板回答“这次课堂 Coach 追问后怎么样”。
- AI 质量回答“Coach 追问是否稳定带来可观察学习改进”。

### Decision 3: dominantOutcome 用教学风险优先级

主导结果优先级：

1. `SAME_ISSUE`：追问后仍卡同类问题，说明追问颗粒度或证据任务不足。
2. `AWAITING_FOLLOWUP`：学生已回答追问但没有后续同题提交，缺成效证据。
3. `ISSUE_SHIFTED` / `VERDICT_CHANGED` / `NO_CLEAR_CHANGE`：进入新阶段或结果不清晰，需要继续诊断。
4. `FOLLOWUP_ACCEPTED`：追问后同题通过，适合沉淀为有效追问样本。

这个优先级让教师先处理“仍卡住”和“缺后续证据”，再沉淀成功案例。

### Decision 4: AI 质量维度先用计数和比例，不新增复杂统计模型

第一版用 impacts 计数、accepted rate、same issue count、awaiting count 计算状态和 score。这样可解释、可测试，也能和现有质量维度保持一致。未来如需要更强因果判断，可再引入对照组或跨题迁移验证。

## Risks / Trade-offs

- [Risk] 同题下一次提交是观察性指标，不一定由 Coach 追问直接导致。-> Mitigation: summary 和 recommendedAction 使用“观察性改善/后续成效”表述，不声明因果证明。
- [Risk] 只看同题后续提交可能遗漏跨题迁移。-> Mitigation: 第一版关注最可靠的短链路成效；跨题迁移已有 post-AC/mastery growth 维度承接。
- [Risk] 等待后续提交可能在课堂中短暂偏高。-> Mitigation: 单独输出 awaitingFollowup，不把它混同为失败；行动建议是收集后续证据。
