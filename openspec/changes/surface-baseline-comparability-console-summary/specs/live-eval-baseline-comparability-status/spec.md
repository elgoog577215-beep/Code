## ADDED Requirements

### Requirement: comparability 状态必须进入人类可见摘要
系统 SHALL 将 live eval baseline regression 的 `comparabilityStatus` 写入 JSON report，并在控制台摘要中展示。

#### Scenario: JSON 和控制台都包含 comparability
- **WHEN** baseline regression report has `comparabilityStatus`
- **THEN** structured JSON report SHALL include `comparabilityStatus`
- **AND** console summary SHALL include the same comparability status
