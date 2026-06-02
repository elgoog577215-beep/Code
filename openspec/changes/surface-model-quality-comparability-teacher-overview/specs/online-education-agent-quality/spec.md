## ADDED Requirements

### Requirement: 外部模型恢复状态必须转化为质量对比决策
系统 SHALL 将当前作业的外部模型恢复状态、真实模型完成样本和 fallback 样本转化为可解释的质量对比决策信号。

#### Scenario: fallback 命中不能代表外部模型质量
- **WHEN** 当前作业没有真实模型完成样本但存在 fallback 样本
- **THEN** 质量对比决策 SHALL be `NOT_COMPARABLE`
- **AND** 决策原因 SHALL include `model hits missing; fallback hits present`

#### Scenario: partial 输出只能部分比较
- **WHEN** 当前作业存在 `MODEL_PARTIAL_COMPLETED` 样本但没有 recovery blocked
- **THEN** 质量对比决策 SHALL be `PARTIAL`
- **AND** 决策原因 SHALL include `partial model outputs present`
