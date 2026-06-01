## MODIFIED Requirements

### Requirement: Teacher AI Quality Overview Must Explain Model Quality Risk
The teacher-facing AI quality overview SHALL separate model invocation quality from teaching-output quality and SHALL expose structured quality dimensions for diagnosis confidence, evidence grounding, hint safety, learning action effectiveness, model runtime stability, and teacher correction feedback.

#### Scenario: AI quality overview is requested
- **WHEN** the quality overview is computed
- **THEN** it includes model fallback count, partial result count, runtime failure count, low-confidence count, high leak-risk count, and teacher correction count
- **AND** it exposes a concise quality risk summary that can guide teacher intervention
- **AND** it exposes structured quality dimensions with status, score, evidence references, and recommended action
- **AND** it exposes improvement priorities and eval readiness so teacher corrections can be turned into regression fixtures
