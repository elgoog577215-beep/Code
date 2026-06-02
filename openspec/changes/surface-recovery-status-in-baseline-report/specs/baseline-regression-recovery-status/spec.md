## ADDED Requirements

### Requirement: model baseline regression report 必须暴露当前 recovery status

系统 SHALL 在 live-model-eval baseline regression report 中输出当前 report 的 recovery status summary。

#### Scenario: 当前 report recovery blocked

- **GIVEN** current live-model-eval report has `recoveryStatus=BLOCKED`
- **AND** current report has recovery blocked reasons
- **WHEN** model baseline regression report is built
- **THEN** the regression report SHALL include `currentRecoveryStatus=BLOCKED`
- **AND** it SHALL include current recovery check counts
- **AND** it SHALL include current recovery blocked reason count and blocked reasons

#### Scenario: assistant regression report 不混入 model recovery 字段

- **GIVEN** assistant live eval baseline regression report is built
- **WHEN** the report is serialized
- **THEN** model recovery status fields SHALL remain unset or empty

### Requirement: recovery status 不改变 baseline gate 判定

系统 SHALL keep baseline regression pass/fail semantics based on existing violations, while recovery status provides explanatory context.

#### Scenario: recovery blocked 但无 baseline violations

- **GIVEN** model baseline regression violations are empty
- **AND** current recovery status is `BLOCKED`
- **WHEN** regression report is built
- **THEN** report `status` SHALL still be `PASSED`
- **AND** `currentRecoveryStatus` SHALL still be `BLOCKED`
