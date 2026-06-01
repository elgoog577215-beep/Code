## ADDED Requirements

### Requirement: AI 质量反馈必须消费教师介入闭环信号

系统 SHALL 将教师介入成效视为 AI 教育能力质量的一部分，并能识别教师已采纳/调整但仍无后续证据、仍命中同类错因或需要升级干预的情况。

#### Scenario: 教师介入后仍卡同类错因

- **WHEN** 某作业存在教师采纳或调整后的复盘建议
- **AND** 后续提交仍命中该建议证据标签
- **THEN** AI 质量概览 MUST 暴露 `TEACHER_INTERVENTION_LOOP` 维度
- **AND** 该维度状态 MUST 为 `ACTION_NEEDED`
- **AND** 改进优先级 MUST 建议降低提示颗粒度、补充更小样例或升级教师介入

#### Scenario: 教师介入后等待后续证据

- **WHEN** 某作业存在教师采纳或调整后的复盘建议
- **AND** 尚无相关后续提交
- **THEN** AI 质量概览 MUST 将教师介入闭环维度标记为 `WATCH`
- **AND** 摘要 MUST 说明当前缺少可观察的干预后证据

#### Scenario: 教师介入后已有改善

- **WHEN** 某作业存在教师采纳或调整后的复盘建议
- **AND** 后续提交已经通过或不再命中原证据标签
- **THEN** AI 质量概览 MUST 将教师介入闭环维度计为健康或观察状态
- **AND** 证据引用 MUST 指向相关教师反馈和后续提交
