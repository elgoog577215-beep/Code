## ADDED Requirements

### Requirement: Retry Transient External Model Call Failures

The system SHALL retry transient external model call failures using bounded, configurable attempts and wait time.

#### Scenario: Rate limited request is retried

- **WHEN** an external model request fails with HTTP 429 or a rate limit message
- **THEN** the system retries the request up to the configured retry limit
- **AND** waits between attempts
- **AND** preserves the final failure reason if all attempts fail

#### Scenario: Non-retryable model validation failure is not retried

- **WHEN** model output fails diagnosis tag, evidence, safety, or schema validation after a successful HTTP response
- **THEN** the system MUST NOT retry the same stage solely to bypass validation
- **AND** the existing validation fallback or partial-completion path remains responsible for the result

### Requirement: Live Eval Can Throttle Cases

The assistant live eval SHALL support configurable waiting between cases.

#### Scenario: Case delay is configured

- **WHEN** `AI_EVAL_CASE_DELAY_MS` is set to a positive number
- **THEN** live eval waits at least that long before starting the next fixture case
- **AND** the report still records per-case latency for the actual assistant call

### Requirement: Report External Call Stability Failures

The assistant live eval SHALL preserve external call stability failures separately from quality misses.

#### Scenario: Quota or rate limit fallback appears in report

- **WHEN** an assistant falls back because the external model returns quota or rate limit errors
- **THEN** the report records `completedOutput=false`
- **AND** the report records a failure reason containing `INSUFFICIENT_QUOTA` or `RATE_LIMITED`
- **AND** quality miss counts are not used as the primary explanation for that runtime failure
