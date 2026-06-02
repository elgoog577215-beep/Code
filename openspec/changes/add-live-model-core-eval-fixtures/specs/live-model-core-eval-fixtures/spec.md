## ADDED Requirements

### Requirement: live-model-eval 必须有核心可比较诊断样本集

系统 SHALL provide an auditable live-model core diagnosis fixture set for real external model baseline and recovery smoke runs.

#### Scenario: core fixture set is loaded

- **GIVEN** `diagnosis-eval-fixtures/live-model-core-cases.json`
- **WHEN** live-model eval cases are loaded
- **THEN** the core set SHALL contain at least 6 cases
- **AND** each case SHALL include a stable `caseId`
- **AND** each case SHALL include expected issue tags and expected fine-grained tags
- **AND** each case SHALL include required evidence refs

#### Scenario: core fixture enters live-model eval

- **WHEN** `ModelDiagnosisEvalTest` builds all eval cases
- **THEN** it SHALL include the built-in smoke cases
- **AND** it SHALL include teacher correction cases
- **AND** it SHALL include live-model core fixture cases

### Requirement: core fixture 必须服务可比较 baseline

系统 SHALL structure each live-model core case so successful model outputs can become quality baselines and failed model outputs can become runtime fixture drafts.

#### Scenario: model completes a core case

- **GIVEN** a live-model core case completes with `modelCompleted=true`
- **AND** `fallbackUsed=false`
- **WHEN** the report is summarized
- **THEN** the quality baseline draft SHALL be able to include model issue or fine-grained hits
- **AND** evidence validity SHALL be checked against the case's evidence requirements

#### Scenario: model falls back on a core case

- **GIVEN** a live-model core case falls back
- **WHEN** the report is summarized
- **THEN** runtime fixture drafts SHALL preserve the case id and failure reason
- **AND** recovery blocked reasons SHALL mention the case id
