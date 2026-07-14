## ADDED Requirements

### Requirement: 教师治理台必须成为正式内容入库入口
教师端标准库治理台 SHALL 支持把新增或修订后的能力点、易错点和提升点写入正式数据库，而不是生成或要求开发者添加 seed。

#### Scenario: 教师批准候选入库
- **WHEN** 教师批准一个标准库成长候选
- **THEN** 系统 SHALL 写入规范化正式表
- **AND** 系统 SHALL 同步兼容快照
- **AND** 系统 SHALL NOT 生成新的 seed 文件或要求重启应用播种

#### Scenario: 教师手动新增正式条目
- **WHEN** 教师在治理台新增能力点、易错点或提升点
- **THEN** 系统 SHALL 直接写入正式数据库
- **AND** 新条目 SHALL 在后续 AI 导航读取中可见

### Requirement: 治理台必须提供内容审计依据
教师端 SHALL 显示正式内容的数据库来源、审核状态和最近更新时间，使教师能区分正式库内容、候选内容和历史迁移内容。

#### Scenario: 查看正式条目来源
- **WHEN** 教师查看标准库正式条目
- **THEN** 系统 SHALL 显示该条目的 code、启用状态、更新时间和来源类型
- **AND** 来源类型 SHALL 能表达人工新增、候选批准、历史迁移或兼容快照

#### Scenario: 拒绝 seed 式新增
- **WHEN** 管理员尝试通过前端或后端新增正式内容
- **THEN** 系统 SHALL 不提供 “生成 seed” 或 “导出 seed” 作为正式入库动作
