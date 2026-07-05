## ADDED Requirements

### Requirement: 标准库结构视图应表达教学参考关系
AI 标准库规范结构 SHALL 表达知识树、能力点、易错点和提升点之间的教学参考关系；该结构 SHALL 帮助外接大模型统一命名和定位颗粒度，但 SHALL NOT 被解释为当前提交的证据或强制答案。

#### Scenario: 参考包传入 AI 诊断链路
- **WHEN** 系统把标准库候选包传给 AI 诊断链路
- **THEN** 候选包 SHALL 保留知识节点、能力点、易错点和提升点结构
- **AND** prompt SHALL 明确该结构是参考规范包，不是强制答案表

#### Scenario: 结构没有覆盖真实问题
- **WHEN** AI 诊断发现标准库结构没有覆盖当前提交的真实问题
- **THEN** 输出 SHALL 可以保留库外发现或空 ID
- **AND** 标准库成长流程 MAY 使用该发现作为候选线索
