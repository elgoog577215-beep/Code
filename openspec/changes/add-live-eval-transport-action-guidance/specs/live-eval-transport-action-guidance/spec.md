## ADDED Requirements

### Requirement: Live eval runtime drafts provide transport-aware actions

系统 SHALL 在 live-model-eval runtime fixture draft 中根据 transport telemetry 生成可执行的运行调试建议。

#### Scenario: Stream no-content quota draft mentions content chunk verification

- **WHEN** live-model-eval entry has `failureReason` containing `INSUFFICIENT_QUOTA`
- **AND** `transportMode=stream`
- **AND** `streamContentChunkCount=0`
- **THEN** generated runtime fixture draft MUST keep `failureType=QUOTA_LIMIT`
- **AND** `expectedRuntimeAction` MUST mention ModelScope quota
- **AND** `expectedRuntimeAction` MUST mention stream content chunk verification

#### Scenario: Stream invalid chunks point to parser validation

- **WHEN** live-model-eval entry has `transportMode=stream`
- **AND** `streamInvalidChunkCount` is greater than zero
- **THEN** generated runtime fixture draft MUST mention SSE or JSON parsing validation in `expectedRuntimeAction`

#### Scenario: Stream fallback retry points to retry validation

- **WHEN** live-model-eval entry has `streamFallbackRetryUsed=true`
- **THEN** generated runtime fixture draft MUST mention fallback retry validation in `expectedRuntimeAction`

#### Scenario: Missing transport keeps default runtime action

- **WHEN** live-model-eval entry has no transport mode
- **THEN** generated runtime fixture draft MUST use the default action for its failure type

### Requirement: Transport-aware actions remain safe

系统 SHALL 保证 transport-aware runtime action 不输出敏感 payload。

#### Scenario: Action guidance avoids raw provider payload

- **WHEN** generated runtime fixture draft includes transport-aware `expectedRuntimeAction`
- **THEN** the action MUST NOT include raw stream chunk contents, raw prompts, raw responses, API keys, authorization headers, or provider raw payloads
