## ADDED Requirements

### Requirement: Model/fallback hit separation is consumed by quality baselines

系统 SHALL 在 live-model-eval quality baseline 和 regression gate 中消费 model/fallback hit separation，而不仅仅在 report 中展示。

#### Scenario: Baseline factory ignores fallback hit fields

- **WHEN** model baseline factory evaluates an entry
- **THEN** `fallbackIssueTagHit` and `fallbackFineTagHit` MUST NOT make the entry eligible for model quality baseline

#### Scenario: Regression gate distinguishes final and model hit

- **WHEN** current entry has `expectedIssueTagHit=true`
- **AND** `modelIssueTagHit=false`
- **AND** baseline requires model issue hit
- **THEN** regression gate MUST fail the model hit requirement
