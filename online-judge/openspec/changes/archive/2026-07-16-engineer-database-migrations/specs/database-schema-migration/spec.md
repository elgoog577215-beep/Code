## ADDED Requirements

### Requirement: PostgreSQL Schema 必须通过版本化迁移演进
学校和生产 PostgreSQL 的 Schema SHALL 以 Flyway 版本化 SQL 为唯一发布来源；每个已发布迁移 MUST 保持不可变，并由数据库记录版本、描述、校验值、执行时间和结果。

#### Scenario: 应用连接已迁移数据库
- **WHEN** 学校环境应用启动且数据库已经执行全部当前迁移
- **THEN** Flyway SHALL 验证迁移历史和校验值
- **AND** Hibernate SHALL 只验证实体映射，不得自动新增、修改或删除 Schema 对象

#### Scenario: 数据库落后于应用版本
- **WHEN** 学校环境应用连接的数据库缺少当前版本迁移
- **THEN** Flyway SHALL 按版本顺序执行未应用迁移
- **AND** 应用 SHALL 只在迁移和 Hibernate 结构校验全部成功后完成启动

#### Scenario: 已执行迁移被修改
- **WHEN** 仓库中的已执行迁移校验值与数据库历史不一致
- **THEN** 应用 SHALL 拒绝启动
- **AND** 系统 SHALL NOT 自动修复或忽略该差异

### Requirement: 全新 PostgreSQL 必须可由 V1 独立初始化
系统 SHALL 提供覆盖当前全部生产实体、关键约束和索引的 V1 基线迁移，使空 PostgreSQL 无需 Hibernate 自动建表即可初始化。

#### Scenario: 初始化空数据库
- **WHEN** Flyway 对空 PostgreSQL Schema 执行迁移
- **THEN** 系统 SHALL 创建当前应用所需的全部表、序列或 identity、约束和索引
- **AND** Hibernate `validate` SHALL 通过

#### Scenario: 重复启动空库初始化后的应用
- **WHEN** 同一应用再次连接已经完成 V1 的数据库
- **THEN** Flyway SHALL 不重复执行 V1
- **AND** 业务表和数据计数 SHALL 保持不变

### Requirement: 现有非空数据库基线必须显式且受门禁保护
系统 SHALL 默认禁止自动接受没有 Flyway 历史的非空 Schema；现有生产库只有在目标确认、结构预检、备份校验和明确基线授权全部满足时，才可登记为 V1。

#### Scenario: 非空旧数据库普通启动
- **WHEN** 应用连接包含业务表但没有 `flyway_schema_history` 的数据库，且未给出一次性基线授权
- **THEN** 应用 SHALL 拒绝启动
- **AND** 系统 SHALL NOT 自动登记基线或重放 V1

#### Scenario: 执行受控基线
- **WHEN** 运维人员通过基线脚本明确确认目标数据库和基线动作
- **THEN** 脚本 SHALL 验证关键表、关键列、备份和迁移前计数
- **AND** Flyway SHALL 只登记 V1 基线，不重建、删除或覆盖现有业务表

### Requirement: Schema 漂移必须阻止正式应用启动
学校和生产环境 MUST 将缺表、缺列、类型不兼容或迁移历史异常视为发布失败，不得依靠运行时自动改表掩盖漂移。

#### Scenario: 关键列缺失
- **WHEN** 已登记基线的数据库缺少当前实体所需关键列
- **THEN** Hibernate 结构校验 SHALL 失败
- **AND** 应用 SHALL 不进入可接收业务请求状态

#### Scenario: 迁移后结构检查
- **WHEN** 数据库完成基线或新版本迁移
- **THEN** 运维检查 SHALL 输出 Flyway 当前版本、失败记录、关键表、关键列和关键索引状态
- **AND** 任一必要检查失败 SHALL 阻止应用容器替换完成

### Requirement: PostgreSQL 迁移链必须经过真实数据库验证
每次新增或修改未发布迁移时，项目 SHALL 在隔离 PostgreSQL 中验证空库迁移、重复迁移和应用结构校验，不得只依赖 H2 测试推断生产兼容性。

#### Scenario: 提交数据库迁移
- **WHEN** 代码变更包含新的 Flyway SQL 或数据库映射变化
- **THEN** 验证流程 SHALL 启动隔离 PostgreSQL 并执行完整迁移链
- **AND** SHALL 验证第二次执行没有额外 Schema 或数据变化

#### Scenario: H2 测试通过但 PostgreSQL 迁移失败
- **WHEN** 快速 H2 测试成功但隔离 PostgreSQL 迁移或结构校验失败
- **THEN** 该变更 SHALL 被视为未通过
- **AND** 不得进入生产发布流程
