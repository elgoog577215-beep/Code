## ADDED Requirements

### Requirement: 真实外部模型可比性必须支持跨作业来源决策
系统 SHALL 将当前作业级外部模型可比性扩展到跨作业来源片段，帮助维护者判断 provider/model/prompt/runtime 组合是否能代表真实外部模型质量。

#### Scenario: fallback 来源不能代表真实外部模型质量
- **WHEN** source segment 没有真实模型完成样本但存在 fallback 样本
- **THEN** source quality comparability SHALL be `NOT_COMPARABLE`
- **AND** comparability reasons SHALL include `model hits missing; fallback hits present`

#### Scenario: 恢复来源可以继续做小批量对比
- **WHEN** source segment recovery status is `RECOVERED` and model completed count is greater than 0
- **THEN** source quality comparability SHALL be `COMPARABLE`
