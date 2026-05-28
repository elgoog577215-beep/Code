## ADDED Requirements

### Requirement: Student page surfaces AI invocation source state

The student problem page SHALL distinguish external model output from local fallback output using persisted analysis invocation metadata.

#### Scenario: External model completed

- **GIVEN** a submission analysis has `aiInvocation.status=MODEL_COMPLETED`
- **AND** `aiInvocation.fallbackUsed` is not true
- **WHEN** the student views the submission result
- **THEN** the analysis metric displays `外部模型完成`

#### Scenario: External model partially completed

- **GIVEN** a submission analysis has `aiInvocation.status=MODEL_PARTIAL_COMPLETED`
- **AND** `aiInvocation.fallbackUsed` is not true
- **WHEN** the student views the submission result
- **THEN** the analysis metric displays `外部模型部分完成`

#### Scenario: Local fallback was used

- **GIVEN** a submission analysis has `aiInvocation.fallbackUsed=true`
- **OR** `aiInvocation.status=MODEL_RUNTIME_FALLBACK`
- **OR** `aiInvocation.provider=LOCAL_RULES`
- **WHEN** the student views the submission result
- **THEN** the analysis metric displays `本地兜底`
- **AND** the metric does not display `外部模型完成`

#### Scenario: Analysis is still missing

- **GIVEN** a submission has no analysis
- **WHEN** the student views the submission result
- **THEN** the analysis metric displays `观察中`
