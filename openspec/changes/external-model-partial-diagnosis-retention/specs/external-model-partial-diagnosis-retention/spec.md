## ADDED Requirements

### Requirement: Retain Valid External Diagnosis When Teaching Stage Fails

The system SHALL retain a validated external diagnosis decision when the later teaching hint stage fails.

#### Scenario: Teaching stage API call fails after diagnosis stage succeeds

- **WHEN** `diagnosis-judge-v1` returns a valid diagnosis decision
- **AND** `teaching-hint-v1` fails due to timeout, quota, rate limit, API error, invalid JSON, or unsupported model
- **THEN** the final analysis uses the external diagnosis tags, evidence references, confidence, and uncertainty
- **AND** the final analysis uses safe local teaching guidance
- **AND** AI invocation status is `MODEL_PARTIAL_COMPLETED`

#### Scenario: Teaching stage is rejected by safety validation

- **WHEN** `diagnosis-judge-v1` returns a valid diagnosis decision
- **AND** `teaching-hint-v1` output is rejected for safety or validation reasons
- **THEN** the final analysis keeps the external diagnosis decision
- **AND** replaces unsafe teaching content with local safe teaching guidance
- **AND** records the teaching failure reason

### Requirement: Completely Invalid Diagnosis Still Falls Back

The system SHALL NOT retain external model output when the diagnosis stage itself fails validation.

#### Scenario: Diagnosis stage uses invalid tags

- **WHEN** `diagnosis-judge-v1` returns invalid tags or invalid evidence references
- **THEN** the final analysis falls back to the rule-aware baseline
- **AND** AI invocation status remains `MODEL_RUNTIME_FALLBACK`

### Requirement: Eval Treats Partial Completion As Usable Model Output

The assistant live eval SHALL count `MODEL_PARTIAL_COMPLETED` as completed model output while preserving the partial failure reason.

#### Scenario: Partial diagnosis appears in eval report

- **WHEN** an analysis has AI invocation status `MODEL_PARTIAL_COMPLETED`
- **THEN** the eval entry has `completedOutput=true`
- **AND** checks expected signals, evidence validity, teaching action validity, and safety normally
- **AND** retains the failure stage and reason in the report
