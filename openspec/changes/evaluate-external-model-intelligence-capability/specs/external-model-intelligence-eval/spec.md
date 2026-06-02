## ADDED Requirements

### Requirement: 外接模型智能评测必须只统计真实模型完成结果

系统 SHALL separate real external model intelligence from local fallback, rule-based hits, and runtime recovery signals.

#### Scenario: fallback is excluded from intelligence score

- **GIVEN** a complex live eval entry uses fallback
- **WHEN** the report summarizes external model intelligence
- **THEN** the entry SHALL remain visible in runtime status
- **AND** it SHALL NOT increase intelligence completed count
- **AND** it SHALL NOT increase intelligence metric passed count
- **AND** it SHALL NOT produce an intelligence quality baseline

#### Scenario: completed model output is counted

- **GIVEN** a complex live eval entry has `modelCompleted=true`
- **AND** `fallbackUsed=false`
- **WHEN** the report summarizes external model intelligence
- **THEN** the entry SHALL be counted as an intelligence evaluated case
- **AND** its mapped intelligence metrics SHALL contribute to the intelligence score

### Requirement: complex live eval 必须有 14 条代表性外接模型智能基准

系统 SHALL provide a stable representative complex live set for external model intelligence evaluation.

#### Scenario: representative complex live set is selected

- **WHEN** the representative intelligence eval set is requested
- **THEN** it SHALL contain exactly 14 `complex-live-*` cases
- **AND** it SHALL cover 14 distinct bug patterns
- **AND** it SHALL include `complex-live-01-*` through `complex-live-14-*`

### Requirement: live intelligence report 必须暴露 AI 能力画像

系统 SHALL summarize external model intelligence metrics separately from generic live model eval metrics.

#### Scenario: intelligence report summary is built

- **WHEN** a live model eval report contains complex entries
- **THEN** the report SHALL include intelligence case count
- **AND** it SHALL include intelligence completed count
- **AND** it SHALL include intelligence quality passed count
- **AND** it SHALL include intelligence metric passed and total count
- **AND** it SHALL include per-metric pass and fail distributions
- **AND** the console summary SHALL include the intelligence aggregate

### Requirement: 智能指标必须对应教育诊断能力

系统 SHALL map complex quality metrics to externally visible model intelligence capabilities.

#### Scenario: intelligence metric mapping is exposed

- **WHEN** a complex live eval entry is scored
- **THEN** `primaryRootCauseHit` SHALL map to `autonomousRootCauseDiscovery`
- **AND** `secondaryIssuesNotOverweighted` SHALL map to `complexSignalPrioritization`
- **AND** `evidenceGrounded` SHALL map to `evidenceGroundedReasoning`
- **AND** `teachingPriorityCorrect` SHALL map to `teachingDecisionQuality`
- **AND** `distractingSignalsIgnored` SHALL map to `distractorResistance`
- **AND** `noFullSolutionLeak` SHALL map to `modelSafetyAndBoundary`

### Requirement: 复杂诊断提示协议必须强调主错因自主判断

系统 SHALL instruct the external diagnosis model to prioritize the teachable root cause before secondary issues in complex submissions.

#### Scenario: prompt protocol guides complex diagnosis

- **WHEN** the diagnosis prompt is built
- **THEN** it SHALL tell the model to choose the most teachable primary root cause first
- **AND** it SHALL require evidence refs for the primary diagnosis
- **AND** it SHALL tell the model not to promote distracting signals to the primary cause
- **AND** it SHALL preserve the existing standard library schema and output schema
