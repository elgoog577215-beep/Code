## ADDED Requirements

### Requirement: 推荐效果必须输出行动证据状态
系统 SHALL 为 recommendation token 生成结构化行动证据，判断推荐行动契约是否被执行和兑现。

#### Scenario: 推荐后提交通过
- **GIVEN** 一个推荐 token 有曝光和后续提交事件
- **AND** 后续提交 verdict 为 `ACCEPTED`
- **WHEN** 教师或系统读取推荐效果概览
- **THEN** `actionEvidenceSignals` SHALL 包含该 token
- **AND** outcome SHALL 为 `CONTRACT_FULFILLED`
- **AND** evidenceRefs SHALL 包含 recommendation token 和 follow-up submission 引用

#### Scenario: 推荐后仍命中同类错因
- **GIVEN** 一个推荐 token 的 focus tags 包含某细分错因
- **AND** 后续提交仍命中相同 issue 或 fine-grained tag
- **WHEN** 系统生成行动证据
- **THEN** outcome SHALL 为 `UNRESOLVED_SAME_FOCUS`
- **AND** recommendedAdjustment SHALL 建议降级到最小样例复盘或教师介入

#### Scenario: 点击或进入后没有提交
- **GIVEN** 一个推荐 token 有点击或进入题目事件
- **AND** 没有后续提交事件
- **WHEN** 系统生成行动证据
- **THEN** outcome SHALL 为 `NO_FOLLOWUP_SUBMISSION`
- **AND** summary SHALL 指出行动证据缺失

### Requirement: 推荐行动证据必须支持教师介入判断
系统 SHALL 在高风险推荐后仍失败或同类错因未解决时标记教师关注。

#### Scenario: 高风险推荐后仍失败
- **GIVEN** 一个推荐 token riskLevel 为 `HIGH`
- **AND** 后续提交未通过
- **WHEN** 系统生成行动证据
- **THEN** needsTeacherAttention SHALL 为 true
- **AND** outcome SHALL 为 `TEACHER_INTERVENTION_NEEDED` 或保留同类错因 outcome 并带教师关注标记

### Requirement: 下一轮推荐必须消费行动证据
系统 SHALL 使用最近推荐行动证据调整下一轮推荐策略。

#### Scenario: 最近推荐行动未兑现
- **GIVEN** 学生最近推荐行动证据 outcome 为 `UNRESOLVED_SAME_FOCUS`
- **WHEN** 系统生成下一轮推荐
- **THEN** 推荐列表 SHALL 优先包含降级复盘或教师介入推荐
- **AND** 推荐 summary SHALL 说明推荐后行动契约未兑现

### Requirement: AI 质量概览必须使用推荐行动证据
AI 质量概览 SHALL 将推荐行动证据纳入 `RECOMMENDATION_LOOP` 维度和 evidence refs。

#### Scenario: 推荐行动证据显示未解决
- **GIVEN** 推荐行动证据存在 `UNRESOLVED_SAME_FOCUS` 或教师关注
- **WHEN** 教师读取 AI 质量概览
- **THEN** `RECOMMENDATION_LOOP` SHALL 标记为需要处理
- **AND** evidenceRefs SHALL 包含对应 recommendation token

### Requirement: 推荐行动证据闭环必须可验证
系统 MUST 提供测试覆盖 action evidence outcome、下一轮推荐调整、质量维度和类型兼容。

#### Scenario: 执行验证
- **WHEN** 运行 OpenSpec 严格校验、推荐相关后端测试、AI 质量测试、后端编译、前端 typecheck 和 diff 检查
- **THEN** 检查 SHALL 通过
