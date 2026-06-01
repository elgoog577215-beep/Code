## ADDED Requirements

### Requirement: AI 质量概览必须输出提示安全事件信号
系统 SHALL 在作业 AI 质量概览中输出结构化 `promptSafetyIncidentSignal`，聚合诊断高泄题风险、安全降级记录和 Coach 安全风险。

#### Scenario: 作业存在安全降级记录
- **GIVEN** 作业内某次提交存在 `HintSafetyCheck.riskLevel` 为 `MEDIUM` 或 `HIGH`
- **WHEN** 教师读取 AI 质量概览
- **THEN** `promptSafetyIncidentSignal.safetyDowngradeCount` SHALL 大于 0
- **AND** `promptSafetyIncidentSignal.evidenceRefs` SHALL 包含对应安全检查或提交引用
- **AND** `promptSafetyIncidentSignal.recommendedAction` SHALL 建议复核提示安全降级原因

#### Scenario: 作业存在高泄题风险诊断
- **GIVEN** 作业内某条诊断的 `answerLeakRisk` 为 `HIGH`
- **WHEN** 系统生成提示安全事件信号
- **THEN** `promptSafetyIncidentSignal.highLeakRiskCount` SHALL 大于 0
- **AND** `promptSafetyIncidentSignal.status` SHALL 为 `ACTION_NEEDED`
- **AND** evidence refs SHALL 指向该诊断或提交证据

#### Scenario: 作业存在 Coach 安全风险回答
- **GIVEN** 作业内 Coach 回答质量信号为 `SAFETY_RISK`
- **WHEN** 系统生成提示安全事件信号
- **THEN** `promptSafetyIncidentSignal.coachSafetyRiskCount` SHALL 大于 0
- **AND** summary SHALL 说明需要把对话拉回证据层

### Requirement: AI 质量概览必须新增提示安全事件闭环维度
系统 SHALL 在 `qualityDimensions` 中输出 `PROMPT_SAFETY_INCIDENT_LOOP`，使教师能把安全事件纳入改进优先级。

#### Scenario: 存在需要处理的安全事件
- **GIVEN** `promptSafetyIncidentSignal.status` 为 `ACTION_NEEDED`
- **WHEN** 教师读取 AI 质量概览
- **THEN** `qualityDimensions` SHALL 包含 `PROMPT_SAFETY_INCIDENT_LOOP`
- **AND** 该维度 status SHALL 为 `ACTION_NEEDED`
- **AND** improvement priorities SHALL 包含该维度

#### Scenario: 没有安全事件
- **GIVEN** 作业内没有高泄题诊断、安全降级或 Coach 安全风险
- **WHEN** 教师读取 AI 质量概览
- **THEN** `PROMPT_SAFETY_INCIDENT_LOOP` status SHALL 为 `HEALTHY`
- **AND** summary SHALL 说明暂未观察到提示安全事件

### Requirement: 提示安全事件闭环必须保持兼容
系统 MUST 保持原有 `HINT_SAFETY`、高泄题计数和旧 AI 质量字段兼容，同时新增安全事件信号。

#### Scenario: 旧字段继续可用
- **WHEN** 作业 AI 质量概览包含 `promptSafetyIncidentSignal`
- **THEN** `highLeakRiskCount`、`highLeakRiskRate` 和 `HINT_SAFETY` SHALL 继续按原语义输出
- **AND** 前端 API 类型 SHALL 接受新增字段而不破坏旧消费方

### Requirement: 提示安全事件闭环必须可验证
系统 MUST 提供测试覆盖安全事件聚合、AI 质量维度、证据引用和类型兼容。

#### Scenario: 执行相关验证
- **WHEN** 运行 OpenSpec 严格校验、AI 质量后端测试、后端编译、前端 typecheck 和 diff 检查
- **THEN** 检查 SHALL 通过
