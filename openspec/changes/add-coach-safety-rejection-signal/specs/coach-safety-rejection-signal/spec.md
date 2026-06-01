## ADDED Requirements

### Requirement: Coach 安全拒绝必须持久化

系统 SHALL 在 Coach 模型追问草稿被安全门拒绝并回退到规则追问时，持久化结构化安全拒绝元数据。

#### Scenario: 模型追问被安全拒绝后保存规则追问

- **GIVEN** Coach 模型草稿触发安全门拒绝
- **WHEN** 系统保存最终 `CoachPrompt`
- **THEN** 保存的 prompt SHALL 保持安全的规则追问文本
- **AND** prompt SHALL 保存 `modelFailureReason=SAFETY_REJECTED`
- **AND** prompt SHALL 保存模型草稿的 `modelAnswerLeakRisk`

### Requirement: Coach prompt 响应必须暴露模型安全元数据

系统 SHALL 在 Coach prompt 响应中返回模型失败原因和模型泄题风险，供后续前端和教师端消费。

#### Scenario: 读取发生过安全回退的 Coach prompt

- **WHEN** API 返回某条发生过安全回退的 Coach prompt
- **THEN** 响应 SHALL 包含 `modelFailureReason`
- **AND** 响应 SHALL 包含 `modelAnswerLeakRisk`
- **AND** 响应中的学生问题 SHALL 仍为安全规则追问

### Requirement: Coach 交互摘要必须识别安全拒绝信号

系统 SHALL 在 Coach 交互摘要中识别模型追问安全拒绝事件，并输出独立于学生回答质量的结构化信号。

#### Scenario: 提交下存在安全拒绝 prompt

- **GIVEN** 某次提交下存在 `modelFailureReason=SAFETY_REJECTED` 的 Coach prompt
- **WHEN** 系统汇总该提交的 Coach 交互
- **THEN** 摘要 SHALL 包含 `coachSafetyRejectionSignal`
- **AND** 信号 SHALL 包含安全拒绝计数、最近原因和 evidenceRefs
- **AND** 信号 SHALL 标记 `needsTeacherAttention=true`

#### Scenario: 提交下没有安全拒绝 prompt

- **WHEN** 系统汇总没有安全拒绝记录的 Coach 交互
- **THEN** `coachSafetyRejectionSignal` SHALL 表示健康或为空计数
- **AND** 原有学生回答质量信号 SHALL 保持兼容
