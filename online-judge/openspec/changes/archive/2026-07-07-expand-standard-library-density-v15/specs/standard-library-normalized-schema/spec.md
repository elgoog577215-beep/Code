## ADDED Requirements

### Requirement: 标准库应直接补齐高频知识点覆盖密度
AI 标准库 SHALL 能通过版本化 seed 批次直接补充高频薄弱知识点下的能力点、易错点和提升点，而不是只输出覆盖报告等待人工补库。

#### Scenario: 发现高频薄弱知识点
- **WHEN** 系统维护者发现高频知识点下能力点、易错点或提升点密度不足
- **THEN** 标准库 SHALL 通过新增 seed 批次直接补充正式候选条目
- **AND** 新增条目 SHALL 按“知识点 -> 能力点 -> 易错点/提升点”组织
- **AND** 每个新增主题 SHOULD 至少包含 1 个能力点、多个易错点和 1 个提升点

#### Scenario: 新增密度扩展批次
- **WHEN** 标准库新增覆盖密度扩展 seed
- **THEN** 每条 seed SHALL 引用现有知识树节点
- **AND** 每条 seed SHALL 至少包含一个 `KNOWLEDGE_POINT` 锚点
- **AND** 测试 SHALL 校验新增条目不是模板化占位内容
