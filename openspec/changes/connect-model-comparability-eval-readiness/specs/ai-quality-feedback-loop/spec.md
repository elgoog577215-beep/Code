## ADDED Requirements

### Requirement: 评测沉淀闭环必须区分 fixture ready 与模型质量 baseline ready
系统 SHALL 在 AI 质量反馈闭环中分别表达“诊断/课堂 fixture 是否可沉淀”和“真实外部模型质量 baseline 是否可对比”。

#### Scenario: fixture 可沉淀但模型质量不可比
- **WHEN** 诊断或课堂 fixture candidate exists
- **AND** external model quality comparability is `NOT_COMPARABLE`
- **THEN** AI quality feedback loop SHALL recommend fixture review
- **AND** SHALL warn that model quality baseline needs recovery or no-fallback model hits before comparison
