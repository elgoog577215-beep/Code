## Why

标准库已经有 V6/V7/V8 多轮手写扩张，但仍有一批高频竞赛能力点主要依赖自动兜底条目，诊断颗粒度不够细。当前需要继续扩张标准库，把更多“学生真实会犯、AI 能稳定识别、教师能据此讲解”的能力点和易错点沉淀为正式 seed。

## What Changes

- 新增 V9 手写标准库扩展，覆盖滑动窗口、并查集、递归回溯、堆/优先队列、拓扑排序、前缀/差分、二分答案、树遍历、map 频次与输出构造。
- 每个主题新增能力点、多个易错点和提升点，保持“知识树节点 → 能力点 → 易错点/提升点”的结构。
- 将 V9 扩展接入标准库 seed catalog 和规范化 seeder 链路。
- 增加 V9 专项测试，校验条目数量、命名、知识节点引用、能力点归属、提升点关联和代表性教学语义。
- 不新增数据库表，不修改 AI 诊断流程，不改前端。

## Capabilities

### New Capabilities

### Modified Capabilities
- `standard-library-normalized-schema`: 增加 V9 手写扩库要求，要求新增条目覆盖更多高频算法与工程诊断主题，并保持规范化入库结构。

## Impact

- 影响后端标准库 seed：新增 `AiStandardLibraryV9ExpansionSeeds`，并在 `AiStandardLibrarySeedCatalog` 中接入。
- 影响测试：新增 V9 扩展测试，并扩展标准库 seeder / normalized seeder 相关回归覆盖。
- 不影响数据库 schema、API、前端页面和外部依赖。
