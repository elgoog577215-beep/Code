## ADDED Requirements

### Requirement: 成长候选必须支持审核状态流转
标准库成长候选 SHALL 支持教师查看、编辑、批准、合并、拒绝和忽略，并保留来源与回滚信息。

#### Scenario: 教师批准候选
- **WHEN** 教师批准待审核成长候选
- **THEN** 系统 SHALL 将候选写入正式标准库条目
- **AND** 系统 SHALL 保留候选来源、审核状态和回滚说明

#### Scenario: 教师拒绝候选
- **WHEN** 教师拒绝成长候选
- **THEN** 系统 SHALL 保留拒绝原因
- **AND** 系统 SHALL NOT 将该候选写入正式标准库

#### Scenario: 重复候选聚合
- **WHEN** 同一层级和建议 ID 的候选再次出现
- **THEN** 系统 SHALL 增加出现次数并合并证据线索
- **AND** 系统 SHALL 保留为教师可审核记录
