## MODIFIED Requirements

### Requirement: 标准库条目可关联知识点

系统 SHALL 允许每个 AI 标准库条目关联一个或多个信息学知识点编码，同时继续保留现有基础层错因、提高层提升点、教师编辑、启用/停用和 `StandardLibraryPack` 兼容输出行为。

#### Scenario: 创建或编辑条目时保存知识点编码

- **WHEN** 教师创建或编辑标准库条目并提交知识点编码列表
- **THEN** 系统保存这些知识点编码
- **AND** 后续查询该条目时返回相同编码列表

#### Scenario: 标准库兼容输出不被知识点字段破坏

- **WHEN** AI 诊断链路构建 `StandardLibraryPack`
- **THEN** 系统仍返回原有 `basicCauses` 和 `improvementPoints` 结构
- **AND** 现有模型输出校验不因知识点字段缺失而失败
