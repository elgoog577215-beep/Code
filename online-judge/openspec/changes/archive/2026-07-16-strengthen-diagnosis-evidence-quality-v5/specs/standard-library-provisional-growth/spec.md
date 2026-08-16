## ADDED Requirements

### Requirement: 临时候选被诊断使用后必须进入事实账本
系统 SHALL 在 advice 选择或生成临时候选时，把稳定候选 code 同时保存到学生可见建议、成长候选和诊断事实，并 SHALL 能由该 code 追溯唯一候选及其父知识点。

#### Scenario: 新临时候选形成学生建议
- **WHEN** 后端在有效父知识点下为建议生成临时候选
- **THEN** 建议、成长候选和诊断事实 SHALL 使用同一稳定 code
- **AND** 成长候选 SHALL 保存有效 `parentKnowledgeNodeCode`

#### Scenario: 已有临时候选再次被选择
- **WHEN** AI 在诊断层选择已存在的临时候选
- **THEN** 新事实 SHALL 保存被选择候选的 code
- **AND** 系统 SHALL 按候选 code 和独立来源提交聚合使用证据
