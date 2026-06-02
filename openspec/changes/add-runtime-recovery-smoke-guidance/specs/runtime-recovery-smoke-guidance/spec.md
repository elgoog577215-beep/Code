## ADDED Requirements

### Requirement: runtime draft 必须结构化输出恢复 smoke 验证建议

系统 SHALL 在外部模型调用受限的 runtime fixture draft 中输出结构化 recovery smoke guidance。

#### Scenario: quota runtime draft 推荐恢复 smoke

- **GIVEN** live-model-eval entry has `failureReason` containing `RATE_LIMITED`
- **AND** generated runtime draft has `failureType=QUOTA_LIMIT`
- **WHEN** the runtime draft is exported
- **THEN** `recoverySmokeRecommended` SHALL be true
- **AND** `recoverySmokeCaseId` SHALL match the live eval case id
- **AND** `recoverySmokeRuntimeProfile` SHALL identify the runtime profile to retest
- **AND** `recoverySmokeRequiredChecks` SHALL include model completion, no fallback, model hit, evidence and safety checks

#### Scenario: stream no-content runtime draft 要求验证 content chunk

- **GIVEN** live-model-eval entry has `transportMode=stream`
- **AND** `streamContentChunkCount=0`
- **WHEN** recovery smoke guidance is generated
- **THEN** `recoverySmokeRequiredChecks` SHALL include `streamContentChunkCount>0`

### Requirement: recovery smoke command hint 必须脱敏

系统 SHALL NOT include API keys, tokens, authorization headers or provider secrets in recovery smoke command hints.

#### Scenario: command hint 不包含密钥

- **GIVEN** runtime draft is generated from quota or provider failures
- **WHEN** `recoverySmokeCommandHint` is serialized
- **THEN** it SHALL NOT contain API Key, api_key, token, Authorization, Bearer or `ms-`
- **AND** it SHALL mention the live model smoke test name or eval entrypoint

### Requirement: 教师端 runtime 草稿必须暴露恢复验证入口

系统 SHALL 在教师端 runtime fixture draft 中暴露 recovery smoke guidance so maintainers can verify external model recovery after quota, provider, budget guard or timeout failures.

#### Scenario: 教师端 quota 草稿包含恢复 smoke 字段

- **GIVEN** an assignment submission analysis has `aiInvocation.status=MODEL_RUNTIME_FALLBACK`
- **AND** `failureReason` contains quota or rate limit
- **WHEN** diagnosis eval fixture drafts are exported
- **THEN** the runtime draft SHALL include `recoverySmokeRecommended=true`
- **AND** `recoverySmokeRequiredChecks` SHALL include `aiInvocation.status=MODEL_COMPLETED` and `fallbackUsed=false`

### Requirement: recovery smoke 不应污染质量失败样本

系统 SHALL NOT recommend recovery smoke for quality misses that do not represent external runtime availability failures.

#### Scenario: quality miss 不推荐恢复 smoke

- **GIVEN** a runtime draft is generated for `QUALITY_MISS`
- **WHEN** recovery smoke guidance is evaluated
- **THEN** `recoverySmokeRecommended` SHALL be false
- **AND** `recoverySmokeRequiredChecks` SHALL be empty
