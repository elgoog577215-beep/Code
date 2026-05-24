## ADDED Requirements

### Requirement: External Model Failures Must Be Classified Consistently

The system SHALL classify external model call failures into stable reason codes shared by diagnosis, Coach, growth report, and live eval.

#### Scenario: Provider returns quota or rate limit errors

- **WHEN** an external model API call fails with quota or rate limit text or HTTP 429
- **THEN** the system classifies the failure as `INSUFFICIENT_QUOTA` or `RATE_LIMITED`
- **AND** the assistant report does not count it as a teaching-quality miss unless a completed output was produced

### Requirement: Budget Guard Must Short-Circuit Repeated Provider Limit Failures

The system SHALL avoid repeated calls to the same external model when a recent quota or rate-limit failure indicates the provider is temporarily unavailable.

#### Scenario: Budget guard is open

- **GIVEN** a recent call recorded `INSUFFICIENT_QUOTA` or `RATE_LIMITED`
- **WHEN** another assistant attempts to call the same provider/model during the guard window
- **THEN** it skips the provider call
- **AND** it returns a safe fallback with failure reason `BUDGET_GUARD_OPEN:<reason>`

#### Scenario: A successful call clears the guard

- **GIVEN** the budget guard has recorded a provider limit failure
- **WHEN** a later call succeeds after the guard window or explicit success record
- **THEN** the guard no longer blocks future calls for that provider/model

### Requirement: Live Evaluation Must Explain Budget-Limited Runs

The live assistant evaluation SHALL preserve budget/limit failures as runtime availability signals.

#### Scenario: Live eval hits provider limits

- **WHEN** a case fails because the provider is quota-limited, rate-limited, or short-circuited by the budget guard
- **THEN** the report records the concrete failure reason
- **AND** the iteration suggestion prioritizes model routing, delay, budget, or fallback strategy instead of prompt rewriting
