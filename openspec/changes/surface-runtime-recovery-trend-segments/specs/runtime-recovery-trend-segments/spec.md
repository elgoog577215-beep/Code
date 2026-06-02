## ADDED Requirements

### Requirement: 跨作业来源片段必须输出 recovery 状态

系统 SHALL 在 AI 质量趋势 `sourceSegments` 中输出外部模型 recovery 状态，帮助教师和维护者判断某个来源片段是否仍然 runtime blocked。

#### Scenario: source segment remains blocked

- **GIVEN** source segment contains `MODEL_RUNTIME_FALLBACK` or fallback samples
- **AND** no sample in the segment satisfies recovery checks
- **WHEN** 系统生成 AI 质量趋势
- **THEN** the source segment SHALL include `recoveryStatus=BLOCKED`
- **AND** `recoveryBlockedReasons` SHALL include at least one missing check or runtime failure reason

#### Scenario: source segment has recovered sample

- **GIVEN** source segment has recovery context
- **AND** at least one sample satisfies recovery checks
- **WHEN** 系统生成 AI 质量趋势
- **THEN** the source segment SHALL include `recoveryStatus=RECOVERED`
- **AND** `recoveryPassedCheckCount` SHALL be greater than zero

### Requirement: 教师工作台来源质量必须展示 recovery 状态

教师工作台 SHALL 在跨作业来源质量列表中展示 source segment recovery 状态和阻塞原因摘要。

#### Scenario: source segment recovery blocked appears in UI

- **GIVEN** source segment has `recoveryStatus=BLOCKED`
- **WHEN** 教师查看跨作业 AI 质量趋势
- **THEN** 页面 SHALL display 恢复 BLOCKED
- **AND** 页面 SHALL display at least one blocked reason when provided

### Requirement: recovery trend segment 必须安全兼容

系统 SHALL tolerate missing recovery fields and SHALL NOT expose API keys, tokens, headers, raw prompts, raw responses, or provider raw bodies.

#### Scenario: old trend response has no recovery fields

- **GIVEN** source segment response lacks recovery fields
- **WHEN** 教师查看来源质量
- **THEN** existing source segment content SHALL continue rendering
- **AND** frontend typecheck SHALL pass
