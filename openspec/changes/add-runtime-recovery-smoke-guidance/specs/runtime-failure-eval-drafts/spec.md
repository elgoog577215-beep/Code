## MODIFIED Requirements

### Requirement: runtime 失败必须导出运行归因 fixture 草稿

系统 SHALL 从真实提交分析中的外部模型运行失败生成 runtime fixture 草稿。

#### Scenario: runtime 草稿暴露恢复验证字段

- **GIVEN** 作业内某次提交的 `aiInvocation.status=MODEL_RUNTIME_FALLBACK`
- **WHEN** 导出 diagnosis eval fixture draft
- **THEN** runtime draft MAY include `recoverySmokeRecommended`
- **AND** runtime draft MAY include `recoverySmokeCommandHint`
- **AND** runtime draft MAY include `recoverySmokeRequiredChecks`
