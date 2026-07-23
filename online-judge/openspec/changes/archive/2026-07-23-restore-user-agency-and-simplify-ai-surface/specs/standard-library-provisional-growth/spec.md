## MODIFIED Requirements

### Requirement: 临时节点必须聚合并按门禁晋升
系统 SHALL 按 layer、父节点和规范化 code 聚合同类候选，SHALL 按独立来源提交累计出现次数，并 MUST 仅在教师或标准库管理员明确审核批准后写入规范化正式标准库。

#### Scenario: 同类候选来自新的独立提交
- **WHEN** 同一父节点下相同 layer 和规范化 code 的候选来自新的来源提交
- **THEN** 系统 SHALL 记录该来源提交并增加 occurrenceCount
- **AND** 系统 SHALL 合并新证据

#### Scenario: 同一提交重复生成同类候选
- **WHEN** 同一来源提交因重试、刷新或重复生成再次提出相同候选
- **THEN** 系统 SHALL 合并证据并更新时间
- **AND** 系统 MUST NOT 增加 occurrenceCount

#### Scenario: 候选达到高置信度和高频门槛
- **WHEN** 候选父节点真实存在、证据状态为 `SUPPORTED` 且独立来源和置信度达到质量门槛
- **THEN** 系统 SHALL 将候选保留为待审核状态
- **AND** 系统 MUST NOT 自动写入正式标准库

#### Scenario: 教师明确批准候选
- **WHEN** 教师或标准库管理员在治理台确认候选内容并执行批准或合并
- **THEN** 系统 SHALL 在完成父节点、证据和去重校验后写入正式标准库
- **AND** 系统 SHALL 保留审核人动作、来源证据和回滚信息
