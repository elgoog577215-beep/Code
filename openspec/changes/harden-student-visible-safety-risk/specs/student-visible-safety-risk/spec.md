## ADDED Requirements

### Requirement: Teaching hint safety uses student-visible content

The external model teaching hint validator SHALL judge answer leak risk from student-visible content instead of treating top-level model self-assessment as an automatic rejection.

#### Scenario: Safe visible hint with high top-level risk

- **WHEN** a teaching hint has safe `studentHint`, safe `studentHintPlan`, and safe `learningInterventionPlan`
- **WHEN** the top-level `teachingHint.answerLeakRisk` is `HIGH`
- **THEN** validation MUST NOT reject the teaching hint only because of the top-level risk field

#### Scenario: Unsafe visible hint remains rejected

- **WHEN** a teaching hint exposes complete code, direct replacement steps, final answers, hidden data, or executable control structures in student-visible fields
- **THEN** validation MUST reject the teaching hint as `SAFETY_RISK`

### Requirement: Runtime answer leak risk is derived from visible plans

The runtime analysis response SHALL derive final answer leak risk from student-visible hint plans and intervention plans, not from teacher-only notes or top-level aggregate risk alone.

#### Scenario: Teacher note mentions forbidden exact fix as a warning

- **WHEN** a model output diagnoses an input parsing issue and gives a safe student hint that asks the student to compare input structure
- **WHEN** the teacher note says not to directly reveal a concrete loop form
- **THEN** the final student hint MUST keep the concrete teaching action such as `COMPARE_INPUT_SPEC`
- **THEN** the final response MUST NOT be downgraded only because the teacher note contains the forbidden exact fix

#### Scenario: Student-visible direct fix remains downgraded

- **WHEN** the student-visible hint or plan tells the student the exact replacement code or final answer
- **THEN** the final response MUST be rejected or downgraded by the safety layer
