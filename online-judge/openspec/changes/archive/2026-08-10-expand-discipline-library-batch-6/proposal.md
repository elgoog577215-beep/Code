## Why

生产学科库当前有 586 个启用叶子知识点，但仍有 454 个沿用占位描述，其中 401 个没有精确主能力点；标准库虽然已有 140 个能力点、381 个易错点、138 个提升点和 34 个应用场景，诊断密度仍集中在少数高频分支。继续按单点补丁扩充会让覆盖越来越偏，本批需要跨六个领域成组补齐可教学、可诊断、可迁移的完整主题包，同时用质量门禁阻止模板化凑数。

## What Changes

- 在不新增平行知识树、不修改稳定知识 code 的前提下，精修 ALGO、BASIC、CONTEST、DS、ENG、MATH 六个领域各 8 个模板叶子，共 48 个知识点。
- 为每个精修知识点新增 1 个正式能力点、3 个可观察易错点、1 个可执行提升点和 1 组课堂—竞赛应用场景，共新增 48 个能力点、144 个易错点、48 个提升点和 96 个场景。
- 通过 Flyway V11 将正式内容写入规范化主表，并同步平铺兼容快照、legacy mapping 和场景引用；不恢复运行时 seed。
- 增加批次专属质量检查，校验六领域分布、主题包闭环、反模板表达、知识/能力/错因/提升/场景归属、兼容结构一致性和有效文本体积增量。
- 在隔离 PostgreSQL 中验证 V1→V11、生产备份恢复迁移、原始 SQL 幂等、代表性导航和业务事实计数不变。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `informatics-knowledge-tree-quality`: 增加第六批六领域均衡精修、稳定节点复用和可教学文本质量要求。
- `standard-library-normalized-schema`: 增加 48 个完整主题包在规范表、兼容层和应用场景层的一致性要求。
- `discipline-data-quality-audit`: 增加批次 6 的数量、闭环、反凑数、文本体积、幂等和业务稳定门禁。

## Impact

- 数据库：新增 `src/main/resources/db/migration/V11__expand_discipline_library_batch_6.sql`，只更新既有知识点内容并新增规范标准库与应用场景内容。
- 验证：扩展 `scripts/check-discipline-data-quality.sh`、`scripts/test-postgres-migrations.sh`、迁移安全测试和标准库导航回归。
- 规格：更新三项现有 OpenSpec 规格并在完成后归档本变更。
- 边界：不修改题目、测试用例、提交、诊断事实、学生反馈和教师分析等业务事实；不以当前小样本宣称教学效果。
