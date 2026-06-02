## ADDED Requirements

### Requirement: AI 质量概览必须输出运行归因行动信号

系统 SHALL 在 AI 质量概览中输出 `runtimeAttributionSignal`，将外部模型运行失败和部分完成归因成可行动信号。

#### Scenario: 额度不足形成主导归因

- **GIVEN** 当前作业包含 `aiInvocation.status=MODEL_RUNTIME_FALLBACK`
- **AND** `aiInvocation.failureReason` includes `INSUFFICIENT_QUOTA`
- **WHEN** 教师端请求 AI 质量概览
- **THEN** 响应 SHALL include `runtimeAttributionSignal`
- **AND** `runtimeAttributionSignal.primaryFailureType` SHALL be `QUOTA_LIMIT`
- **AND** `runtimeAttributionSignal.recommendedAction` SHALL mention 检查额度 or 降低评测调用规模
- **AND** `runtimeAttributionSignal.evidenceRefs` SHALL include related diagnosis evidence

### Requirement: 运行归因必须区分主要失败类别

系统 SHALL 根据 `aiInvocation.failureReason`、`failureStage`、`status` 和 `fallbackUsed` 区分额度、预算保护、安全拒绝、校验失败、超时、provider 错误、部分完成和未知运行失败。

#### Scenario: 多种失败原因同时出现

- **GIVEN** 当前作业同时存在 `INSUFFICIENT_QUOTA` 和 `BUDGET_GUARD_OPEN`
- **WHEN** 系统生成运行归因信号
- **THEN** 系统 SHALL count both failure types
- **AND** `primaryFailureType` SHALL represent the most frequent type
- **AND** ties SHALL prefer quota or budget guard before unknown runtime failure

### Requirement: 模型运行质量维度必须消费运行归因

系统 SHALL 让 `MODEL_RUNTIME` 质量维度和 `improvementPriorities` 使用运行归因信号的摘要、证据和推荐动作。

#### Scenario: 模型运行失败进入优先改进

- **GIVEN** `runtimeAttributionSignal.status=ACTION_NEEDED`
- **WHEN** 系统生成 AI 质量概览
- **THEN** `MODEL_RUNTIME` quality dimension SHALL use the signal summary
- **AND** its `recommendedAction` SHALL equal the signal recommended action
- **AND** an improvement priority for `MODEL_RUNTIME` SHALL carry the same action

### Requirement: 教师工作台必须展示运行归因行动建议

教师工作台 SHALL 在 AI 质量信号摘要中展示运行归因主导类别、摘要和推荐动作。

#### Scenario: 当前作业存在运行归因信号

- **GIVEN** AI 质量概览响应包含 `runtimeAttributionSignal`
- **WHEN** 教师查看 AI 质量信号
- **THEN** 页面 SHALL display 模型归因
- **AND** 页面 SHALL display the recommended action

### Requirement: 运行归因行动信号必须可验证

系统 SHALL 通过后端概览测试、前端类型检查和 OpenSpec 校验验证运行归因行动信号。

#### Scenario: 运行归因行动信号回归测试

- **WHEN** 执行 AI 质量概览相关测试
- **THEN** 测试 SHALL assert `runtimeAttributionSignal`
- **AND** 前端类型检查 SHALL pass with the new signal fields
