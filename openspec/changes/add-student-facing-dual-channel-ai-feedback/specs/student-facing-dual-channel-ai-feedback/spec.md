## ADDED Requirements

### Requirement: 学生端必须获得双通道 AI 反馈

系统 SHALL expose a `studentFeedback` object on submission analysis responses that separates current blocking issues from improvement opportunities.

#### Scenario: failed submission receives blocking and improvement feedback

- **GIVEN** a submission analysis is generated for a failed submission
- **WHEN** the response is serialized
- **THEN** it SHALL include `studentFeedback.summary`
- **AND** it SHALL include at least one `studentFeedback.blockingIssues` item
- **AND** it SHALL include `studentFeedback.nextLearningAction`
- **AND** it MAY include `studentFeedback.improvementOpportunities`

### Requirement: 当前错误点必须优先解释当前失败

系统 SHALL keep blocking issues focused on the evidence that explains the current verdict or first failed case.

#### Scenario: first failed case shows missing multi-query output

- **GIVEN** the problem requires q query outputs
- **AND** the first failed case expected two output lines but actual output contains one line
- **WHEN** student feedback is assembled
- **THEN** the first blocking issue SHALL mention input/query handling or output count mismatch
- **AND** boundary helpers or debug branches SHALL NOT be promoted as the primary blocking issue

### Requirement: 继续提升点必须与当前错误点分离

系统 SHALL present improvement opportunities as follow-up learning value, not as the primary cause of the current failure.

#### Scenario: improvement opportunities are generated

- **WHEN** student feedback contains `improvementOpportunities`
- **THEN** each item SHALL use an allowed improvement category
- **AND** each item SHALL describe a learning benefit
- **AND** it SHALL NOT duplicate the primary blocking issue as an improvement item

### Requirement: 外接模型学生反馈必须通过证据和安全校验

系统 SHALL validate model-generated student feedback before exposing it to students.

#### Scenario: unsafe model feedback is rejected

- **GIVEN** the external model returns a valid diagnosis
- **AND** its student feedback contains complete code, a direct replacement, or hidden test speculation
- **WHEN** the output is validated
- **THEN** the diagnosis SHALL be retained when valid
- **AND** unsafe student feedback SHALL be replaced by local safe feedback
- **AND** the invocation status SHALL reflect partial completion

### Requirement: 标准库必须包含提升点 taxonomy

系统 SHALL include improvement taxonomy and student feedback rules in the standard library pack sent to the external model.

#### Scenario: standard library pack is built

- **WHEN** a runtime plan is prepared for external model diagnosis
- **THEN** `standardLibrary.improvementTags` SHALL include improvement categories
- **AND** `standardLibrary.studentFeedbackRules` SHALL tell the model to separate blocking issues from improvement opportunities
- **AND** low-latency runtime profile SHALL keep a compact representation of these fields

### Requirement: 学生反馈评测必须区分真实模型能力和 fallback

系统 SHALL score student-facing feedback quality only for real external model outputs when reporting model intelligence.

#### Scenario: fallback output is excluded from model feedback score

- **GIVEN** a complex live eval entry used fallback
- **WHEN** student feedback metrics are summarized
- **THEN** the entry SHALL remain visible in runtime status
- **AND** it SHALL NOT count as external model student feedback quality
