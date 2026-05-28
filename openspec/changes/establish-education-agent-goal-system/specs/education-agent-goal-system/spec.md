## ADDED Requirements

### Requirement: Education Agent Goal Hierarchy

The system SHALL define a multi-layer goal hierarchy for the education AI agent, covering the north-star outcome, online diagnosis availability, student learning improvement, teacher teaching value, and long-term maturity.

#### Scenario: Goal hierarchy is reviewed

- **WHEN** a developer reviews the education agent goal system
- **THEN** the goals include a north-star outcome for student improvement and teacher value
- **AND** the goals include measurable first-stage targets for external model completion, diagnosis quality, evidence quality, teaching action quality, and safety
- **AND** the goals distinguish later-stage student learning outcomes and teacher analytics outcomes from first-stage model-output metrics

### Requirement: External Model Quality Metrics

The system SHALL report external model quality metrics separately from local fallback availability.

#### Scenario: Live eval report is generated

- **WHEN** live eval finishes
- **THEN** the report includes external completion rate, runtime failure rate, signal hit rate, evidence valid rate, safety pass rate, and teaching action valid rate
- **AND** signal, evidence, and teaching action quality rates use completed external model outputs as their denominator
- **AND** fallback entries are preserved for resilience analysis but do not count as external model quality hits

#### Scenario: Runtime failure occurs before model quality can be judged

- **WHEN** a case fails because of quota, rate limit, timeout, or runtime fallback before completed model output exists
- **THEN** the report counts the case against completion and runtime-failure metrics
- **AND** the case does not lower signal, evidence, or teaching-action quality rates
- **AND** the goal gap directs the next step toward external model stability rather than prompt, standard-library, or validator tuning

### Requirement: Goal Gap Reporting

The system SHALL include goal gap information and a recommended next optimization focus in live eval reports.

#### Scenario: Current metrics miss first-stage targets

- **WHEN** a report metric is below or above its target threshold
- **THEN** the report records a human-readable goal gap
- **AND** the goal gap names the metric, actual value, target value, and likely next optimization direction
- **AND** the report records a `nextOptimizationFocus` and `nextAction` that prioritize the largest current blocker

#### Scenario: Current metrics meet first-stage targets

- **WHEN** all first-stage report metrics meet their targets
- **THEN** the report records no goal gaps
- **AND** the report remains usable as a regression baseline
- **AND** the next optimization focus moves to coverage expansion or next-stage student and teacher outcomes

#### Scenario: Smoke sample meets metrics but does not meet coverage floor

- **WHEN** a live eval report meets first-stage rate targets on fewer long-code diagnosis cases than the configured coverage floor
- **THEN** the report records no metric goal gap for the satisfied rates
- **AND** the report records a coverage gap naming the evaluated long-code diagnosis count and target count
- **AND** the report states that the result is a smoke or local regression baseline, not proof of broad external-model diagnosis quality
- **AND** the next optimization focus is evaluation coverage rather than prompt tuning

### Requirement: Long-Code Diagnosis Evaluation Floor

The system SHALL require enough long-code diagnosis fixtures for external-model diagnosis evaluation.

#### Scenario: Fixture structure test runs

- **WHEN** the assistant eval fixture structure is validated
- **THEN** at least 10 submission diagnosis fixtures contain source code with 20 or more lines
- **AND** these fixtures include teacher expectations, required evidence references, expected issue tags, expected fine-grained tags, and forbidden leakage phrases

### Requirement: Goal System Iteration Boundary

The system SHALL treat student improvement and teacher value as explicit later-stage targets without pretending they are complete in first-stage live eval.

#### Scenario: First-stage implementation is complete

- **WHEN** first-stage live eval target reporting is implemented
- **THEN** the system documents next-stage targets for next-submission improvement, repeated-error detection, teacher common-error clustering, teacher intervention suggestions, and teacher correction feedback
- **AND** it does not claim those later-stage outcomes are already measured unless corresponding data and tests exist
