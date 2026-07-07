## ADDED Requirements

### Requirement: 知识树末端必须是知识点
信息学知识树 SHALL 只表达学科地图，树形层级 SHALL 收束到 `KNOWLEDGE_POINT`；能力点、易错点和提升点 SHALL NOT 作为知识树节点混入领域、章节或 topic 层级。

#### Scenario: 知识树同步保持末端语义
- **WHEN** 系统同步信息学知识树 seed
- **THEN** 领域、章节和 topic SHALL 继续作为知识地图上层结构
- **AND** 可诊断的最细学习对象 SHALL 使用 `KNOWLEDGE_POINT`
- **AND** 能力点、易错点和提升点 SHALL 通过标准库诊断层挂在知识点下

#### Scenario: 高中术语进入知识点而不是新库
- **WHEN** 系统吸收高中真题术语
- **THEN** 该术语 SHALL 复用现有知识树路径并落到 topic 或 knowledge point
- **AND** 系统 SHALL NOT 为能力点或易错点额外创建高中专用知识树分支
