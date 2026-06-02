## ADDED Requirements

### Requirement: evalReadiness 必须输出模型质量 baseline 可比性
系统 SHALL 在 `AiQualityOverviewResponse.evalReadiness` 中输出当前作业是否适合沉淀真实外部模型质量 baseline。

#### Scenario: 运行可比性不可比时 baseline blocked
- **WHEN** `runtimeAttributionSignal.qualityComparabilityStatus` is `NOT_COMPARABLE`
- **THEN** `evalReadiness.modelQualityBaselineStatus` SHALL be `BLOCKED`
- **AND** `modelQualityBaselineReasons` SHALL include the runtime comparability reasons

#### Scenario: 运行可比性可比时 baseline ready
- **WHEN** `runtimeAttributionSignal.qualityComparabilityStatus` is `COMPARABLE`
- **THEN** `evalReadiness.modelQualityBaselineStatus` SHALL be `READY`
- **AND** `modelQualityBaselineSummary` SHALL explain that model quality baseline can be compared in a small batch

#### Scenario: partial 模型证据时 baseline partial
- **WHEN** `runtimeAttributionSignal.qualityComparabilityStatus` is `PARTIAL`
- **THEN** `evalReadiness.modelQualityBaselineStatus` SHALL be `PARTIAL`

### Requirement: eval readiness 状态必须保留 fixture 沉淀语义
系统 SHALL NOT 将诊断 fixture 或课堂介入 fixture 的 ready 状态仅因为模型质量 baseline 不可比而整体降级。

#### Scenario: 诊断候选 ready 但模型质量 baseline blocked
- **WHEN** `evalReadiness.candidateCount` is greater than 0
- **AND** model quality baseline status is `BLOCKED`
- **THEN** `evalReadiness.status` SHALL remain `READY`
- **AND** `evalReadiness.modelQualityBaselineStatus` SHALL be `BLOCKED`

### Requirement: 教师工作台必须展示模型质量 baseline 状态
系统 SHALL 在教师工作台“评测沉淀”块展示非空且非 `NOT_APPLICABLE` 的模型质量 baseline 状态。

#### Scenario: baseline blocked 时展示原因
- **WHEN** AI 质量概览返回 `modelQualityBaselineStatus=BLOCKED`
- **THEN** 教师工作台 SHALL display a model baseline status chip
- **AND** 教师工作台 SHALL display the baseline summary
- **AND** 教师工作台 SHALL display at most two baseline reasons
