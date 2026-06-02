## ADDED Requirements

### Requirement: 默认低预算外部模型 runtime

系统 SHALL 在启用外部模型 runtime 且未显式配置 `ai.external-runtime-mode` 时，默认使用 `single-call` 模式调用外部模型。

#### Scenario: 未配置 runtime mode

- **GIVEN** `ai.external-runtime-enabled=true`
- **AND** 未设置 `ai.external-runtime-mode`
- **WHEN** 提交诊断调用外部模型 runtime
- **THEN** 系统 SHALL 使用 `diagnosis-and-teaching-v2` 单次调用 prompt
- **AND** 每个诊断样本最多发起一次提交诊断模型请求
- **AND** `aiInvocation.runtimeMode` SHALL be `single-call`

#### Scenario: 显式回滚 staged

- **GIVEN** `ai.external-runtime-mode=staged`
- **WHEN** 提交诊断调用外部模型 runtime
- **THEN** 系统 SHALL 保持 staged 两阶段调用能力
- **AND** `aiInvocation.runtimeMode` SHALL be `staged`

### Requirement: 结构化记录外部模型运行归因

系统 SHALL 在 `aiInvocation` 中结构化记录外部模型运行模式、失败阶段和失败原因。

#### Scenario: single-call 成功

- **WHEN** single-call 外部模型诊断通过诊断和教学提示校验
- **THEN** `aiInvocation.status` SHALL be `MODEL_COMPLETED`
- **AND** `aiInvocation.promptVersion` SHALL be `diagnosis-and-teaching-v2`
- **AND** `aiInvocation.runtimeMode` SHALL be `single-call`
- **AND** `aiInvocation.failureStage` SHALL be blank
- **AND** `aiInvocation.failureReason` SHALL be blank

#### Scenario: single-call 部分完成

- **WHEN** single-call 外部模型诊断裁决有效但教学提示不安全或无效
- **THEN** 系统 SHALL 保留模型诊断裁决
- **AND** `aiInvocation.status` SHALL be `MODEL_PARTIAL_COMPLETED`
- **AND** `aiInvocation.runtimeMode` SHALL be `single-call`
- **AND** `aiInvocation.failureStage` SHALL identify the failed stage
- **AND** `aiInvocation.failureReason` SHALL identify the validation failure reason

#### Scenario: 外部模型运行回退

- **WHEN** 外部模型调用失败、输出无效、额度不足或被预算保护短路
- **THEN** `aiInvocation.status` SHALL be `MODEL_RUNTIME_FALLBACK`
- **AND** `aiInvocation.fallbackUsed` SHALL be true
- **AND** `aiInvocation.runtimeMode` SHALL record the active runtime mode
- **AND** `aiInvocation.failureStage` SHALL identify the failed stage
- **AND** `aiInvocation.failureReason` SHALL identify the failure reason

### Requirement: live eval 使用结构化归因

live eval 报告 SHALL 优先使用 `aiInvocation` 的结构化运行归因生成 per-case failure reason。

#### Scenario: 结构化失败字段存在

- **GIVEN** `aiInvocation.status=MODEL_RUNTIME_FALLBACK`
- **AND** `aiInvocation.failureStage=DIAGNOSIS_AND_TEACHING`
- **AND** `aiInvocation.failureReason=INSUFFICIENT_QUOTA`
- **WHEN** live eval 生成报告条目
- **THEN** 条目 `failureReason` SHALL include `MODEL_RUNTIME_FALLBACK`
- **AND** 条目 `failureReason` SHALL include `DIAGNOSIS_AND_TEACHING`
- **AND** 条目 `failureReason` SHALL include `INSUFFICIENT_QUOTA`

#### Scenario: 旧数据缺失结构化字段

- **GIVEN** 旧 `aiInvocation` 不包含 `failureStage` 或 `failureReason`
- **WHEN** live eval 或诊断报告读取该记录
- **THEN** 系统 SHALL 保持兼容
- **AND** 可以回退到 `uncertainty` 或 trace 文本归因。
