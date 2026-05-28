## MODIFIED Requirements

### Requirement: Unified External Assistant Evaluation

The system SHALL provide a backend evaluation path that can exercise external-model-backed AI assistants without using the frontend, and SHALL include first-stage education-agent goal metrics in the generated report.

#### Scenario: Fixture structure is validated without external model access

- **WHEN** the evaluator runs without `AI_EVAL_API_KEY`
- **THEN** it validates the assistant eval fixture schema, teacher expectations, rubrics, required signals, forbidden phrases, and long-code diagnosis fixture floor
- **AND** it does not call any external model

#### Scenario: Live smoke evaluation runs with external model access

- **WHEN** `AI_EVAL_API_KEY` is provided
- **THEN** the evaluator calls the configured external model through the project AI services
- **AND** it writes a per-case report under `target/ai-eval-reports`
- **AND** it records runtime failures separately from teaching-quality misses
- **AND** it includes a sample profile with assistant types, case ids, and long-code diagnosis coverage
- **AND** it includes a route profile with primary route and fallback-route configuration status
- **AND** it includes failure reason counts for quota, rate limit, budget guard, timeout, unsupported model, invalid output, and other runtime failures
- **AND** it includes a goal snapshot with completion, failure, quality, safety, teaching-action, goal-gap, coverage-gap, next-focus, and next-action metrics

#### Scenario: Live smoke evaluation defaults to low-budget runtime

- **WHEN** `AI_EVAL_API_KEY` is provided
- **AND** `AI_EVAL_EXTERNAL_RUNTIME_MODE` is not provided
- **THEN** submission diagnosis live eval uses `single-call` runtime mode
- **AND** the report records `runtimeMode=single-call`
- **AND** developers can still set `AI_EVAL_EXTERNAL_RUNTIME_MODE=staged` for staged diagnosis debugging

### Requirement: Failure Classification

The evaluator SHALL classify external model failures in a way that supports agent iteration and SHALL prevent fallback output from being counted as external model quality.

#### Scenario: External model is unavailable or unstable

- **WHEN** the AI call fails due to quota, rate limit, timeout, unsupported model, invalid JSON, safety rejection, or runtime fallback
- **THEN** the report records a specific failure reason
- **AND** the case is not counted as a teaching-quality miss unless the assistant produced a completed output
- **AND** the case is not counted as signal hit, evidence valid, or teaching action valid for external model quality metrics
- **AND** signal, evidence, and teaching-action rates are computed over completed model outputs rather than all requested cases
- **AND** the next optimization focus is external model capacity rather than prompt tuning

#### Scenario: Provider capacity guard opens during a live evaluation

- **WHEN** a live evaluation encounters an insufficient quota or rate-limit failure
- **THEN** subsequent assistant calls in the same run reuse the shared budget guard
- **AND** blocked calls are reported as `BUDGET_GUARD_OPEN`
- **AND** the report failure reason counts make the capacity issue visible without requiring every remaining sample to call the provider again

#### Scenario: Capacity fails and no fallback route is configured

- **WHEN** runtime failure rate misses the target because the primary route is quota-limited or rate-limited
- **AND** the route profile shows only one configured route
- **THEN** the next optimization focus is model route configuration
- **AND** the next action asks developers to configure a fallback OpenAI-compatible route or restore primary-route quota before continuing large live eval runs
