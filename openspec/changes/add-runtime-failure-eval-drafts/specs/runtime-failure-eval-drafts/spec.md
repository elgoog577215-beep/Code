## ADDED Requirements

### Requirement: 外部模型运行失败必须导出为 runtime eval 草稿

系统 SHALL 在诊断 eval fixture 草稿导出中，将外部模型运行失败样本转换为 runtime fixture 草稿。

#### Scenario: 作业内存在 runtime fallback 分析

- **GIVEN** 作业内某次提交的 `aiInvocation.status=MODEL_RUNTIME_FALLBACK`
- **OR** `aiInvocation.fallbackUsed=true`
- **WHEN** 教师预览诊断 eval fixture 草稿
- **THEN** 响应 SHALL include `runtimeFixtureCount`
- **AND** 响应的 `runtimeFixtures` SHALL 包含该提交的 runtime 草稿
- **AND** 草稿 SHALL include `status`、`runtimeMode`、`failureStage`、`failureReason`、`failureType` 和 `expectedRuntimeAction`
- **AND** 草稿 SHALL include 与提交或诊断证据相关的 `evidenceRefs`

### Requirement: runtime 草稿必须区分主要归因类别

系统 SHALL 根据 `aiInvocation.failureReason`、`failureStage`、`status` 和 `fallbackUsed` 区分 quota、budget guard、安全拒绝、结构校验失败、超时、provider 错误、部分完成和未知运行失败。

#### Scenario: 额度不足形成 runtime 草稿

- **GIVEN** `aiInvocation.failureReason` includes `INSUFFICIENT_QUOTA`
- **WHEN** 系统生成 runtime fixture 草稿
- **THEN** 草稿的 `failureType` SHALL be `QUOTA_LIMIT`
- **AND** `expectedRuntimeAction` SHALL mention ModelScope 额度 or 降低 live eval 调用规模

#### Scenario: 部分完成形成 runtime 草稿

- **GIVEN** `aiInvocation.status=MODEL_PARTIAL_COMPLETED`
- **WHEN** 系统生成 runtime fixture 草稿
- **THEN** 草稿的 `failureType` SHALL be `PARTIAL_COMPLETION`
- **AND** `expectedRuntimeAction` SHALL mention 保留可用诊断 and 复核教学提示阶段

### Requirement: runtime 草稿必须保护密钥和敏感错误信息

系统 SHALL 避免在 runtime fixture 草稿中输出 API Key、token 或 provider 原始敏感错误全文。

#### Scenario: failureReason 含有敏感片段

- **GIVEN** `aiInvocation.failureReason` 包含 API Key、token 或较长 provider 错误文本
- **WHEN** 系统生成 runtime fixture 草稿
- **THEN** `failureReason` SHALL be a sanitized summary
- **AND** `mustNotMention` SHALL include API Key and token
- **AND** `sourceMaterial.anonymizationNote` SHALL state that raw provider error text and secrets are excluded

### Requirement: 教师端必须预览 runtime 草稿

教师工作台 SHALL 在 Fixture 草稿预览中展示 runtime fixture 计数、失败类别和推荐动作，并在 JSON 预览中包含 runtime fixtures。

#### Scenario: fixture draft response 包含 runtime 草稿

- **GIVEN** `DiagnosisEvalFixtureDraft` response 包含 `runtimeFixtures`
- **WHEN** 教师打开 Fixture 草稿预览
- **THEN** 页面 SHALL display runtime fixture count
- **AND** 页面 SHALL display visible runtime failure type and expected runtime action
- **AND** JSON preview SHALL include `runtimeFixtures`

### Requirement: runtime eval 草稿必须可验证

系统 SHALL 通过后端导出测试、前端类型检查和 OpenSpec 校验验证 runtime eval 草稿能力。

#### Scenario: runtime 草稿回归测试

- **WHEN** 执行课堂服务相关测试
- **THEN** 测试 SHALL assert `runtimeFixtureCount` and `runtimeFixtures`
- **AND** 测试 SHALL assert failure classification and sanitized output
- **AND** 前端 typecheck SHALL pass with the new fixture fields
