## ADDED Requirements

### Requirement: 标准库候选包必须提供结构化视图
AI 标准库候选包 SHALL 在兼容旧字段之外提供结构化视图，表达知识节点、能力点、易错点和提升点之间的层级关系。

#### Scenario: 生成结构化候选包
- **WHEN** 系统从规范标准库生成 AI 诊断候选包
- **THEN** 候选包 SHALL 包含按知识节点分组的能力点、易错点和提升点结构

#### Scenario: 兼容旧字段
- **WHEN** 候选包包含结构化视图
- **THEN** 候选包 SHALL 继续保留现有 `basicCauses`、`improvementPoints`、`skillUnits` 和 `mistakePoints` 兼容字段

### Requirement: 标准库结构视图不得替代当前提交证据
标准库结构视图 SHALL 只作为候选地图和命名体系，AI 诊断 MUST 使用当前提交代码、评测结果、错误日志和 evidenceRefs 判断是否命中。

#### Scenario: 候选存在但证据不足
- **WHEN** 标准库结构视图中存在相近易错点但当前提交证据不足
- **THEN** AI 诊断 SHALL NOT 强制输出该易错点为 HIT
