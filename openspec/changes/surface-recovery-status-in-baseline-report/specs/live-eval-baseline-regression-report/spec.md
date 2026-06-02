## MODIFIED Requirements

### Requirement: baseline regression 必须写出结构化对比报告

When a live eval baseline regression gate is explicitly enabled with a baseline report path, the system SHALL write a structured JSON regression report for the comparison.

#### Scenario: Model baseline report comparison writes recovery context

- **WHEN** live-model-eval runs with `AI_EVAL_MODEL_BASELINE_REPORT` pointing to a baseline report
- **THEN** the regression report MUST include current recovery status fields when available
- **AND** the report MUST preserve existing compared case count, violation count, status and violation details
