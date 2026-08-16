# database-backup-recovery Specification

## Purpose
TBD - created by archiving change engineer-database-migrations. Update Purpose after archive.
## Requirements
### Requirement: 数据库迁移前必须创建可验证备份
对现有正式 PostgreSQL 执行基线或版本迁移前，系统 SHALL 创建 PostgreSQL custom-format 备份，并 MUST 在数据库结构发生变化前验证归档可读取。

#### Scenario: 备份成功
- **WHEN** 运维人员执行正式数据库备份
- **THEN** 脚本 SHALL 生成非空 `.dump` 归档
- **AND** SHALL 通过 `pg_restore --list` 验证归档目录可读取
- **AND** SHALL 生成 SHA-256 和备份元数据文件

#### Scenario: 备份不可验证
- **WHEN** 归档为空、`pg_restore --list` 失败或 SHA-256 无法生成
- **THEN** 备份 SHALL 被视为失败
- **AND** 后续基线或迁移 SHALL NOT 执行

### Requirement: 备份证据必须标识目标与来源
每个正式备份 SHALL 记录创建时间、数据库名、数据库用户、PostgreSQL 版本和来源容器，使恢复操作能够确认备份属于哪个目标。

#### Scenario: 检查备份元数据
- **WHEN** 运维人员准备执行迁移或恢复
- **THEN** 系统 SHALL 提供与归档同名的元数据和校验和文件
- **AND** 元数据 SHALL 足以区分目标数据库与备份生成环境

### Requirement: 恢复必须显式确认并先验证归档
恢复操作 MUST 要求破坏性动作确认，并 SHALL 在连接目标数据库前验证输入文件、归档目录和已有校验和。

#### Scenario: 未确认恢复
- **WHEN** 运维人员未提供明确的恢复确认参数
- **THEN** 恢复脚本 SHALL 拒绝执行
- **AND** 目标数据库 SHALL 保持不变

#### Scenario: 恢复 custom-format 备份
- **WHEN** 运维人员明确确认恢复一个校验通过的 `.dump` 归档
- **THEN** 脚本 SHALL 使用 `pg_restore` 的错误即停模式恢复
- **AND** 任一 SQL 或对象恢复错误 SHALL 使命令返回失败

#### Scenario: 恢复历史 SQL 备份
- **WHEN** 运维人员明确确认恢复历史 `.sql` 备份
- **THEN** 脚本 MAY 使用 `psql` 兼容恢复
- **AND** SHALL 启用遇错即停并提示该格式缺少 custom-format 目录校验能力

### Requirement: 迁移必须保留前后数据与可用性证据
数据库基线或版本迁移 SHALL 记录关键正式表的迁移前后计数，并 SHALL 在完成后验证数据库内容 readiness 和应用健康状态。

#### Scenario: 迁移关键表计数稳定
- **WHEN** 迁移不包含明确的数据删除任务
- **THEN** 题目、测试用例、知识节点、能力点、易错点和提升点计数 SHALL NOT 异常下降
- **AND** 迁移记录 SHALL 保留前后对比结果

#### Scenario: Schema 成功但业务读取失败
- **WHEN** Flyway 显示成功但数据库内容检查或应用 readiness 失败
- **THEN** 发布 SHALL 被视为未完成
- **AND** 运维人员 SHALL 保留旧应用镜像、数据库 Volume 和备份用于诊断或恢复

### Requirement: 备份恢复能力必须能够被隔离演练
项目 SHALL 提供不覆盖正式数据库的恢复验证路径，使备份能够在临时数据库中被真实恢复和查询，而不是只检查文件存在。

#### Scenario: 隔离恢复演练
- **WHEN** 运维人员对备份执行恢复演练
- **THEN** 系统 SHALL 将归档恢复到独立临时数据库或容器
- **AND** SHALL 验证 Flyway 历史、关键表存在和关键数据计数可查询
- **AND** SHALL NOT 连接或修改正式数据库 Volume
