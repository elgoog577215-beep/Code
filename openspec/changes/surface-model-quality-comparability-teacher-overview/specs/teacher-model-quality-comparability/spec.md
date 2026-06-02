## ADDED Requirements

### Requirement: 教师端 AI 质量概览必须输出模型质量可比性
系统 SHALL 在当前作业的 `runtimeAttributionSignal` 中输出外部模型质量可比性状态、摘要和原因。

#### Scenario: recovery blocked 时不可比较
- **WHEN** 当前作业存在外部模型 runtime fallback 且 recovery status 为 `BLOCKED`
- **THEN** `runtimeAttributionSignal.qualityComparabilityStatus` SHALL be `NOT_COMPARABLE`
- **AND** `qualityComparabilityReasons` SHALL include `current recovery blocked`

#### Scenario: recovery recovered 时可比较
- **WHEN** 当前作业存在真实外部模型完成样本且 recovery status 为 `RECOVERED`
- **THEN** `runtimeAttributionSignal.qualityComparabilityStatus` SHALL be `COMPARABLE`
- **AND** `qualityComparabilitySummary` SHALL explain that current evidence can support small-batch model quality comparison

#### Scenario: 健康但无恢复上下文时不适用
- **WHEN** 当前作业没有 runtime failure、partial 或 recovery smoke context
- **THEN** `runtimeAttributionSignal.qualityComparabilityStatus` SHALL be `NOT_APPLICABLE`

### Requirement: 教师工作台必须展示模型质量可比性
系统 SHALL 在教师工作台模型归因块中展示非空且非 `NOT_APPLICABLE` 的质量可比性状态。

#### Scenario: 不可比较状态展示原因
- **WHEN** AI 质量概览返回 `qualityComparabilityStatus=NOT_COMPARABLE`
- **THEN** 教师工作台 SHALL display a quality comparability chip
- **AND** 教师工作台 SHALL display the comparability summary
- **AND** 教师工作台 SHALL display at most two comparability reasons

#### Scenario: 不适用状态不增加噪音
- **WHEN** AI 质量概览返回 `qualityComparabilityStatus=NOT_APPLICABLE`
- **THEN** 教师工作台 SHALL NOT display a quality comparability block
