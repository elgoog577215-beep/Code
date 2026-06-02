## ADDED Requirements

### Requirement: Stream length finish reason becomes output truncated signal

系统 SHALL 将外部模型 stream `finish_reason=length` 导致的解析或校验失败归因为输出截断。

#### Scenario: Truncated stream JSON is classified explicitly

- **WHEN** the latest external model stream has `streamFinishReason=length`
- **AND** single-call runtime cannot parse or validate the combined JSON output
- **THEN** `aiInvocation.failureReason` MUST be `OUTPUT_TRUNCATED`
- **AND** `aiInvocation.streamFinishReason` MUST remain `length`

### Requirement: Runtime fixture drafts expose output truncated action

系统 SHALL 在 runtime fixture draft 中把输出截断导出为独立失败类型。

#### Scenario: Live eval draft for output truncation

- **WHEN** a live-model-eval entry has `failureReason` containing `OUTPUT_TRUNCATED`
- **THEN** generated runtime fixture draft MUST set `failureType=OUTPUT_TRUNCATED`
- **AND** `expectedRuntimeAction` MUST mention output token budget or schema/context reduction
- **AND** `mustMention` MUST include `streamFinishReason=length`

### Requirement: Teacher and quality attribution distinguish output truncation

系统 SHALL 在教师 runtime fixture draft 和 AI 质量运行归因中区分输出截断。

#### Scenario: Classroom runtime draft classifies length truncation

- **WHEN** an analysis has `aiInvocation.failureReason=OUTPUT_TRUNCATED`
- **THEN** classroom runtime fixture draft MUST classify it as `OUTPUT_TRUNCATED`
- **AND** recommended action MUST mention token budget or splitting the runtime call

#### Scenario: AI quality overview classifies length truncation

- **WHEN** AI quality overview reads an invocation with `failureReason=OUTPUT_TRUNCATED`
- **THEN** runtime attribution primary failure type MAY be `OUTPUT_TRUNCATED`
- **AND** recommended action MUST not describe it as quota or provider outage
