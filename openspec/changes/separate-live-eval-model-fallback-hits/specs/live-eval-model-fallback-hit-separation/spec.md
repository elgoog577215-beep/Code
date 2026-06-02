## ADDED Requirements

### Requirement: Live model eval separates model and fallback hits

系统 SHALL 在 live-model-eval report 中区分真实外部模型命中与规则 fallback 命中，同时保留最终诊断命中计数。

#### Scenario: Model completed hit counts as model hit

- **WHEN** live-model-eval entry has `status=MODEL_COMPLETED`
- **AND** `fallbackUsed=false`
- **AND** final analysis hits an expected issue or fine tag
- **THEN** the entry MUST set the corresponding model hit flag to true
- **AND** report model hit count MUST include the hit
- **AND** fallback hit count MUST NOT include the hit

#### Scenario: Partial model hit counts as model hit

- **WHEN** live-model-eval entry has `status=MODEL_PARTIAL_COMPLETED`
- **AND** `fallbackUsed=false`
- **AND** retained diagnosis hits an expected issue or fine tag
- **THEN** the entry MUST set `modelCompleted=true`
- **AND** report model hit count MUST include the retained hit

#### Scenario: Runtime fallback hit counts only as fallback hit

- **WHEN** live-model-eval entry has `fallbackUsed=true`
- **AND** final fallback analysis hits an expected issue or fine tag
- **THEN** the entry MUST set the corresponding final hit flag to true
- **AND** the entry MUST set the corresponding model hit flag to false
- **AND** the entry MUST set the corresponding fallback hit flag to true
- **AND** report fallback hit count MUST include the hit
- **AND** report model hit count MUST NOT include the hit

### Requirement: Live model eval reports model-vs-fallback summary counts

系统 SHALL 在 report summary 中输出模型命中、fallback 命中和最终命中，避免把规则兜底误读为外部模型质量。

#### Scenario: Summary exposes separate hit counts

- **WHEN** live-model-eval report is summarized
- **THEN** report MUST include final `issueTagHitCount` and `fineTagHitCount`
- **AND** report MUST include `modelIssueTagHitCount` and `modelFineTagHitCount`
- **AND** report MUST include `fallbackIssueTagHitCount` and `fallbackFineTagHitCount`

#### Scenario: Quota fallback does not inflate model quality

- **WHEN** all live-model-eval entries are runtime fallbacks caused by quota or rate limit
- **THEN** model hit counts MUST be zero
- **AND** fallback hit counts MAY be greater than zero when rule fallback preserved expected tags
