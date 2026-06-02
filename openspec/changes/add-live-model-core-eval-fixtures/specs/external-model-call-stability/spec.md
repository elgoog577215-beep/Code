## MODIFIED Requirements

### Requirement: Live Eval Can Throttle Cases

The assistant live eval and live-model-eval SHALL support configurable waiting between cases, and live-model-eval SHALL support selecting stable case ids for small recovery or baseline runs.

#### Scenario: Assistant case delay is configured

- **WHEN** `AI_EVAL_CASE_DELAY_MS` is set to a positive number
- **THEN** assistant live eval waits at least that long before starting the next fixture case
- **AND** the report still records per-case latency for the actual assistant call

#### Scenario: Model diagnosis case delay is configured

- **WHEN** `AI_EVAL_CASE_DELAY_MS` is set to a positive number
- **THEN** live-model-eval waits at least that long before starting the next diagnosis case
- **AND** the report records the configured `caseDelayMs`
- **AND** the report records how many cases were delayed

#### Scenario: Model diagnosis case ids are configured

- **WHEN** `AI_EVAL_CASE_IDS` contains one or more comma-separated case ids
- **THEN** live-model-eval SHALL only run matching case ids
- **AND** matching SHALL be case-insensitive
- **AND** smoke limit SHALL apply after case id filtering
