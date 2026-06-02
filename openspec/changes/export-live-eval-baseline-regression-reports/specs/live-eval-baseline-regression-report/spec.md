## ADDED Requirements

### Requirement: Baseline regression comparisons export audit reports

When a live eval baseline regression gate is explicitly enabled with a baseline report path, the system SHALL write a structured JSON regression report for the comparison.

#### Scenario: Assistant baseline report comparison writes audit report

- **WHEN** assistant live eval runs with `AI_EVAL_BASELINE_REPORT` pointing to a baseline report
- **THEN** the run MUST write an `assistant-live-eval-baseline-regression-*.json` report containing baseline path, current report path, compared case count, violation count, status, and violation details

#### Scenario: Model baseline report comparison writes audit report

- **WHEN** live-model-eval runs with `AI_EVAL_MODEL_BASELINE_REPORT` pointing to a baseline report
- **THEN** the run MUST write a `live-model-eval-baseline-regression-*.json` report containing baseline path, current report path, compared case count, violation count, status, and violation details

#### Scenario: Failed regression still leaves report

- **WHEN** a baseline regression comparison produces one or more violations
- **THEN** the regression report MUST be written before the test assertion fails

#### Scenario: Baseline disabled preserves existing behavior

- **WHEN** no baseline report environment variable is configured
- **THEN** the live eval run MUST NOT write a baseline regression report and MUST keep the existing smoke behavior
