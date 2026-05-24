## ADDED Requirements

### Requirement: Evaluation Fixtures Must Be Readable

The system SHALL reject AI evaluation fixtures that contain obvious mojibake, unreadable Chinese text, or corrupted forbidden phrases.

#### Scenario: Fixture hygiene is checked without external model access

- **WHEN** the assistant evaluation fixture structure test runs
- **THEN** it scans teacher expectations, rubrics, quality notes, required signals, and forbidden phrases for known mojibake markers
- **AND** it fails before any external model call if corrupted text is found

### Requirement: Standard Library Must Preserve Fine-Grained Teaching Actions

The system SHALL include teaching actions for both coarse issue tags and fine-grained issue tags when building an external model standard library pack.

#### Scenario: Fine-grained diagnosis has a dedicated teaching action

- **GIVEN** a diagnosis brief or rule signal contains a fine-grained issue tag with a teaching action
- **WHEN** the standard library pack is built
- **THEN** the pack includes that fine-grained teaching action
- **AND** it preserves parent issue guidance for context

### Requirement: Coach Must Reuse Diagnosis Evidence And Safety Boundaries

The Coach assistant SHALL reuse the diagnosis standard library, evidence references, and safety rules instead of relying only on free-form diagnosis text.

#### Scenario: Coach prompt is prepared from a diagnosed submission

- **GIVEN** a coach request contains a diagnosis tag and evidence references
- **WHEN** the Coach assistant prepares model context
- **THEN** the context includes the relevant diagnosis tag, teaching action, allowed evidence references, and safety rules
- **AND** generated questions must not reveal full code, final answers, or hidden test details

### Requirement: Live Evaluation Must Support Enforceable Quality Gates

The live external-model evaluation SHALL support configurable quality thresholds for signal hit rate, evidence validity, safety pass rate, and fallback rate.

#### Scenario: Threshold enforcement is enabled

- **GIVEN** `AI_EVAL_ENFORCE_THRESHOLDS=true`
- **WHEN** a live eval report is generated
- **THEN** the evaluator compares report metrics against configured thresholds
- **AND** the test fails with explicit violations when the external-model-backed assistant misses the quality gate

#### Scenario: Threshold enforcement is disabled

- **GIVEN** threshold enforcement is not enabled
- **WHEN** a live eval report is generated
- **THEN** the report is still written
- **AND** threshold results are available for human review without failing the test

### Requirement: Teacher AI Quality Overview Must Explain Model Quality Risk

The teacher-facing AI quality overview SHALL separate model invocation quality from teaching-output quality.

#### Scenario: AI quality overview is requested

- **WHEN** the quality overview is computed
- **THEN** it includes model fallback count, partial result count, runtime failure count, low-confidence count, high leak-risk count, and teacher correction count
- **AND** it exposes a concise quality risk summary that can guide teacher intervention
