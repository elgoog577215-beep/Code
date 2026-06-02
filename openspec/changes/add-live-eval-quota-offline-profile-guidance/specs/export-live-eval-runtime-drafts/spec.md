## ADDED Requirements

### Requirement: Runtime fixture drafts expose offline profile guidance fields

系统 SHALL 在 live eval runtime fixture draft 中提供兼容的离线 profile 指导字段，供额度受限时的评测闭环消费。

#### Scenario: Draft schema carries offline profile guidance

- **WHEN** a live eval runtime fixture draft is exported
- **THEN** the draft MAY include `offlineProfileEvalRecommended`
- **AND** the draft MAY include `offlineProfileReportPattern`
- **AND** the draft MAY include `offlineProfileCaseId`
- **AND** the draft MAY include `offlineProfileRequiredChecks`

#### Scenario: Empty guidance stays compatible

- **WHEN** no offline profile eval guidance applies
- **THEN** `offlineProfileEvalRecommended` MUST be false
- **AND** offline profile string fields MUST be blank
- **AND** `offlineProfileRequiredChecks` MUST be empty
