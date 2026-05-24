## ADDED Requirements

### Requirement: Runtime Must Support Single-Call Low-Budget Mode

The external model runtime SHALL support a configurable single-call mode that requests diagnosis and teaching output in one model call.

#### Scenario: Single-call mode is enabled

- **GIVEN** `ai.external-runtime-mode=single-call`
- **WHEN** a submission diagnosis uses the external runtime
- **THEN** the system sends one model request containing the diagnosis brief and standard library
- **AND** the model response includes both `diagnosisDecision` and `teachingHint`
- **AND** both parts are validated with the same validators used by staged mode

### Requirement: Single-Call Output Must Preserve Existing Safety Guarantees

The single-call runtime SHALL preserve tag validation, evidence validation, teaching action validation, and answer leak checks.

#### Scenario: Combined response has unsafe teaching hint

- **GIVEN** the combined response contains a valid diagnosis decision
- **AND** the teaching hint fails safety or schema validation
- **WHEN** the runtime builds the analysis response
- **THEN** it preserves the valid diagnosis
- **AND** it uses the local safe teaching template
- **AND** it marks the invocation as `MODEL_PARTIAL_COMPLETED`

### Requirement: Default Runtime Behavior Must Remain Staged

The system SHALL keep staged runtime as the default behavior unless single-call mode is explicitly configured.

#### Scenario: Runtime mode is not configured

- **WHEN** the external runtime runs without `ai.external-runtime-mode`
- **THEN** it uses the existing staged diagnosis and teaching calls
- **AND** existing tests for staged mode continue to pass
