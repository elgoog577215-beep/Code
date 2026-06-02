## ADDED Requirements

### Requirement: AI 质量概览必须输出外部模型恢复状态

系统 SHALL 在 `runtimeAttributionSignal` 中输出外部模型 recovery 状态，表达当前作业是否已经证明外部模型从 fallback、部分完成或运行失败中恢复。

#### Scenario: 运行失败后仍无恢复证据

- **GIVEN** 当前作业包含 `aiInvocation.status=MODEL_RUNTIME_FALLBACK`
- **AND** 没有任何样本满足恢复检查
- **WHEN** 教师端请求 AI 质量概览
- **THEN** `runtimeAttributionSignal.recoveryStatus` SHALL be `BLOCKED`
- **AND** `runtimeAttributionSignal.recoverySmokeRecommended` SHALL be `true`
- **AND** `runtimeAttributionSignal.recoveryBlockedReasons` SHALL explain the missing checks

#### Scenario: 运行失败后已有恢复证据

- **GIVEN** 当前作业包含运行失败样本
- **AND** 同一作业存在 `aiInvocation.status=MODEL_COMPLETED`
- **AND** 该完成样本未 fallback、包含结构化错因标签、包含 evidence refs、没有高泄题风险
- **WHEN** 教师端请求 AI 质量概览
- **THEN** `runtimeAttributionSignal.recoveryStatus` SHALL be `RECOVERED`
- **AND** `runtimeAttributionSignal.recoveryPassedChecks` SHALL include the passed checks

### Requirement: stream 恢复检查必须验证 content chunk

系统 SHALL 在 stream 外部模型样本中把 `streamContentChunkCount>0` 作为 recovery 检查条件。

#### Scenario: stream 无内容导致恢复阻塞

- **GIVEN** 当前作业的运行失败样本使用 `transportMode=stream`
- **AND** `streamContentChunkCount=0`
- **WHEN** 系统生成运行归因信号
- **THEN** `runtimeAttributionSignal.recoveryBlockedReasons` SHALL include stream content chunk missing
- **AND** `runtimeAttributionSignal.recoverySmokeRequiredChecks` SHALL include `streamContentChunkCount>0`

### Requirement: 无恢复上下文时不得误报恢复阻塞

系统 SHALL 仅在存在运行失败、部分完成、fallback 或 recovery smoke 推荐上下文时输出 `BLOCKED` 或 `RECOVERED`，普通健康作业 SHALL 输出 `NOT_APPLICABLE`。

#### Scenario: 普通健康作业没有恢复上下文

- **GIVEN** 当前作业只有健康的 `MODEL_COMPLETED` 样本
- **WHEN** 教师端请求 AI 质量概览
- **THEN** `runtimeAttributionSignal.recoveryStatus` SHALL be `NOT_APPLICABLE`
- **AND** `runtimeAttributionSignal.recoverySmokeRecommended` SHALL be `false`

### Requirement: 模型运行质量维度必须消费 recovery 状态

系统 SHALL 让 `MODEL_RUNTIME` 质量维度和运行归因推荐动作包含 recovery 状态，使教师能区分外部模型仍阻塞和已有恢复证据。

#### Scenario: recovery blocked 进入模型运行建议

- **GIVEN** `runtimeAttributionSignal.recoveryStatus=BLOCKED`
- **WHEN** 系统生成 AI 质量概览
- **THEN** `MODEL_RUNTIME` quality dimension summary SHALL mention recovery blocked
- **AND** its recommended action SHALL mention recovery smoke or single external model verification

### Requirement: 外部模型恢复状态必须可验证且不泄密

系统 SHALL 通过后端测试和 OpenSpec 校验验证 recovery 状态字段，并且 SHALL NOT 输出 API Key、token、header 或 provider 原始响应体。

#### Scenario: recovery 状态回归测试

- **WHEN** 执行 AI 质量概览相关测试
- **THEN** 测试 SHALL assert `RECOVERED`、`BLOCKED` 和 `NOT_APPLICABLE`
- **AND** 测试 SHALL assert recovery smoke metadata does not contain API key or token
