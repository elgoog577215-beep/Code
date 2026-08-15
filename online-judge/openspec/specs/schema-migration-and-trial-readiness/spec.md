# schema-migration-and-trial-readiness Specification

## Purpose
用可重复、可验证的数据库迁移、备份恢复演练和运行检查保护现有正式数据，并为多教师试用建立明确、可审计且能阻断错误发布的上线门禁。
## Requirements
### Requirement: 正式数据库结构必须由版本迁移管理
系统 SHALL 使用版本化迁移管理正式结构，现有非空数据库 SHALL 可通过受控 baseline 接管，生产 ORM SHALL 只验证结构而不得自动修改。

#### Scenario: 空数据库启动
- **WHEN** 应用连接空 Postgres 或 H2 开发库
- **THEN** 所有迁移 SHALL 按版本成功执行
- **AND** ORM 结构验证 SHALL 通过

#### Scenario: 接管现有数据库副本
- **WHEN** 已备份的现有数据库副本启用 baseline
- **THEN** 迁移 SHALL 保留现有题目、班级、学生、作业和提交
- **AND** 历史教学数据 SHALL 归属于 bootstrap 管理员

### Requirement: 迁移必须以前置备份和演练为门禁
正式迁移前 SHALL 强制生成可恢复备份，并 SHALL 在空库及正式库副本分别演练；回滚 SHALL 依靠迁移前备份恢复。

#### Scenario: 未提供迁移前备份
- **WHEN** 部署请求执行破坏性或结构迁移且没有有效备份证明
- **THEN** 部署流程 SHALL 失败并阻止继续

### Requirement: Readiness 必须覆盖试用关键依赖
Readiness SHALL 报告迁移版本、有效管理员、孤立数据、名单异常、公共题数量、AI Provider 和额度系统状态。

#### Scenario: 存在孤立教学数据
- **WHEN** 任一班级或作业没有有效所有者
- **THEN** Readiness SHALL 标记不可推广状态并给出不含敏感数据的原因
