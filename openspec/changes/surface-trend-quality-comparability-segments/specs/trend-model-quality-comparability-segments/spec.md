## ADDED Requirements

### Requirement: 跨作业来源片段必须输出模型质量可比性
系统 SHALL 在 `AiQualityTrendResponse.sourceSegments` 中为每个来源片段输出外部模型质量可比性状态、摘要和原因。

#### Scenario: blocked 来源片段不可比较
- **WHEN** source segment 的 recovery status 为 `BLOCKED`
- **THEN** `qualityComparabilityStatus` SHALL be `NOT_COMPARABLE`
- **AND** `qualityComparabilityReasons` SHALL include `current recovery blocked`

#### Scenario: recovered 来源片段可比较
- **WHEN** source segment 有真实模型完成样本且 recovery status 为 `RECOVERED`
- **THEN** `qualityComparabilityStatus` SHALL be `COMPARABLE`
- **AND** `qualityComparabilitySummary` SHALL explain that the source can support small-batch model quality comparison

#### Scenario: partial 来源片段部分可比较
- **WHEN** source segment 存在 `MODEL_PARTIAL_COMPLETED` 样本且 recovery 没有 blocked
- **THEN** `qualityComparabilityStatus` SHALL be `PARTIAL`
- **AND** `qualityComparabilityReasons` SHALL include `partial model outputs present`

### Requirement: 教师工作台来源质量必须展示可比性
系统 SHALL 在教师工作台“来源质量”列表中展示非空且非 `NOT_APPLICABLE` 的来源质量可比性状态。

#### Scenario: 来源不可比时展示原因
- **WHEN** source segment 返回 `qualityComparabilityStatus=NOT_COMPARABLE`
- **THEN** 教师工作台 SHALL display a source quality comparability chip
- **AND** 教师工作台 SHALL display the comparability summary
- **AND** 教师工作台 SHALL display at most two comparability reasons

#### Scenario: 来源不适用时不增加噪音
- **WHEN** source segment 返回 `qualityComparabilityStatus=NOT_APPLICABLE`
- **THEN** 教师工作台 SHALL NOT display a source quality comparability block
