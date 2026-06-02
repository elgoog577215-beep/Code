## ADDED Requirements

### Requirement: baseline regression report 必须输出可比性状态

系统 SHALL 在 live eval baseline regression report 中输出 `comparabilityStatus`，区分 gate pass/fail 与真实外部模型质量是否可比。

#### Scenario: recovery blocked makes model report not comparable

- **GIVEN** current model report has `currentRecoveryStatus=BLOCKED`
- **WHEN** 系统生成 baseline regression report
- **THEN** `comparabilityStatus` SHALL be `NOT_COMPARABLE`
- **AND** `comparabilityReasons` SHALL include `current recovery blocked`

#### Scenario: no compared cases makes report not comparable

- **GIVEN** baseline and current report have no overlapping case ids
- **WHEN** 系统生成 baseline regression report
- **THEN** `comparabilityStatus` SHALL be `NOT_COMPARABLE`
- **AND** `comparabilityReasons` SHALL include `no compared cases`

#### Scenario: comparable report remains comparable

- **GIVEN** baseline and current report have overlapping cases
- **AND** current recovery is not blocked
- **AND** model hits are present when source report is live model eval
- **WHEN** 系统生成 baseline regression report
- **THEN** `comparabilityStatus` SHALL be `COMPARABLE`

### Requirement: 可比性不得改变 gate pass/fail

系统 SHALL keep existing `status=PASSED/FAILED` semantics independent from `comparabilityStatus`.

#### Scenario: passed but not comparable

- **GIVEN** baseline regression has no violations
- **AND** current recovery is blocked
- **WHEN** 系统生成 baseline regression report
- **THEN** `status` SHALL be `PASSED`
- **AND** `comparabilityStatus` SHALL be `NOT_COMPARABLE`

### Requirement: 可比性报告必须安全

系统 SHALL NOT include API keys, tokens, headers, raw prompts, raw responses, or provider raw bodies in comparability reasons.

#### Scenario: comparability reasons are safe summaries

- **WHEN** 系统输出 `comparabilityReasons`
- **THEN** reasons SHALL contain short diagnostic summaries
- **AND** reasons SHALL NOT contain API key or token values
