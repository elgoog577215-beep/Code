## MODIFIED Requirements

### Requirement: live eval 报告必须导出 runtime fixture 草稿

系统 SHALL 在 assistant live eval 和 model diagnosis live eval 报告中输出顶层 `runtimeFixtureDrafts` 与 `runtimeFixtureDraftCount`。

#### Scenario: runtime 草稿携带恢复 smoke guidance

- **GIVEN** a live-model-eval entry is exported as a runtime fixture draft for quota, provider, budget guard, timeout or stream no-content failure
- **WHEN** report `runtimeFixtureDrafts` are generated
- **THEN** the draft MAY include `recoverySmokeRecommended`
- **AND** the draft MAY include `recoverySmokeCaseId`
- **AND** the draft MAY include `recoverySmokeRuntimeProfile`
- **AND** the draft MAY include `recoverySmokeCommandHint`
- **AND** the draft MAY include `recoverySmokeRequiredChecks`
