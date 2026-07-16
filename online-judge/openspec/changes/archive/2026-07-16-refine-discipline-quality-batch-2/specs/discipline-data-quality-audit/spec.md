## ADDED Requirements

### Requirement: 学科质量批次必须以同口径生产基线证明债务下降
每个学科内容精修批次 SHALL 在迁移前固化生产数据库、数据粒度、指标定义和债务数量，并 SHALL 在迁移后用相同查询证明目标债务下降且结构阻断项没有回归。

#### Scenario: 第二批精修完成
- **WHEN** Flyway V3 在生产备份的隔离恢复库执行完成
- **THEN** `informatics-knowledge-discipline-v2` 知识点 SHALL 不少于 30 个
- **AND** 累计人工精修知识点 SHALL 不少于 54 个
- **AND** 模板化知识点描述 SHALL 从 562 降到不高于 532
- **AND** 缺少提升点的启用能力点 SHALL 从 65 降到不高于 53

#### Scenario: 结构问题在内容批次中回归
- **WHEN** 第二批内容迁移造成孤儿引用、重复启用条目、无效前置、失效兼容映射或分类实现词重新出现
- **THEN** 学科质量门禁 SHALL 失败
- **AND** 系统 SHALL NOT 用内容债务允许值掩盖结构错误

### Requirement: 正式提升点三表一致性必须成为发布门禁
质量检查 SHALL 比较第二批正式提升点在规范化主表、平铺兼容快照和 legacy mapping 中的启用 code 集合及关键归属，任一缺失或错配 SHALL 阻断发布。

#### Scenario: 规范化提升点缺少兼容快照
- **WHEN** `informatics-discipline-quality-v2` 提升点存在于规范化主表但不存在同 code 的启用 `IMPROVEMENT_POINT` 快照
- **THEN** 学科质量门禁 SHALL 失败

#### Scenario: 兼容映射目标错误
- **WHEN** 第二批提升点的 legacy mapping 不是同 code、`MAPPED` 且目标类型不是 `IMPROVEMENT_POINT`
- **THEN** 学科质量门禁 SHALL 失败
