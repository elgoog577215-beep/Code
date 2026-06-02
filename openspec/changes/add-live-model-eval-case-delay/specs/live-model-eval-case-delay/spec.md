## ADDED Requirements

### Requirement: live-model-eval 支持样本间冷却

系统 SHALL allow live-model-eval to wait between model diagnosis cases using `AI_EVAL_CASE_DELAY_MS`.

#### Scenario: case delay is configured

- **WHEN** `AI_EVAL_CASE_DELAY_MS` is set to a positive number
- **AND** live-model-eval runs more than one case
- **THEN** the eval SHALL wait before starting each case after the first one
- **AND** per-case `latencyMs` SHALL measure the model diagnosis call rather than the waiting time

### Requirement: live-model-eval 报告沉淀冷却配置

系统 SHALL include the live-model-eval case delay configuration in the structured report and console summary.

#### Scenario: report exposes case delay

- **GIVEN** a live-model-eval report is summarized with `caseDelayMs=1500`
- **AND** three cases were evaluated
- **WHEN** the report is written
- **THEN** top-level `caseDelayMs` SHALL be `1500`
- **AND** top-level `delayedCaseCount` SHALL be `2`
- **AND** the console summary SHALL include `caseDelayMs=1500`
- **AND** the console summary SHALL include `delayedCases=2`

#### Scenario: no delay keeps zero delayed cases

- **GIVEN** `AI_EVAL_CASE_DELAY_MS` is missing, zero, negative, or invalid
- **WHEN** live-model-eval is summarized
- **THEN** top-level `caseDelayMs` SHALL be `0`
- **AND** top-level `delayedCaseCount` SHALL be `0`
