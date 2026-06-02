## MODIFIED Requirements

### Requirement: runtime draft 必须结构化输出恢复 smoke 验证建议

系统 SHALL 在外部模型调用受限的 runtime fixture draft 中输出结构化 recovery smoke guidance。

#### Scenario: recovery smoke guidance 被 report summary 消费

- **GIVEN** a runtime fixture draft has `recoverySmokeRecommended=true`
- **WHEN** live-model-eval report summary is generated
- **THEN** the recovery status logic SHALL use that draft as recovery context
- **AND** if no current entry satisfies recovery checks, the report SHALL mark recovery as `BLOCKED`
