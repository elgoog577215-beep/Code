## ADDED Requirements

### Requirement: 标准库必须提供 AI 可导航目录
AI 标准库规范结构 SHALL 在现有知识树和规范化诊断层之上提供可分页、可展开的导航读取能力，使 AI 能按大章节、小章节、知识点、能力点、易错点和提升点逐层选择，而不是依赖后端本地召回候选。

#### Scenario: 读取一级目录
- **WHEN** AI 标准库导航阶段开始
- **THEN** 系统 SHALL 返回启用的一级知识目录
- **AND** 返回内容 SHALL 包含主名、别名摘要、节点类型和是否有子节点

#### Scenario: 展开知识节点
- **WHEN** AI 选择一个大章节、小章节或知识点
- **THEN** 系统 SHALL 只展开被选择节点的直接子节点或必要摘要
- **AND** 系统 SHALL NOT 一次性返回整棵标准库

#### Scenario: 展开知识点诊断层
- **WHEN** AI 选择一个 `KNOWLEDGE_POINT`
- **THEN** 系统 SHALL 返回该知识点下启用的能力点、易错点和提升点
- **AND** 易错点 SHALL 按所属能力点组织
- **AND** 提升点 SHALL 标明关联能力点或知识点

#### Scenario: 导航读取复用统一标准库
- **WHEN** AI 浏览高中和竞赛都覆盖的概念
- **THEN** 系统 SHALL 返回同一标准库节点的主名和 aliases
- **AND** 系统 SHALL NOT 创建高中库和竞赛库两套平行目录
