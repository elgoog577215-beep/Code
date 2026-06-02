## ADDED Requirements

### Requirement: AI 质量趋势必须暴露外部模型运行归因

系统 SHALL 在跨作业 AI 质量趋势响应中统计外部模型完成数、部分完成数、运行失败数和运行失败率。

#### Scenario: 趋势响应包含模型运行统计

- **GIVEN** 提交诊断报告包含 `aiInvocation.status=MODEL_COMPLETED`
- **AND** 另一个诊断报告包含 `aiInvocation.status=MODEL_PARTIAL_COMPLETED`
- **AND** 另一个诊断报告包含 `aiInvocation.status=MODEL_RUNTIME_FALLBACK`
- **WHEN** 教师端请求跨作业 AI 质量趋势
- **THEN** 响应 SHALL include `modelCompletedCount`
- **AND** 响应 SHALL include `modelPartialCount`
- **AND** 响应 SHALL include `modelRuntimeFailureCount`
- **AND** 响应 SHALL include `modelRuntimeFailureRate`

### Requirement: 作业趋势点必须暴露模型运行失败

系统 SHALL 在每个作业趋势点中统计该作业范围内的模型完成、部分完成、运行失败和运行失败率。

#### Scenario: 单个作业存在模型运行失败

- **GIVEN** 某个作业下至少一条提交诊断使用 `MODEL_RUNTIME_FALLBACK`
- **WHEN** 系统生成该作业的趋势点
- **THEN** 该趋势点 SHALL include `modelRuntimeFailureCount`
- **AND** 该趋势点 SHALL include `modelRuntimeFailureRate`
- **AND** 该趋势点 SHALL preserve existing correction, low confidence and prompt safety fields

### Requirement: 来源质量片段必须按运行归因拆分

系统 SHALL 在 `sourceSegments` 中暴露 `runtimeMode`、`failureStage`、`failureReason`，并按运行模式与失败归因拆分来源质量片段。

#### Scenario: 同一模型存在成功和额度失败

- **GIVEN** 两条提交诊断使用同一 provider、model、prompt 和 agent
- **AND** 第一条 `aiInvocation.status=MODEL_COMPLETED`
- **AND** 第二条 `aiInvocation.status=MODEL_RUNTIME_FALLBACK`
- **AND** 第二条 `aiInvocation.failureReason=INSUFFICIENT_QUOTA`
- **WHEN** 系统生成 `sourceSegments`
- **THEN** 成功样本和额度失败样本 SHALL appear in separate source segments
- **AND** 失败片段 SHALL include `runtimeMode`
- **AND** 失败片段 SHALL include `failureStage`
- **AND** 失败片段 SHALL include `failureReason`
- **AND** 失败片段 SHALL include `modelRuntimeFailureCount`

### Requirement: 教师工作台必须展示外部模型运行归因

教师工作台 SHALL 在跨作业 AI 质量趋势区展示模型运行失败、部分完成、作业级运行异常 badge 和来源级失败归因。

#### Scenario: 趋势中存在模型运行失败

- **GIVEN** AI 质量趋势响应包含 `modelRuntimeFailureCount` 大于 0
- **WHEN** 教师查看 AI 质量趋势区
- **THEN** 页面 SHALL display 模型失败计数
- **AND** 作业趋势点 SHALL mark 作业级模型失败
- **AND** 来源质量片段 SHALL display failure stage or failure reason when present

### Requirement: 外部模型运行归因趋势必须可验证

系统 SHALL 通过后端趋势聚合测试、前端类型检查和 OpenSpec 校验验证外部模型运行归因趋势。

#### Scenario: 运行归因趋势回归测试

- **WHEN** 执行 AI 质量趋势相关测试
- **THEN** 测试 SHALL assert 顶层、作业点和 source segment 的模型运行归因字段
- **AND** 前端类型检查 SHALL pass with the new trend fields
