## Context

项目当前同时支持本机 H2 和学校部署 PostgreSQL，所有环境默认使用 Hibernate `ddl-auto: update`。这种方式能降低早期开发成本，但正式库已经成为内容与学习事实主库后，隐式改表无法提供版本历史、确定的执行顺序、结构漂移门禁和可审计发布证据。现有 `backup-postgres.sh` 输出 plain SQL，未验证归档结构、校验和或恢复可读性；`restore-postgres.sh` 也缺少破坏性操作确认。

当前生产 PostgreSQL 已包含正式内容和历史学习数据，不能通过删除重建方式接入 Flyway。与此同时，本机大量测试仍使用 H2 `create-drop`，第一期不能把全部业务测试一次性改造成容器测试。设计必须同时保护生产数据、建立 PostgreSQL 权威迁移链，并保留现有快速测试能力。

## Goals / Non-Goals

**Goals:**

- 让学校/生产 PostgreSQL 的每次 Schema 变化都有不可变版本、校验值和执行历史。
- 让全新空 PostgreSQL 可以只靠 Flyway V1 建成当前应用所需的完整 Schema。
- 让现有非空 PostgreSQL 在备份、结构预检和显式确认后登记 V1 基线，不重放建表 SQL。
- 将学校配置的 Hibernate 切换为 `validate`，使缺表、缺列或类型不匹配直接阻止应用启动。
- 为备份增加 custom-format 可读性校验、SHA-256、元数据和显式恢复确认。
- 用真实 PostgreSQL 容器验证空库初始化、现有库基线、幂等重跑和漂移阻断。

**Non-Goals:**

- 本轮不重命名、删除或拆分业务表和字段。
- 本轮不清洗题库、知识树、标准库或学生学习数据。
- 本轮不移除 H2；H2 继续服务快速开发和单元/集成测试，但不作为生产 Schema 权威来源。
- 本轮不提供自动降级或自动反向 SQL；数据库破坏性回滚仍以已验证备份恢复或向前修复为准。

## Decisions

### 1. PostgreSQL Schema 以 Flyway SQL 为唯一发布源

新增 Spring Boot 管理的 Flyway 依赖，迁移文件位于 `src/main/resources/db/migration`。V1 保存当前完整 PostgreSQL Schema；后续改动只能新增 `V<N>__<description>.sql`，已执行迁移不得修改、重排或删除。

选择 Flyway SQL 而不是继续使用 Hibernate `update`，因为 SQL 能审查真实 DDL、在不同环境保持确定顺序，并由 `flyway_schema_history` 保存版本和校验值。未选择 Liquibase，是因为当前项目没有跨数据库抽象需求，纯 SQL 与现有 PostgreSQL 运维方式更直接。

### 2. 空库执行 V1，现有非空库显式登记 V1 基线

默认 `baseline-on-migrate=false`。全新空库直接执行 V1；生产现有库必须通过独立基线脚本完成以下门禁：确认目标数据库、检查关键表和关键列、确认尚无 Flyway 历史、创建并验证备份、记录关键数据计数，然后仅在一次性进程中设置 `FLYWAY_BASELINE_ON_MIGRATE=true` 登记 V1。正式应用启动继续保持该开关为 `false`。

不把基线开关永久设为 `true`，因为这会让任意未知非空 Schema 被静默接受。也不在生产库重放 V1，因为 `CREATE TABLE` 会与现有对象冲突，并可能诱发破坏性清理。

### 3. 学校环境严格校验，H2 保留为非权威快速环境

`school` profile 启用 Flyway并使用 `ddl-auto=validate`；默认 `dev` profile 暂时保持 H2 和 `ddl-auto=update`，避免一次变更重写全部测试。新增真实 PostgreSQL 验证脚本作为迁移链门禁，确保 V1 和后续版本实际可在目标数据库执行。

H2 不再被描述为生产 Schema 的对等来源。未来若继续收敛开发环境，可另行将本地开发默认切换为 PostgreSQL；本轮先完成风险最高的生产主链路。

### 4. 备份使用 PostgreSQL custom format 并生成伴随证据

备份扩展名改为 `.dump`，使用 `pg_dump --format=custom`。脚本在成功后必须执行 `pg_restore --list`，生成 SHA-256 文件和包含时间、数据库、用户、PostgreSQL 版本、镜像/容器信息的元数据文件。空文件、不可列举归档或校验失败均视为备份失败。

恢复脚本要求 `--confirm-restore`，先验证归档与校验和，再通过 `pg_restore --clean --if-exists --exit-on-error` 恢复。继续识别历史 `.sql` 备份，但同样要求显式确认，并明确其校验能力较弱。

### 5. 数据库变更使用前后快照和应用 readiness 双重验收

基线或后续迁移前记录关键正式表计数；迁移后检查 Flyway 状态、关键表/列/索引、计数未异常下降、`database-content` readiness 和应用健康状态。Schema 迁移成功不等于业务可用，二者必须分别给出证据。

## Risks / Trade-offs

- [V1 与现有生产 Schema 存在历史漂移] → 基线前运行结构预检，基线后由 Hibernate `validate` 阻止应用启动；首次生产切换必须保留旧镜像和已验证备份。
- [Hibernate 生成的 DDL 含实现细节或命名不稳定] → 从当前实体在真实 PostgreSQL 上生成初始结构，再人工规范化并用空库启动测试校验；V1 固化后不再重新生成。
- [H2 与 PostgreSQL 仍可能漂移] → 明确 PostgreSQL 为权威，所有迁移必须通过容器级 PostgreSQL 验证；H2 只承担快速反馈。
- [custom-format 备份不再能直接用文本编辑器查看] → 提供 `pg_restore --list` 校验与恢复脚本，同时保留历史 `.sql` 恢复兼容。
- [数据库 DDL 回滚通常不可安全自动化] → 迁移遵守 expand/contract 和向前兼容原则；破坏性变更拆成多版本，失败时优先回退应用或恢复已验证备份。

## Migration Plan

1. 在隔离 PostgreSQL 中根据当前实体生成完整 Schema，固化为 V1，并验证空库启动。
2. 接入 Flyway；学校环境切换为 `validate`，测试现有库缺少历史时默认拒绝启动。
3. 加强备份、校验、恢复、基线和 Schema readiness 脚本。
4. 在临时 PostgreSQL 中分别验证空库执行 V1、模拟旧 Hibernate 库登记基线、重复执行幂等、故意删除列后启动失败。
5. 生产发布前检查服务器资源并保留旧镜像；执行基线脚本生成备份和计数证据；确认 `flyway_schema_history` 为 V1 后只替换应用容器。
6. 若基线或校验失败，不启动新应用；保留原数据库和旧容器。若基线成功但新应用失败，先回退旧镜像，禁止删除数据卷；只有确认数据损坏时才从已验证备份恢复。

## Open Questions

无。下一阶段再决定是否完全移除 H2，以及是否引入自动化的定期恢复演练环境。
