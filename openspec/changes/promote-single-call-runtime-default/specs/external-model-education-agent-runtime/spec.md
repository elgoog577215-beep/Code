## MODIFIED Requirements

### Requirement: External Model Runtime Must Prefer Low-Budget Single-Call Diagnosis

The submission diagnosis runtime SHALL use single-call external diagnosis by default to reduce online model request count while preserving validation and safety gates.

#### Scenario: Runtime mode is not explicitly configured

- **GIVEN** external runtime is enabled
- **AND** no runtime mode override is configured
- **WHEN** a submission diagnosis uses the external model runtime
- **THEN** the system uses the single-call diagnosis-and-teaching prompt
- **AND** performs one external model call for that diagnosis attempt
- **AND** validates the diagnosis and teaching output before returning it to the student

#### Scenario: Staged runtime is explicitly configured

- **GIVEN** external runtime is enabled
- **AND** `AI_EXTERNAL_RUNTIME_MODE=staged`
- **WHEN** a submission diagnosis uses the external model runtime
- **THEN** the system uses the staged diagnosis and teaching prompts
- **AND** keeps the existing staged validation, partial-completion, and fallback behavior
