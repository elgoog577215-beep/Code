## MODIFIED Requirements

### Requirement: 临时节点必须聚合并按门禁晋升
系统 SHALL 按 layer、父节点和规范化 code 聚合同类候选，SHALL 按独立来源提交累计出现次数，并 SHALL 仅在配置门禁全部满足时自动写入规范化正式标准库。

#### Scenario: 同类候选来自新的独立提交
- **WHEN** 同一父节点下相同 layer 和规范化 code 的候选来自新的来源提交
- **THEN** 系统 SHALL 记录该来源提交并增加 occurrenceCount
- **AND** 系统 SHALL 合并新证据

#### Scenario: 同一提交重复生成同类候选
- **WHEN** 同一来源提交因重试、刷新或重复生成再次提出相同候选
- **THEN** 系统 SHALL 合并证据并更新时间
- **AND** 系统 MUST NOT 增加 occurrenceCount

#### Scenario: 候选没有可验证来源提交
- **WHEN** 候选缺少来源提交 ID
- **THEN** 系统 MAY 保存候选供教师治理
- **AND** 重复写入 MUST NOT 使其达到自动晋升的独立出现次数门槛

#### Scenario: 满足自动晋升门禁
- **WHEN** 候选父节点真实存在、证据状态为 `SUPPORTED`、独立来源提交数量和置信度达到配置阈值且正式库无重复项
- **THEN** 系统 SHALL 将候选写入规范化正式标准库
- **AND** 系统 SHALL 保留全部独立来源、候选审计与回滚信息

#### Scenario: 未满足自动晋升门禁
- **WHEN** 任一父节点、证据、置信度、独立来源提交数量或去重门禁不满足
- **THEN** 系统 SHALL 保留候选为临时状态
- **AND** 系统 SHALL NOT 写入正式标准库
