## ADDED Requirements

### Requirement: 正式内容质量修正必须通过版本化迁移发布
对正式知识树和标准库执行批量质量修正时，系统 SHALL 使用未发布的 Flyway 版本化 SQL，并 SHALL 在生产备份与隔离恢复演练成功后发布。

#### Scenario: 在隔离 PostgreSQL 验证质量迁移
- **WHEN** 变更包含学科范围表或正式内容批量更新
- **THEN** 完整 Flyway 链 SHALL 在空 PostgreSQL 和现有 V1 结构上执行成功
- **AND** 重复启动 SHALL 不重复修改已经迁移的数据

#### Scenario: 在生产执行质量迁移
- **WHEN** 生产数据库准备执行学科质量迁移
- **THEN** 运维流程 SHALL 先保存迁移前计数与质量指标
- **AND** SHALL 生成可校验备份并完成隔离恢复演练
- **AND** 迁移后 SHALL 运行 Schema、内容、学科质量和导航读取验证
