## ADDED Requirements

### Requirement: 规范标准库必须作为知识点以下诊断层
AI 标准库规范结构 SHALL 被解释为知识点以下的诊断层：能力点 SHALL 挂到一个主知识点，易错点 SHALL 挂到能力点，提升点 SHALL 挂到能力点或知识点。

#### Scenario: 能力点主归属知识点
- **WHEN** 系统同步或读取一个能力点
- **THEN** 能力点的 `primaryKnowledgeNodeCode` SHALL 指向 `informatics_knowledge_nodes` 中 type 为 `KNOWLEDGE_POINT` 的节点
- **AND** 能力点 MAY 保存其他相关知识节点作为检索补充

#### Scenario: 易错点归属能力点
- **WHEN** 系统同步或读取一个易错点
- **THEN** 易错点 SHALL 保存合法 `skillUnitCode`
- **AND** AI 候选包 SHALL 将该易错点放入所属能力点下面
- **AND** 易错点的知识节点集合 SHALL NOT 替代能力点归属链路

#### Scenario: 提升点归属诊断层
- **WHEN** 系统同步或读取一个提升点
- **THEN** 提升点 SHALL 优先挂到能力点
- **AND** 若提升点面向整个知识点迁移，SHALL 至少保留主知识点路径

### Requirement: 结构化候选包必须使用中文知识点路径
AI 标准库候选包 SHALL 以知识点为第一分组，并 SHALL 在 `knowledgeGroups` 中提供知识点名称、中文路径和该知识点下的能力点、易错点、提升点。

#### Scenario: 候选包按知识点分组
- **WHEN** 系统从召回结果构造 `StandardLibraryPack`
- **THEN** `knowledgeGroups[].id` SHALL 是知识点 code
- **AND** `knowledgeGroups[].name` SHALL 优先使用知识点中文名
- **AND** `knowledgeGroups[].path` SHALL 优先使用知识树中文 path

#### Scenario: 能力点下展开易错点
- **WHEN** 召回结果包含同一知识点下的能力点和易错点
- **THEN** 候选包 SHALL 将能力点放在对应知识点分组下
- **AND** 候选包 SHALL 将易错点放在所属能力点下
- **AND** 同能力点下的相邻易错点 MAY 作为上下文一起提供
