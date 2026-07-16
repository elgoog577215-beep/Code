## ADDED Requirements

### Requirement: 父子归属不得冒充真实前置知识
知识树的 `parentCode` SHALL 只表达目录归属，`prerequisites` SHALL 只保存学习该节点前真正需要掌握的其他知识节点 code。

#### Scenario: 历史父节点伪前置被清理
- **WHEN** 历史主题或知识点的 `prerequisites` 仅等于自身 `parentCode`
- **THEN** 数据迁移 SHALL 清空该伪前置关系
- **AND** 导航路径 SHALL 继续由 `parentCode` 和 `path` 保持完整

#### Scenario: 人工精修知识点保存真实前置
- **WHEN** 首批知识点存在明确的学习先决条件
- **THEN** `prerequisites` SHALL 只引用已存在的知识节点 code
- **AND** 没有可靠先决条件时 SHALL 留空而不是复制父节点 code

### Requirement: 学科精修必须替换占位描述而非改写模板
人工精修知识点 SHALL 用概念对象、适用边界、状态或验证方法表达教学语义，并 SHALL 同步维护学习目标、典型问题和有效别名。

#### Scenario: 首批高引用知识点完成精修
- **WHEN** V2 数据质量迁移执行完成
- **THEN** 至少 20 个跨 BASIC、DS、ALGO、MATH、ENG 或 CONTEST 的高引用知识点 SHALL 不再使用“细颗粒知识点”模板描述
- **AND** 每个精修节点 SHALL 包含可观察学习目标和具体典型任务

#### Scenario: 别名只保存不同叫法
- **WHEN** 知识节点 aliases 仅重复主名
- **THEN** 数据迁移 SHALL 清空该自指别名
- **AND** 首批精修节点的 aliases SHALL 只保存高中、通用、竞赛或英文的不同叫法
