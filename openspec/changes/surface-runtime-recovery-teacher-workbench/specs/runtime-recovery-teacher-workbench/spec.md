## ADDED Requirements

### Requirement: 教师工作台必须展示外部模型恢复状态

教师工作台 SHALL 在 AI 质量“模型归因”区域展示 `runtimeAttributionSignal.recoveryStatus`，让教师区分外部模型仍阻塞、已有恢复证据或无恢复上下文。

#### Scenario: recovery blocked 显示阻塞状态

- **GIVEN** AI 质量概览响应包含 `runtimeAttributionSignal.recoveryStatus=BLOCKED`
- **WHEN** 教师查看 AI 质量信号
- **THEN** 页面 SHALL display 恢复 BLOCKED
- **AND** 页面 SHALL display recovery smoke case or profile when provided

#### Scenario: recovery recovered 显示恢复证据

- **GIVEN** AI 质量概览响应包含 `runtimeAttributionSignal.recoveryStatus=RECOVERED`
- **WHEN** 教师查看 AI 质量信号
- **THEN** 页面 SHALL display 恢复 RECOVERED
- **AND** 页面 SHALL display passed check count or evidence when provided

### Requirement: 教师工作台必须展示 recovery smoke 检查线索

教师工作台 SHALL 在 recovery blocked 时展示阻塞原因和 required checks，帮助维护者知道下一次真实模型 smoke 要验证什么。

#### Scenario: blocked reasons and required checks are present

- **GIVEN** `runtimeAttributionSignal.recoveryBlockedReasons` contains values
- **AND** `runtimeAttributionSignal.recoverySmokeRequiredChecks` contains values
- **WHEN** 教师查看模型归因
- **THEN** 页面 SHALL display at least one blocked reason
- **AND** 页面 SHALL display at least one required check

### Requirement: recovery 展示必须安全且兼容老数据

教师工作台 SHALL NOT 展示 API key、token、header、raw prompt、raw response 或 provider 原始 body，并且 SHALL tolerate missing recovery fields.

#### Scenario: recovery fields are absent

- **GIVEN** AI 质量概览响应没有 recovery fields
- **WHEN** 教师查看 AI 质量信号
- **THEN** 页面 SHALL continue displaying existing model attribution content
- **AND** typecheck SHALL pass
