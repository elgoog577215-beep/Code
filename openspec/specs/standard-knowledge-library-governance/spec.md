# standard-knowledge-library-governance Specification

## Purpose
TBD - created by archiving change govern-standard-knowledge-library-quality. Update Purpose after archive.
## Requirements
### Requirement: 存量治理必须保护稳定标识和历史事实
系统 SHALL 通过新增 Flyway 迁移治理知识库与标准库内容；治理 MUST 保留知识节点、能力点、易错点、提升点和场景的稳定 code，MUST NOT 改写提交诊断事实、推荐事件或其他业务证据以迎合库内容。

#### Scenario: 同名知识节点需要消歧
- **WHEN** 两个稳定知识节点使用相同展示名但表达不同路径语义
- **THEN** 迁移 SHALL 通过名称、描述、别名和 path 消歧
- **AND** 两个节点的 code、父节点和既有引用 MUST 保持不变

#### Scenario: 历史错因已有业务引用
- **WHEN** 一个需要优化的易错点 code 可能出现在历史诊断事实中
- **THEN** 系统 SHALL 保留该 code 并优化其内容或兼容语义
- **AND** 迁移 MUST NOT 批量改写历史事实为另一个 code

### Requirement: 正式易错点必须提供可执行修正策略
每个启用正式易错点的修正策略 SHALL 说明如何建立修复合同、如何定位最小复现或首个偏差、如何修改以及如何验证；修正策略 MUST NOT 只是要求 AI 结合上下文另行生成建议的元说明。

#### Scenario: 旧易错点仍使用 AI 元说明
- **WHEN** 迁移发现修正策略只说明“由 AI 结合题目、代码与判题结果生成”
- **THEN** 迁移 SHALL 按错误类型和该条目的实际症状补成可执行协议
- **AND** 发布门禁 MUST 要求此类元说明数量为 0

#### Scenario: 平铺兼容快照存在
- **WHEN** 规范易错点已有同 code 的平铺快照
- **THEN** 迁移 SHALL 从规范条目同步描述、误区、分级提示和证据信号
- **AND** 平铺快照 MUST NOT 反向覆盖规范表

### Requirement: 同名概念必须能从路径语义区分
启用知识节点和正式易错点 SHALL 避免出现会让 AI 或教师无法区分的同名概念；跨路径保留相同原术语时，名称 MUST 指出算法方法、输入建模、数据结构、工程检查或竞赛识别等观察面，aliases SHALL 保留原术语用于召回。

#### Scenario: 一个术语同时出现在算法与工程路径
- **WHEN** “数据范围反推”等术语同时属于算法枚举和工程复杂度路径
- **THEN** 两个节点 SHALL 使用不同且具体的展示名和描述
- **AND** 原术语 SHALL 保留在别名中而不是复制成两个不可区分的主名

#### Scenario: 两个错因描述相近
- **WHEN** 两个稳定错因分别表达题意合同错误和实现推进错误
- **THEN** 名称、误区和修正策略 SHALL 明确各自层次
- **AND** 质量门禁 MUST 阻断同一锚点下规范化名称重复

### Requirement: 存量治理必须具备可重复质量门禁
系统 SHALL 在迁移内断言和发布审计中持续检查元说明修正策略、治理目标同名、规范/快照一致性、场景差异和引用闭合；空内容数据库 SHALL 安全执行，存在正式内容的数据库 MUST 满足预期基线后才能迁移。

#### Scenario: 生产内容规模与预期不一致
- **WHEN** 数据库已有正式标准内容但本轮目标记录缺失或数量异常
- **THEN** 迁移 MUST 失败并整体回滚
- **AND** 系统 MUST NOT 在不完整目标集上静默完成治理

#### Scenario: 空数据库执行完整迁移链
- **WHEN** V1 到最新版本在没有正式种子内容的空 PostgreSQL 上执行
- **THEN** V12 SHALL 以 0 条内容更新安全通过
- **AND** Hibernate schema 校验与重复启动 SHALL 保持成功
