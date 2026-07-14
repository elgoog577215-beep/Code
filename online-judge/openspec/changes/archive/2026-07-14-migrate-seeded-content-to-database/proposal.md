## Why

正式题库、知识树和 AI 标准库内容长期散落在 Java `SeedCatalog`、`VxExpansionSeeds` 和资源 seed 文件里，导致代码仓库承担内容主库职责，服务器数据库反而像运行时副本。用户已经明确要求禁止继续用 seed 交付正式内容，因此需要把细颗粒内容迁回正式数据库，并从项目主链路移除 seed 机制。

## What Changes

- **BREAKING**：正式内容新增、修订和批量导入不再通过 Java seed、资源 seed 或启动播种完成。
- 将现有 seed 中仍有价值的题库、知识树、能力点、易错点和提升点内容导入正式数据库，导入前必须备份，导入后必须通过数据库查询和 AI 导航读取验证。
- 移除或停用运行时 `CommandLineRunner` seed 入口，应用启动不得再把代码里的内容写入正式数据库。
- 移除内容型 seed 文件和扩库 seed 类；历史内容如需保留，只能作为归档/迁移输入，不参与运行时。
- 提供数据库优先的内容治理路径：管理 API、教师治理台、可审计 SQL/脚本、备份与回滚说明。
- 更新测试：不再断言 seed 数量和 seed 文案质量，而是验证数据库内容读取、正式表写入、导航可见性和旧 seed 入口不可运行。

## Capabilities

### New Capabilities

- `database-first-content-source`: 题库、知识树和标准库正式内容以数据库为主库，GitHub 只保存结构、治理、迁移和审计工具。

### Modified Capabilities

- `standard-library-normalized-schema`: 标准库正式写入和读取必须以规范化数据库表为准，历史 seed 同步要求改为一次性迁移要求。
- `standard-library-review-workflow`: 教师治理台成为标准库内容进入正式库的主流程之一，不再依赖 seed 批次扩库。
- `informatics-knowledge-tree-quality`: 知识树正式内容以数据库维护和审计为准，不再通过运行时 seed 扩建。

## Impact

- 后端启动链路：`PublicProblemSeeder`、`InformaticsKnowledgeSeeder`、`AiStandardLibrarySeeder`、`AiStandardLibraryNormalizedSeeder`、`DefaultClassroomSeeder` 等需要拆分为运行时禁用、迁移工具或测试夹具。
- 内容代码：`PublicProblemSeedCatalog`、`InformaticsKnowledgeSeedCatalog`、`AiStandardLibrarySeedCatalog`、`AiStandardLibraryV*Seeds`、`public-problem-seeds/*` 等内容型文件需要迁出运行时或移除。
- 数据库：正式 Postgres 是内容主库；迁移必须备份并写入 `problems/tasks`、`informatics_knowledge_nodes`、`ai_standard_skill_units`、`ai_standard_mistake_points`、`ai_standard_improvement_points` 和兼容快照。
- 测试：从 seed 静态断言迁移到数据库 fixture、repository/API 查询和导航读取验证。
- 运维：部署前需要一次性迁移和验证；部署后应用重启不得重复插入内容。
