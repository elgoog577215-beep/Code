## ADDED Requirements

### Requirement: 作业概览必须汇总 Coach 后续提交成效
系统 SHALL 在作业概览中输出班级级 Coach 后续提交成效摘要。

#### Scenario: 输出 Coach 后续成效摘要
- **WHEN** 教师读取作业概览
- **THEN** 响应 SHALL 包含 `coachFollowupImpactSummary`
- **AND** 该字段 SHALL 包含 impacted、accepted、shifted、sameIssue、verdictChanged、noClearChange 和 awaitingFollowup 计数

### Requirement: Coach 后续成效汇总必须给出主导结果和行动建议
系统 SHALL 基于 Coach 追问后的同题后续提交结果生成主导结果、摘要和教师行动建议。

#### Scenario: 追问后仍卡同类问题
- **GIVEN** 多个 Coach impact 状态为 `SAME_ISSUE`
- **WHEN** 教师读取作业概览
- **THEN** `coachFollowupImpactSummary.dominantOutcome` SHALL 表示仍卡同类问题
- **AND** summary/recommendedAction SHALL 提醒教师降低追问颗粒度、补最小失败样例或介入检查

#### Scenario: 追问后通过
- **GIVEN** 多个 Coach impact 状态为 `FOLLOWUP_ACCEPTED`
- **WHEN** 教师读取作业概览
- **THEN** `coachFollowupImpactSummary.dominantOutcome` SHALL 表示追问后通过
- **AND** summary/recommendedAction SHALL 提醒教师沉淀有效追问样本

### Requirement: Coach 后续成效汇总必须暴露证据引用
系统 SHALL 为 Coach 后续成效摘要提供可追溯 evidence refs。

#### Scenario: 生成后续成效证据引用
- **GIVEN** 学生存在 Coach impact
- **WHEN** 系统生成 `coachFollowupImpactSummary`
- **THEN** evidenceRefs SHALL 包含若干 coached submission、followup submission 或 impact status ref

### Requirement: AI 质量概览必须包含 Coach 后续成效闭环维度
AI 质量概览 SHALL 增加 Coach 后续成效闭环维度，用于评估 Coach 追问是否带来可观察的后续提交改善。

#### Scenario: 质量维度识别仍卡同类问题
- **GIVEN** Coach impact 中存在 `SAME_ISSUE`
- **WHEN** 教师读取 AI 质量概览
- **THEN** `qualityDimensions` SHALL 包含 `COACH_FOLLOWUP_IMPACT_LOOP`
- **AND** 该维度 SHALL 标记为需要处理或观察
- **AND** improvement priorities SHALL 可引用该维度

### Requirement: 教师工作台必须展示 Coach 后续成效摘要
教师工作台 SHALL 展示班级级 Coach 后续成效摘要，帮助老师判断继续自动追问、收集后续提交或升级教师介入。

#### Scenario: 展示 Coach 后续成效卡片
- **WHEN** 作业概览包含 `coachFollowupImpactSummary`
- **THEN** 页面 SHALL 展示追问影响、追问后通过、错因转移、仍卡同类、等待后续等计数
- **AND** 页面 SHALL 展示 summary 或 recommendedAction

### Requirement: Coach 后续成效闭环必须可验证
系统 MUST 提供后端测试和前端验证覆盖新增汇总字段、AI 质量维度与展示。

#### Scenario: 执行验证
- **WHEN** 运行相关后端测试、后端编译、前端 typecheck、浏览器视觉检查和 diff 检查
- **THEN** 检查 SHALL 通过
