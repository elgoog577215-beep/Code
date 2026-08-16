# Seed 迁移清单

## 运行时写入入口

| 入口 | 当前行为 | 迁移目标 |
| --- | --- | --- |
| `shared/bootstrap/DataInitializer` | 首次启动写入 5 个示例题，并确保演示作业 | 题目迁入正式 `problems/test_cases`；演示班级改成测试/演示 fixture，不进入正式内容主链路 |
| `problem/application/PublicProblemSeeder` | 启动时从 `PublicProblemSeedCatalog` 写公共题库，并回填 starter code | 公共题库迁入 `problems/test_cases`；starter code 维护走数据库字段或管理 API |
| `learning/knowledge/application/InformaticsKnowledgeSeeder` | 启动时从 `InformaticsKnowledgeSeedCatalog` 写知识树 | 知识树迁入 `informatics_knowledge_nodes` |
| `learning/standardlibrary/application/AiStandardLibrarySeeder` | 启动时从 `AiStandardLibrarySeedCatalog` 写旧平铺标准库并禁用部分旧兜底 | 标准库内容迁入规范化表；旧平铺表只做兼容快照 |
| `learning/standardlibrary/application/AiStandardLibraryNormalizedSeeder` | 启动时从标准库 seed 同步规范化能力/易错/提升表 | 规范化表由迁移脚本、管理 API、教师治理台直接写入 |
| `classroom/application/DefaultClassroomSeeder` | 开发自动登录开启时创建默认班级 | 保留为开发/测试 fixture 候选，不作为正式题库/标准库内容迁移对象 |

## 内容型 seed 文件

| 类型 | 文件 | 处理方式 |
| --- | --- | --- |
| 公共题库 | `PublicProblemSeed.java`、`PublicProblemSeedCatalog.java`、`src/main/resources/public-problem-seeds/tidal-discount-path-wrong.py` | 迁入 `problems` 与 `test_cases` 后从 `src/main` 移除；必要样例移到测试 fixture |
| 知识树 | `InformaticsKnowledgeSeed.java`、`InformaticsKnowledgeSeedCatalog.java` | 迁入 `informatics_knowledge_nodes` 后从运行时代码移除 |
| 标准库基础结构 | `AiStandardLibrarySeed.java`、`AiStandardLibrarySeedCatalog.java` | 迁入规范化表和兼容快照后移除；旧兜底识别改为数据库/模式规则 |
| 标准库扩张批次 | `AiStandardLibraryV6ExpansionSeeds.java` 到 `AiStandardLibraryV16DensityExpansionSeeds.java` | 已有内容迁入正式库；后续禁止新增 Vx seed 批次 |
| 静态冷备份 | `backups/standard-library/generated-fallback-archive-2026-07-06/` | 保留为归档材料，不得被运行时代码读取 |

## 运行时旁路依赖

这些代码不一定直接播种，但仍读取或引用 seed catalog，需要在删除 seed 前改为数据库读取或纯规则判断：

- `problem/application/PublicStarterCodeCatalog`：当前从 `PublicProblemSeedCatalog` 回填 starter code。
- `learning/standardlibrary/application/AiStandardLibraryService`：当前引用 `AiStandardLibrarySeedCatalog` 识别旧兜底 code 和默认版本。
- `learning/standardlibrary/application/AiStandardLibraryQualityReportService`：当前读取知识树和标准库 seed 生成质量报告。
- `learning/standardlibrary/application/AiStandardLibraryNormalizedSeeder`：当前静态读取知识树 seed 判断 knowledge point code。

## 依赖 seed 的测试

需要重写为数据库行为测试或测试 fixture：

- `learning/knowledge/InformaticsKnowledgeSeederTest`
- `learning/standardlibrary/AiStandardLibrarySeederTest`
- `learning/standardlibrary/AiStandardLibraryNormalizedSeederTest`
- `learning/standardlibrary/AiStandardLibraryQualityReportServiceTest`
- `learning/standardlibrary/AiStandardLibraryV6ExpansionSeedsTest` 到 `AiStandardLibraryV16DensityExpansionSeedsTest`
- `learning/standardlibrary/StandardLibraryPackBuilderDatabaseTest`
- `problem/PublicProblemSeederTest`

## 正式数据库表映射

### 公共题库

- `problems.title`：题目唯一识别字段，迁移 upsert 键。
- `problems.description`：题面 Markdown。
- `problems.difficulty`、`time_limit`、`memory_limit`：判题配置。
- `problems.ai_prompt_direction`：AI 诊断上下文提示。
- `problems.starter_code`：起始代码。
- `problems.knowledge_points`、`algorithm_strategies`、`common_mistakes`、`boundary_types`：JSON 文本列表。
- `test_cases.problem_id`、`input`、`expected_output`、`is_hidden`、`order_index`：测试点。

### 信息学知识树

- `informatics_knowledge_nodes.code`：唯一迁移键。
- `parent_code`、`type`、`name`、`description`、`path`：树结构和展示。
- `stage`、`difficulty`、`aliases`、`prerequisites`、`learning_objectives`、`typical_problems`：检索和教学信息。
- `sort_order`、`enabled`、`library_version`：排序、启用和版本记录。

### AI 标准库

- `ai_standard_skill_units.code`：能力点唯一迁移键。
- `ai_standard_mistake_points.code`：易错点唯一迁移键，`skill_unit_code` 归属能力点。
- `ai_standard_improvement_points.code`：提升点唯一迁移键，`skill_unit_code` 归属能力点。
- `ai_standard_library_relations`：能力/易错/提升与知识节点、前置关系。
- `ai_standard_library_legacy_mappings`：旧 `layer/code` 到规范化目标的兼容映射。
- `ai_standard_library_items`：旧平铺兼容快照，不再作为正式内容主库。

## 迁移前检查

正式迁移前必须完成：

1. 运行 `scripts/backup-postgres.sh` 生成备份。
2. 运行 `scripts/check-database-content-readiness.sh` 确认连接的是 Postgres 目标库，并输出关键表数量。
3. 确认 `ai_standard_library_items` 中旧 `BASIC_CAUSE / KB_* / 掌握偏差` 启用数量为 0，或先执行旧低质条目禁用脚本。
4. 确认迁移脚本只执行 upsert/disable，不删除学生提交、班级、作业和历史提交数据。
