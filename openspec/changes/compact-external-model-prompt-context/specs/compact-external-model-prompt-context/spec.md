## ADDED Requirements

### Requirement: External model calls use compact prompt context

External model runtime calls SHALL send a compact prompt context that preserves diagnosis-critical fields while omitting non-essential explanation fields.

#### Scenario: Single-call runtime sends compact context

- **WHEN** single-call runtime invokes an external model for submission diagnosis
- **THEN** the user prompt MUST include the current evidence brief, candidate signals, evidence refs, allowed tags, and teaching actions
- **THEN** the user prompt MUST omit non-essential standard library fields such as student explanations, teacher explanations, ability points, and verbose action usage text

#### Scenario: Compact context limits long fields

- **WHEN** a submitted code excerpt or problem brief is long
- **THEN** the prompt context MUST truncate it to the configured prompt budget
- **THEN** the original full internal evidence package MUST remain available for local validation and reporting

### Requirement: Compact context does not weaken validation

External model output validation SHALL continue using the full runtime plan rather than the compact prompt object.

#### Scenario: Model output cites invalid evidence after compact prompt

- **WHEN** the external model returns an evidence ref or tag not allowed by the full runtime plan
- **THEN** the validator MUST still reject or fallback according to existing rules

#### Scenario: Model output contains unsafe student-visible content

- **WHEN** the external model returns a direct fix, complete code, hidden data, or executable control structure in student-visible fields
- **THEN** the safety validator MUST still reject or downgrade the output
