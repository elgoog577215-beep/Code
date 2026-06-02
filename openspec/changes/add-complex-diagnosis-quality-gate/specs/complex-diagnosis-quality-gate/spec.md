## ADDED Requirements

### Requirement: 复杂诊断输出必须被逐项评分

系统 SHALL score generated complex diagnosis outputs against fixture truth using the expected complex metrics.

#### Scenario: complex diagnosis quality is scored

- **WHEN** a complex fixture and a diagnosis analysis are evaluated
- **THEN** the system SHALL compute `primaryRootCauseHit`
- **AND** it SHALL compute `teachingPriorityCorrect`
- **AND** it SHALL compute `secondaryIssuesNotOverweighted`
- **AND** it SHALL compute `distractingSignalsIgnored`
- **AND** it SHALL compute `evidenceGrounded`
- **AND** it SHALL compute `noFullSolutionLeak`
- **AND** it SHALL expose failed metric names when any metric fails

### Requirement: live model eval 必须汇总复杂质量门禁

系统 SHALL include complex diagnosis quality metrics in live model eval reports for generated complex cases.

#### Scenario: complex live report summary is built

- **WHEN** live model eval summarizes entries that include complex quality metrics
- **THEN** the report SHALL include complex case count
- **AND** it SHALL include complex quality passed case count
- **AND** it SHALL include complex metric passed count and total count
- **AND** its console summary SHALL include the complex quality aggregate

### Requirement: complex quality baseline 必须防止能力回退

系统 SHALL preserve successful complex quality signals in quality baseline drafts and check them during model baseline regression.

#### Scenario: complex quality baseline is generated

- **WHEN** a live model eval entry completes a complex case and passes the complex quality gate
- **THEN** the quality baseline draft SHALL include the passed complex metric signals
- **AND** the regression gate SHALL fail if a later report no longer preserves those metric signals
