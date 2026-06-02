## ADDED Requirements

### Requirement: Quota fallback recommends offline profile eval

系统 SHALL 在 live-model-eval 因 provider 额度不足进入 runtime fallback 时，为 runtime fixture draft 生成离线 runtime profile eval 指导。

#### Scenario: Quota fallback draft includes offline profile guidance

- **WHEN** live-model-eval entry has `failureReason` containing `INSUFFICIENT_QUOTA`
- **AND** generated runtime fixture draft has `failureType=QUOTA_LIMIT`
- **THEN** `offlineProfileEvalRecommended` MUST be true
- **AND** `offlineProfileReportPattern` MUST point to `target/ai-eval-reports/offline-runtime-profile-eval-*.json`
- **AND** `offlineProfileCaseId` MUST match the live eval case id
- **AND** `offlineProfileRequiredChecks` MUST include request byte reduction, compact marker, compression ratio and structural anchors

#### Scenario: Quota action mentions offline profile before retrying broad live eval

- **WHEN** quota fallback runtime fixture draft is generated
- **THEN** `expectedRuntimeAction` MUST mention ModelScope quota
- **AND** `expectedRuntimeAction` MUST mention running offline runtime profile eval while quota is unavailable
- **AND** `iterationSuggestion` MUST include the offline profile report pattern

### Requirement: Offline profile guidance remains scoped and safe

系统 SHALL 保证 quota offline profile 指导只记录安全元数据，不持久化敏感上下文或 provider payload。

#### Scenario: Guidance avoids raw request and secrets

- **WHEN** quota fallback runtime fixture draft is serialized
- **THEN** serialized JSON MUST NOT contain raw request JSON, chat messages, source code, actual API Key value, Authorization header value, bearer token value or ModelScope token

#### Scenario: Non quota runtime failures do not recommend offline profile

- **WHEN** live-model-eval runtime fixture draft is generated for a non quota failure
- **THEN** `offlineProfileEvalRecommended` MUST be false
- **AND** offline profile report fields MUST remain empty
