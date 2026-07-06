## Why

当前数据库已经有较完整的知识树和较大规模的 AI 标准库，但自动铺底条目占比较高，手写高质量细颗粒条目不足，且提升点尚未进入规范化主结构。继续扩库应先细化知识树，再补人工设计的易错点与提升点，让 AI 诊断能落到更具体、更可教学的路径上。

## What Changes

- 扩建信息学知识树，在现有大章节和 topic 下补充更细的小知识点，重点覆盖模拟、字符串、数组更新、图存储、双指针、复杂度、运行错误和读题提交检查等薄弱区域。
- 新增 V8 手写标准库 seed 模块，人工补充高质量能力点、易错点和提升点，避免继续依赖泛化自动条目堆量。
- 将提升点作为规范标准库的一等内容同步到 `ai_standard_improvement_points`，使“修错后怎么提升”能进入结构化候选包。
- 增加专项测试，校验新增知识节点合法、V8 条目具体、易错点挂能力点、提升点挂能力点或相关易错点，并防止空泛命名回退。

## Capabilities

### New Capabilities

- `informatics-knowledge-tree-quality`: 信息学知识树扩展和细化的质量要求。

### Modified Capabilities

- `standard-library-normalized-schema`: 规范标准库需要同步人工提升点，并保证手写细颗粒易错点与提升点进入结构化候选主路径。

## Impact

- 影响代码：`InformaticsKnowledgeSeedCatalog`、`AiStandardLibrarySeedCatalog`、新增 V8 seed 模块、标准库同步测试和知识树测试。
- 影响数据库 seed：新增知识节点、能力点、易错点、提升点和规范提升点同步结果。
- 不改变数据库表结构、外部模型调用 API、学生端展示协议或教师端页面结构。
