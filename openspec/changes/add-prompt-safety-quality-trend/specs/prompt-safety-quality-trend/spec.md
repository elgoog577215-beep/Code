## ADDED Requirements

### Requirement: 跨作业 AI 质量趋势统计提示安全事件

系统 SHALL 在跨作业 AI 质量趋势响应中统计提示安全事件数量、安全降级数量、高风险安全降级数量和提示安全事件率。

#### Scenario: 趋势中存在高泄题诊断和中高风险提示安全检查

- **GIVEN** 多个作业下存在已分析提交
- **AND** 其中一个提交的诊断 `answerLeakRisk` 为 `HIGH`
- **AND** 另一个提交存在 `riskLevel` 为 `MEDIUM` 或 `HIGH` 的 `HintSafetyCheck`
- **WHEN** 教师端请求跨作业 AI 质量趋势
- **THEN** 响应中的 `promptSafetyIncidentCount` 等于高泄题诊断数量加中高风险提示安全检查数量
- **AND** `promptSafetyDowngradeCount` 等于中高风险提示安全检查数量
- **AND** `promptSafetyHighRiskDowngradeCount` 只统计 `HIGH` 安全检查
- **AND** `promptSafetyIncidentRate` 按已分析提交数计算

#### Scenario: LOW 提示安全检查不计入安全事件

- **GIVEN** 某提交只有 `riskLevel` 为 `LOW` 的 `HintSafetyCheck`
- **WHEN** 构建跨作业 AI 质量趋势
- **THEN** 该检查不增加 `promptSafetyIncidentCount`
- **AND** 该检查不增加 `promptSafetyDowngradeCount`

### Requirement: 作业趋势点展示提示安全事件

系统 SHALL 在每个作业趋势点中展示该作业内的提示安全事件数量、安全降级数量、高风险安全降级数量和提示安全事件率。

#### Scenario: 某作业触发提示安全降级

- **GIVEN** 某作业下存在一个已分析提交
- **AND** 该提交存在 `riskLevel` 为 `HIGH` 的 `HintSafetyCheck`
- **WHEN** 构建跨作业 AI 质量趋势
- **THEN** 该作业趋势点的 `promptSafetyIncidentCount` 为 1
- **AND** 该作业趋势点的 `promptSafetyDowngradeCount` 为 1
- **AND** 该作业趋势点的 `promptSafetyHighRiskDowngradeCount` 为 1

### Requirement: 来源分段归因提示安全事件

系统 SHALL 在 AI 质量来源分段中归因提示安全事件和提示安全降级。

#### Scenario: 安全降级提交有诊断来源信息

- **GIVEN** 一个提交有 `SubmissionAnalysis.aiInvocation` 信息
- **AND** 该提交存在中高风险 `HintSafetyCheck`
- **WHEN** 构建跨作业 AI 质量趋势
- **THEN** 对应 `SourceQualitySegment` 的 `promptSafetyDowngradeCount` 增加
- **AND** 对应 `SourceQualitySegment` 的 `promptSafetyIncidentCount` 增加
- **AND** `promptSafetyIncidentRate` 按该来源分段的已分析提交数计算

### Requirement: 教师端趋势区展示提示安全趋势

教师端 SHALL 在跨作业 AI 质量趋势区展示提示安全事件和安全降级读数，并在作业趋势点和来源质量片段中标记安全事件。

#### Scenario: 跨作业趋势存在提示安全事件

- **GIVEN** 跨作业 AI 质量趋势响应包含 `promptSafetyIncidentCount` 大于 0
- **WHEN** 教师查看跨作业 AI 质量趋势区
- **THEN** 顶部状态显示需要复核
- **AND** 指标区展示提示安全事件数量
- **AND** 作业趋势点中存在对应安全标记
- **AND** 来源质量片段展示对应安全事件数量
