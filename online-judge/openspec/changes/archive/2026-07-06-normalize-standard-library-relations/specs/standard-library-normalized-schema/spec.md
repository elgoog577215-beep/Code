## MODIFIED Requirements

### Requirement: 标准库必须按能力易错提升分表存储
AI 标准库规范结构 SHALL 将能力点、易错点和提升点存储为独立实体，其中易错点 SHALL 归属于能力点，提升点 SHALL 归属于能力点或知识节点。

#### Scenario: 易错点归属能力点
- **WHEN** 同步一个 `MISTAKE_POINT` 或兼容 `BASIC_CAUSE` 条目
- **THEN** 系统 SHALL 将它保存为规范易错点
- **AND** SHALL 记录它关联的能力点 code 作为主归属
- **AND** SHALL NOT 把易错点的知识节点集合解释为主归属链路

#### Scenario: 能力点拥有主知识节点
- **WHEN** 同步或保存一个能力点
- **THEN** 系统 SHALL 保存一个主知识节点 code
- **AND** MAY 继续保存相关知识节点集合用于检索和上下文补充

### Requirement: 标准库候选包必须提供结构化视图
AI 标准库候选包 SHALL 在兼容旧字段之外提供结构化视图，表达知识节点、能力点、易错点和提升点之间的层级关系。

#### Scenario: 生成结构化候选包
- **WHEN** 系统从规范标准库生成 AI 诊断候选包
- **THEN** 候选包 SHALL 优先按能力点的主知识节点分组
- **AND** 易错点 SHALL 放在所属能力点下
- **AND** 相关知识节点 SHALL 作为补充上下文，而不是第二条主路径

#### Scenario: 兼容旧字段
- **WHEN** 候选包包含结构化视图
- **THEN** 候选包 SHALL 继续保留现有 `basicCauses`、`improvementPoints`、`skillUnits` 和 `mistakePoints` 兼容字段
- **AND** SHOULD 输出 `primaryKnowledgeNodeCode` 与 `relatedKnowledgeNodeCodes` 帮助调用方区分主路径和辅助标签
