## ADDED Requirements

### Requirement: live-model-eval records latency budget signals

系统 SHALL 在 live-model-eval report 中记录每条外部模型评测的延迟预算信号。

#### Scenario: Successful slow response is counted separately

- **WHEN** a live-model-eval entry completes without fallback
- **AND** `latencyMs` is greater than `latencyBudgetMs`
- **THEN** the entry MUST set `latencyBudgetExceeded=true`
- **AND** the report MUST increment `latencyBudgetExceededCount`
- **AND** the report MUST NOT count the entry as timeout unless the failure reason indicates timeout

#### Scenario: Fast successful response remains healthy

- **WHEN** a live-model-eval entry completes without fallback
- **AND** `latencyMs` is less than or equal to `latencyBudgetMs`
- **THEN** the entry MUST set `latencyBudgetExceeded=false`
- **AND** the entry MAY be exported as a quality baseline if other quality gates pass

### Requirement: Slow successful model outputs become runtime drafts

系统 SHALL 将成功但超过 latency budget 的 live-model-eval entry 导出为 runtime fixture draft。

#### Scenario: Slow response draft preserves quality context

- **WHEN** a successful live-model-eval entry has `latencyBudgetExceeded=true`
- **THEN** generated runtime fixture draft MUST include `failureType=SLOW_RESPONSE`
- **AND** `expectedRuntimeAction` MUST mention reducing context or output token budget
- **AND** `mustMention` MUST include a latency budget marker

### Requirement: Slow successful outputs are excluded from healthy baselines

系统 SHALL 避免把超过 latency budget 的成功 live-model-eval entry 导出为健康 quality baseline。

#### Scenario: Slow response does not create quality baseline

- **WHEN** a live-model-eval entry has `latencyBudgetExceeded=true`
- **AND** issue tag, fine tag, evidence and safety checks pass
- **THEN** quality baseline draft generation MUST skip the entry

### Requirement: Model baseline regression detects latency budget regressions

系统 SHALL 在 live-model-eval baseline regression gate 中检测 latency budget 退化。

#### Scenario: Current entry exceeds baseline latency budget

- **GIVEN** a model quality baseline contains a latency budget signal
- **AND** current live-model-eval entry has `latencyBudgetExceeded=true`
- **WHEN** model baseline regression gate evaluates the report
- **THEN** it MUST report a latency budget regression for that case
