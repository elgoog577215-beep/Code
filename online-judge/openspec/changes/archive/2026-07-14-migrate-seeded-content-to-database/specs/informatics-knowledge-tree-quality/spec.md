## ADDED Requirements

### Requirement: 知识树正式内容必须由数据库维护
信息学知识树正式节点 SHALL 由数据库中的 `informatics_knowledge_nodes` 维护；运行时 SHALL NOT 通过 `InformaticsKnowledgeSeedCatalog` 或启动播种器扩建正式知识树。

#### Scenario: 读取知识树
- **WHEN** 前端、AI 导航或教师端读取知识树
- **THEN** 系统 SHALL 从数据库读取启用节点
- **AND** 系统 SHALL NOT 在读取前从代码 seed 自动补齐节点

#### Scenario: 新增知识节点
- **WHEN** 需要新增大章节、小章节、知识点或小知识点
- **THEN** 系统 SHALL 通过管理 API、迁移脚本或治理流程写入 `informatics_knowledge_nodes`
- **AND** 系统 SHALL NOT 要求新增 `InformaticsKnowledgeSeedCatalog` 内容

### Requirement: 知识树迁移必须保留路径和别名
历史 seed 中需要保留的知识树节点 SHALL 在一次性迁移中写入数据库，并保留路径、父子关系、阶段、难度、别名和学习目标。

#### Scenario: 迁移节点
- **WHEN** 执行知识树历史 seed 迁移
- **THEN** 系统 SHALL 使用节点 code 幂等 upsert
- **AND** SHALL 保留 parentCode、type、name、path、stage、difficulty、aliases、prerequisites、learningObjectives 和 typicalProblems

#### Scenario: 验证导航路径
- **WHEN** 迁移完成后
- **THEN** 系统 SHALL 验证代表性知识点可以沿父子路径导航
- **AND** 标准库能力点的 `primaryKnowledgeNodeCode` SHALL 指向存在的数据库知识点
