## ADDED Requirements

### Requirement: Rate-limited live model eval uses quota guidance

系统 SHALL 将 live-model-eval 的 429/rate-limited 运行失败视为额度或限流受限，并生成 quota offline profile guidance。

#### Scenario: Rate limited draft is quota limit

- **WHEN** live-model-eval entry has `failureReason` containing `RATE_LIMITED`
- **AND** generated runtime fixture draft is exported
- **THEN** the draft MUST set `failureType=QUOTA_LIMIT`
- **AND** the draft MUST set `offlineProfileEvalRecommended=true`
- **AND** the expected runtime action MUST mention ModelScope quota or rate limit
- **AND** the expected runtime action MUST mention offline runtime profile eval

#### Scenario: Provider non-quota error remains provider error

- **WHEN** live-model-eval entry has a provider error that is not quota, rate limit or 429
- **THEN** the draft MUST keep `failureType=PROVIDER_ERROR`
- **AND** `offlineProfileEvalRecommended` MUST be false
