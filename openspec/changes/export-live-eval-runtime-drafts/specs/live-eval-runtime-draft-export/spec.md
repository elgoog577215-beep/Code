## ADDED Requirements

### Requirement: live eval 报告必须输出 runtime 草稿清单

系统 SHALL 在 assistant live eval 和 model diagnosis live eval 报告中输出顶层 `runtimeFixtureDrafts` 与 `runtimeFixtureDraftCount`。

#### Scenario: live eval 存在 fallback 或 partial 样本

- **GIVEN** live eval entry 的 `fallbackUsed=true`
- **OR** `status=MODEL_RUNTIME_FALLBACK`
- **OR** `status=MODEL_PARTIAL_COMPLETED`
- **WHEN** 系统生成 live eval 报告
- **THEN** 报告 SHALL include `runtimeFixtureDraftCount`
- **AND** 报告的 `runtimeFixtureDrafts` SHALL include 对应 case 的草稿
- **AND** 草稿 SHALL include `caseId`、`sourceReportType`、`status`、`failureStage`、`failureReason`、`failureType` 和 `expectedRuntimeAction`

### Requirement: live eval runtime 草稿必须区分运行归因与质量缺口

系统 SHALL 根据 entry 的 `status`、`fallbackUsed`、`completedOutput`、`failureStage` 和 `failureReason` 区分 quota、budget guard、安全拒绝、结构校验失败、timeout、provider error、partial completion、quality miss、teaching action mismatch 和 unknown runtime failure。

#### Scenario: budget guard 样本生成草稿

- **GIVEN** live eval entry `failureReason` includes `BUDGET_GUARD_OPEN`
- **WHEN** 生成 runtime 草稿
- **THEN** 草稿的 `failureType` SHALL be `BUDGET_GUARD`
- **AND** `expectedRuntimeAction` SHALL mention 解除预算保护 or 重跑小样本 live eval

#### Scenario: 完成输出但质量未命中

- **GIVEN** live eval entry `completedOutput=true`
- **AND** `failureReason=QUALITY_MISS`
- **WHEN** 生成 runtime 草稿
- **THEN** 草稿的 `failureType` SHALL be `QUALITY_MISS`
- **AND** `expectedRuntimeAction` SHALL mention prompt/rubric/fixture 质量优化

### Requirement: live eval runtime 草稿必须脱敏

系统 SHALL 对 live eval runtime 草稿中的失败原因和输出摘要做脱敏，避免输出 API Key、token 或 provider 原始敏感错误全文。

#### Scenario: live eval 失败原因含敏感片段

- **GIVEN** entry `failureReason` 或 `outputSummary` 包含 API Key 或 token 字样
- **WHEN** 生成 runtime 草稿
- **THEN** 草稿 SHALL replace sensitive values with redacted markers
- **AND** `mustNotMention` SHALL include API Key and token
- **AND** 草稿 SHALL keep source caseId and failureType for review

### Requirement: live eval runtime 草稿必须可结构化验证

系统 SHALL 通过无需真实外部模型的测试验证 live eval runtime 草稿生成。

#### Scenario: 结构测试生成 runtime 草稿

- **WHEN** 执行 live eval 报告结构测试
- **THEN** 测试 SHALL construct fallback、partial 和 quality miss entries
- **AND** 测试 SHALL assert runtime draft count, failureType classification, expectedRuntimeAction and redaction
- **AND** 测试 SHALL not require `AI_EVAL_API_KEY`
