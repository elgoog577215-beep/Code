## MODIFIED Requirements

### Requirement: Live Eval Can Throttle Cases

The assistant live eval and live-model-eval SHALL support configurable waiting between cases.

#### Scenario: Assistant case delay is configured

- **WHEN** `AI_EVAL_CASE_DELAY_MS` is set to a positive number
- **THEN** assistant live eval waits at least that long before starting the next fixture case
- **AND** the report still records per-case latency for the actual assistant call

#### Scenario: Model diagnosis case delay is configured

- **WHEN** `AI_EVAL_CASE_DELAY_MS` is set to a positive number
- **THEN** live-model-eval waits at least that long before starting the next diagnosis case
- **AND** the report records the configured `caseDelayMs`
- **AND** the report records how many cases were delayed
