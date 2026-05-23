## ADDED Requirements

### Requirement: Unified External Assistant Evaluation

The system SHALL provide a backend evaluation path that can exercise external-model-backed AI assistants without using the frontend.

#### Scenario: Fixture structure is validated without external model access

- **WHEN** the evaluator runs without `AI_EVAL_API_KEY`
- **THEN** it validates the assistant eval fixture schema, teacher expectations, rubrics, required signals, and forbidden phrases
- **AND** it does not call any external model

#### Scenario: Live smoke evaluation runs with external model access

- **WHEN** `AI_EVAL_API_KEY` is provided
- **THEN** the evaluator calls the configured external model through the project AI services
- **AND** it writes a per-case report under `target/ai-eval-reports`
- **AND** it records runtime failures separately from teaching-quality misses

### Requirement: Multi-Assistant Coverage

The evaluation fixture set SHALL cover at least submission diagnosis, Coach questioning, and growth report generation.

#### Scenario: Submission diagnosis sample is evaluated

- **WHEN** a fixture has assistant type `SUBMISSION_DIAGNOSIS`
- **THEN** the evaluator checks expected issue tags, fine-grained tags, evidence references, safety, and output summary

#### Scenario: Coach question sample is evaluated

- **WHEN** a fixture has assistant type `COACH_QUESTION`
- **THEN** the evaluator checks question specificity, required evidence references, forbidden leakage phrases, and answer leak risk

#### Scenario: Growth report sample is evaluated

- **WHEN** a fixture has assistant type `GROWTH_REPORT`
- **THEN** the evaluator checks that the report uses the submission timeline, names concrete learning changes, avoids unsupported claims, and gives an actionable next step

### Requirement: Teacher-AI Comparison

The evaluation report SHALL preserve a concise comparison between teacher expectation and AI output.

#### Scenario: Per-case report is generated

- **WHEN** a case finishes
- **THEN** the report includes teacher expectation, output summary, AI-better note, teacher-better note, and iteration suggestion

### Requirement: Failure Classification

The evaluator SHALL classify external model failures in a way that supports agent iteration.

#### Scenario: External model is unavailable or unstable

- **WHEN** the AI call fails due to quota, rate limit, timeout, unsupported model, invalid JSON, safety rejection, or runtime fallback
- **THEN** the report records a specific failure reason
- **AND** the case is not counted as a teaching-quality miss unless the assistant produced a completed output
