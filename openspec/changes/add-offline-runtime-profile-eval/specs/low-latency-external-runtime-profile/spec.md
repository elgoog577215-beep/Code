## ADDED Requirements

### Requirement: Low latency profile can be evaluated offline

系统 SHALL 支持在 provider 不可用或额度不足时离线验证 low-latency profile 的输入压缩效果。

#### Scenario: Offline eval supports quota-limited live eval

- **WHEN** live eval cannot complete a low-latency provider call because of quota or provider failure
- **THEN** maintainers MUST be able to run offline runtime profile eval
- **AND** the offline report MUST show whether low-latency requestBytes are lower than standard requestBytes for the same cases
