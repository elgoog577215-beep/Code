## ADDED Requirements

### Requirement: Coach prompt 响应必须暴露自适应策略信号
系统 SHALL 在新生成的 Coach prompt 响应中输出结构化 `adaptiveStrategySignal`，说明本轮追问采用的策略、原因、建议教学动作、教师关注标记和证据引用。

#### Scenario: 新生成追问返回策略信号
- **WHEN** 学生请求生成下一轮 Coach 追问
- **THEN** 响应 SHALL 包含 `adaptiveStrategySignal.strategy`
- **AND** 响应 SHALL 包含 `reason`、`recommendedCoachMove`、`needsTeacherAttention` 和 `evidenceRefs`
- **AND** `evidenceRefs` SHALL 包含 `coach-strategy:<strategy>` 形式的机器可读引用

#### Scenario: 旧追问保持兼容
- **WHEN** 系统读取没有策略引用的旧 Coach prompt
- **THEN** 响应 SHALL 保持可读取
- **AND** `adaptiveStrategySignal` MAY 为空

### Requirement: Coach 必须根据历史学习证据选择下一问策略
系统 SHALL 基于学习动作证据、最近提交轨迹、重复错因和当前 verdict 选择 Coach 下一问策略，而不是只依赖单次诊断标签。

#### Scenario: 前次学习动作被后续证据反驳
- **GIVEN** 当前诊断包含学习动作执行状态为 `CONTRADICTED`
- **WHEN** 系统生成 Coach 下一问
- **THEN** `adaptiveStrategySignal.strategy` SHALL 为 `REDUCE_GRANULARITY`
- **AND** `needsTeacherAttention` SHALL 为 true
- **AND** 问题 SHALL 引导学生缩到最小失败样例或关键变量轨迹

#### Scenario: 同类细分错因反复出现
- **GIVEN** 最近多次提交包含相同细分错因标签
- **WHEN** 系统生成 Coach 下一问
- **THEN** `adaptiveStrategySignal.strategy` SHALL 为 `REDUCE_GRANULARITY`
- **AND** evidence refs SHALL 包含重复错因引用

#### Scenario: 当前提交已经通过
- **GIVEN** 当前提交 verdict 为 `ACCEPTED`
- **WHEN** 系统生成 Coach 下一问
- **THEN** `adaptiveStrategySignal.strategy` SHALL 为 `TRANSFER_REFLECTION`
- **AND** 问题 SHALL 引导学生做复杂度、边界样例或迁移复盘

### Requirement: Coach follow-up 必须根据学生回答质量调整策略
系统 SHALL 根据学生上一轮回答是否包含证据、是否为空、是否泄露完整答案倾向，选择下一轮 follow-up 策略。

#### Scenario: 学生回答含糊且缺少证据
- **GIVEN** 学生回答只有方向但没有样例、变量、输入输出或复杂度证据
- **WHEN** 系统生成 follow-up
- **THEN** `adaptiveStrategySignal.strategy` SHALL 为 `COLLECT_EVIDENCE`
- **AND** 问题 SHALL 要求学生补充最小样例、变量轨迹、输入输出对照或复杂度数量级

#### Scenario: 学生回答已经包含证据
- **GIVEN** 学生回答包含样例、变量、输入输出、边界或复杂度证据
- **WHEN** 系统生成 follow-up
- **THEN** `adaptiveStrategySignal.strategy` SHALL 为 `VERIFY_MINIMAL_CHANGE`
- **AND** 问题 SHALL 引导学生只做一个最小修改并预测评测现象

#### Scenario: 学生回答出现答案或完整代码倾向
- **GIVEN** 学生回答包含完整代码、答案或明显直接改法倾向
- **WHEN** 系统生成 follow-up
- **THEN** `adaptiveStrategySignal.strategy` SHALL 为 `SAFETY_RESET`
- **AND** 问题 SHALL 把学生拉回到输入特征、证据或测试现象
- **AND** 问题 SHALL NOT 直接给出完整改法或答案

### Requirement: 自适应策略必须进入模型上下文和规则 fallback
系统 MUST 确保模型生成路径和规则 fallback 路径消费同一个自适应策略信号。

#### Scenario: 模型生成 Coach 追问
- **WHEN** `CoachAgentService` 调用外部模型生成追问
- **THEN** 模型上下文 SHALL 包含自适应策略说明或可读策略摘要
- **AND** 系统提示 SHALL 要求模型优先执行该教学策略

#### Scenario: 规则 fallback 生成 Coach 追问
- **WHEN** 模型不可用或模型输出不安全
- **THEN** 规则生成的问题 SHALL 与 `adaptiveStrategySignal.strategy` 对应
- **AND** rationale/evidence refs SHALL 保留该策略证据

### Requirement: 自适应策略闭环必须可验证
系统 MUST 提供测试覆盖策略选择、证据引用、兼容响应和安全边界。

#### Scenario: 执行相关验证
- **WHEN** 运行 OpenSpec 严格校验、Coach prompt 后端测试、相关 Coach agent 测试、后端编译、前端 typecheck 和 diff 检查
- **THEN** 检查 SHALL 通过
