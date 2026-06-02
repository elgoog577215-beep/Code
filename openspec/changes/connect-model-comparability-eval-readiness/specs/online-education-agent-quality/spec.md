## ADDED Requirements

### Requirement: 评测沉淀必须区分真实模型成功、partial 和 fallback
系统 SHALL 在评测沉淀建议中区分真实外部模型成功、partial 完成、runtime fallback 和不可比状态，避免把规则兜底样本沉淀为外部模型质量 baseline。

#### Scenario: fallback 样本阻塞模型质量 baseline
- **WHEN** 当前作业没有真实模型完成样本但存在 fallback 样本
- **THEN** model quality baseline status SHALL be `BLOCKED`
- **AND** reasons SHALL include `model hits missing; fallback hits present`

#### Scenario: partial 样本只允许部分观察
- **WHEN** 当前作业存在 partial 模型样本且没有 recovery blocked
- **THEN** model quality baseline status SHALL be `PARTIAL`
