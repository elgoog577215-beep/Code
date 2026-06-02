## ADDED Requirements

### Requirement: baseline regression 控制台必须展示可比性摘要
系统 SHALL 在 live eval baseline regression report 写出后打印一行包含 pass/fail 和 comparability 的摘要。

#### Scenario: model baseline report passed but not comparable
- **WHEN** model baseline regression report has `status=PASSED`
- **AND** `comparabilityStatus=NOT_COMPARABLE`
- **THEN** console summary SHALL include `status=PASSED`
- **AND** console summary SHALL include `comparability=NOT_COMPARABLE`
- **AND** console summary SHALL include comparability reason count

#### Scenario: assistant baseline report prints same summary shape
- **WHEN** assistant baseline regression report is written
- **THEN** console summary SHALL include `status=`
- **AND** console summary SHALL include `comparability=`
- **AND** console summary SHALL include compared case and violation counts

### Requirement: 可比性摘要不得泄露敏感信息
系统 SHALL only print structured report metadata in the baseline regression console summary.

#### Scenario: summary uses safe fields only
- **WHEN** baseline regression summary is generated
- **THEN** it SHALL NOT include API keys, tokens, headers, raw prompts, raw responses, or provider raw bodies
