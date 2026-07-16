## Why

生产 V3 仍有 532 个模板化知识点和 53 个缺少提升点的启用能力点；其中 ALGO 与 DS 分别承载 457 和 194 次规范结构引用，最近 55 条诊断事实又重复落在 Dijkstra、区间 DP、区间合并等算法路径。继续平均扩量会保留“有节点但不可教学”的问题，因此第三批需要把算法与数据结构的高影响分支精修成可解释、可练习、可验证的学科闭环。

## What Changes

- 固化生产 V3 的节点、标准库、诊断事实、业务表计数和同口径质量债务基线。
- 以规范结构引用为主证据、诊断路径出现次数为有限加权证据，人工选择 ALGO 20 个、DS 10 个模板知识点进行精修，不新增知识树节点、不改变稳定 code。
- 为 12 个存在启用易错点、尚无提升点且不是 `SK_COMPAT_*` 的正式能力点补齐可执行提升路径，并同步规范表、平铺快照和 legacy mapping。
- 扩展数据库质量门禁，约束 Flyway V4 数量、累计精修数量、模板债务上限、正式能力缺口上限、三表一致性和兼容占位排除规则。
- 在生产备份的隔离恢复库完成 V4 重复执行、应用启动、代表性导航与业务计数验证，再发布生产并生成可复核报告。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `discipline-data-quality-audit`：增加第三批生产基线、诊断事实有限加权、兼容能力排除和 V4 债务下降门禁。
- `informatics-knowledge-tree-quality`：增加 ALGO/DS 分支级精修与 `informatics-knowledge-discipline-v3` 内容质量要求。
- `standard-library-normalized-schema`：增加第三批正式提升点的三表一致性、可执行练习和代表性导航要求。

## Impact

- 数据库：新增 Flyway V4 内容迁移，更新 `informatics_knowledge_nodes`，新增 `ai_standard_improvement_points` 并同步兼容结构。
- 验证：更新学科质量门禁、PostgreSQL 迁移链、安全测试和代表性导航测试。
- 运维：生产发布前新增备份、SHA-256 校验、独立恢复演练和回滚镜像；不修改题目、提交、诊断事实或课堂业务数据。
- 文档：新增第三批选择证据、HTML 质量报告、项目决策记录和 OpenSpec 归档。
