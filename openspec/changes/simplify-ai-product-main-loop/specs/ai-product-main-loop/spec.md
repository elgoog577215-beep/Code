## ADDED Requirements

### Requirement: 学生端主反馈必须以 studentFeedback 为中心

系统 SHALL use `SubmissionAnalysisResponse.studentFeedback` as the primary student-facing AI feedback contract.

#### Scenario: failed submission response is displayed to a student

- **GIVEN** a failed submission has an analysis response
- **WHEN** the student-facing feedback card is rendered
- **THEN** the primary content SHALL come from `studentFeedback.summary`, `studentFeedback.blockingIssues`, `studentFeedback.improvementOpportunities`, and `studentFeedback.nextLearningAction`
- **AND** taxonomy labels, provider, promptVersion, fallback status, latency, baseline, and model metrics SHALL NOT be shown in the primary feedback area

### Requirement: 后端 AI 主链路必须保持固定六步

系统 SHALL keep the student-facing diagnosis loop organized as six conceptual steps: judge result, evidence package, local candidate signals, external model attempt, safety validation, and student feedback assembly.

#### Scenario: analysis is generated for a stored submission

- **GIVEN** a submission result is available
- **WHEN** analysis is generated
- **THEN** the system SHALL build judge evidence
- **AND** it SHALL derive local candidate signals
- **AND** it MAY attempt external model analysis
- **AND** it SHALL validate model output before student exposure
- **AND** it SHALL assemble `studentFeedback` before returning or persisting the response

### Requirement: 本地学生反馈组装器必须提供可用契约输出

系统 SHALL ensure local fallback feedback always includes at least one blocking issue and one next learning action when an analysis response exists.

#### Scenario: model output is unavailable or unsafe

- **GIVEN** a non-null submission analysis response
- **AND** external model feedback is unavailable, partial, or unsafe
- **WHEN** student feedback is assembled
- **THEN** the result SHALL include at least one `blockingIssues` item
- **AND** it SHALL include `nextLearningAction.task`
- **AND** it SHALL avoid complete code, direct replacement instructions, and hidden test speculation

### Requirement: 复杂能力必须按层下沉

系统 SHALL keep runtime telemetry, live eval, baseline regression, and fallback attribution in teacher or研发 surfaces instead of the student primary surface.

#### Scenario: live eval or runtime metadata exists

- **GIVEN** an analysis includes model invocation metadata or eval report fields
- **WHEN** student-facing feedback is shown
- **THEN** those fields SHALL remain hidden from the primary student feedback area
- **AND** they MAY appear in teacher dashboards, folded details, or研发 reports

### Requirement: OpenSpec 变更必须可按产品层级索引

系统 SHALL maintain a layer index for active OpenSpec changes to help future work choose the right surface.

#### Scenario: engineer reviews existing AI changes

- **WHEN** the OpenSpec layer index is opened
- **THEN** each active change SHALL be grouped into product, teacher, evaluation, runtime, UI, or platform/history buckets
- **AND** the index SHALL state that student main loop changes should not expose研发 telemetry to students
