## Why

正式内容已经迁入 PostgreSQL，但数据库结构仍由 Hibernate `ddl-auto: update` 隐式修改，无法回答生产库执行过哪些结构变化，也无法可靠阻止本地、测试和生产 Schema 漂移。现在需要先建立可追踪、可验证、可恢复的数据库工程底座，再继续进行内容质量治理和结构演进。

## What Changes

- 引入 Flyway，把数据库结构升级收束为不可变、按版本排序的显式迁移链。
- 为全新数据库提供完整基线 Schema，并让已有非空数据库在确认当前结构后安全登记基线，避免重建或覆盖生产数据。
- **BREAKING**：学校部署从 Hibernate 自动改表切换为 `validate`；未执行迁移或结构不匹配时，应用必须拒绝启动。
- 建立迁移前备份、备份校验、迁移状态检查、关键数据计数和恢复演练脚本，禁止把“命令退出 0”当作备份可恢复证据。
- 增加空库建库、已有库登记基线、重复启动幂等、Schema 漂移和部署脚本安全测试。
- 记录数据库工程化运维文档和后续迁移编写规则；本轮不重构业务表、不清洗正式内容。

## Capabilities

### New Capabilities

- `database-schema-migration`: 定义 Flyway 版本链、空库初始化、既有数据库基线登记、Hibernate 结构校验和漂移阻断能力。
- `database-backup-recovery`: 定义迁移前备份、校验、元数据、恢复演练和关键数据前后对比能力。

### Modified Capabilities

无。

## Impact

- 依赖：新增 Spring Boot Flyway 集成与 PostgreSQL Flyway 数据库支持。
- 配置：调整 `application.yml` 中 Flyway、JPA 和 `school`/`dev` 数据库策略。
- 数据库：新增 `flyway_schema_history`；已有生产库只登记确认过的基线，不删除、不重建业务表。
- 脚本：强化 PostgreSQL 备份/恢复，并新增迁移预检、状态验证和结构漂移检查入口。
- 测试与部署：新增 PostgreSQL 容器级迁移验证；生产发布仍遵守“外部构建、服务器只启动”，且应用容器替换前必须完成备份和迁移门禁。
