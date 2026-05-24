## ADDED Requirements

### Requirement: Learning Action Evidence in Diagnosis History

The system SHALL include the previous same-problem learning intervention and learning action evidence in the current diagnosis history when those data are available.

#### Scenario: Previous learning action evidence is available

- **WHEN** a current submission has a previous same-problem submission with diagnosis analysis
- **AND** the previous analysis includes `learningInterventionPlan` and `learningActionEvidence`
- **THEN** the current `HistoryEvidence` includes the previous intervention type, task, completion signal, action status, confidence, evidence refs, observed summary, and next adjustment

#### Scenario: Previous learning action evidence is missing

- **WHEN** previous diagnosis analysis does not include learning action evidence
- **THEN** the current diagnosis history remains valid
- **AND** learning action evidence fields are empty rather than fabricated

### Requirement: External Model Brief Uses Learning Action Feedback

The system SHALL expose previous learning action feedback to the external model through the model diagnosis brief.

#### Scenario: Model brief is built from history with action feedback

- **WHEN** `ModelDiagnosisBriefBuilder` receives history containing previous learning action evidence
- **THEN** `learningTrajectorySummary` includes the previous intervention type, execution status, observed evidence, and next adjustment
- **AND** the summary remains concise enough for prompt use

### Requirement: Diagnosis Strategy Reacts to Action Evidence

The diagnostic agent SHALL adjust the next learning intervention according to previous learning action status.

#### Scenario: Previous action was contradicted

- **WHEN** history says the previous learning action status is `CONTRADICTED`
- **THEN** the next intervention prioritizes a smaller observable action or teacher attention
- **AND** the diagnostic evidence refs include the previous action evidence refs

#### Scenario: Previous action was observed

- **WHEN** history says the previous learning action status is `OBSERVED`
- **THEN** the next intervention can shift toward review, transfer, or generalization

### Requirement: Prompt and Standard Library Describe Action Feedback Rules

The external model prompt and standard library SHALL instruct the model how to use previous learning action feedback.

#### Scenario: Prompt contains action feedback rules

- **WHEN** the external model runtime prepares diagnosis and teaching prompts
- **THEN** the prompt tells the model to distinguish `OBSERVED`, `PARTIALLY_OBSERVED`, `CONTRADICTED`, and `NOT_OBSERVED`
- **AND** it forbids assuming that a student executed an action when no evidence supports it
