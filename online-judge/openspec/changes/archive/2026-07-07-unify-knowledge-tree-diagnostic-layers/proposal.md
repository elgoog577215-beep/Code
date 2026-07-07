## Why

当前知识树与 AI 标准库在物理上已经有多张表，但语义层仍容易被理解为两套库：知识树负责目录，标准库另行维护能力点、易错点和提升点。用户明确要求把它们统一成一棵结构：知识树只到知识点，知识点下面挂多个能力点，能力点下面挂易错点，AI 召回和诊断都沿这条主链路工作。

## What Changes

- 将系统语义收敛为 `领域/章节/topic/知识点 -> 能力点 -> 易错点/提升点`。
- 约束能力点主归属必须指向知识树末端 `KNOWLEDGE_POINT`，相关知识节点只能作为检索和上下文补充。
- 调整 AI 标准库候选包，让 `knowledgeGroups` 使用知识点中文名称和路径，并按知识点组织能力点、易错点和提升点。
- 调整 prompt 语义，要求模型沿“知识点 -> 能力点 -> 易错点”理解候选结构，而不是把知识树和标准库当成两套平行库。
- 保留现有表和兼容字段，不做破坏性数据库迁移。

## Capabilities

### New Capabilities

### Modified Capabilities

- `informatics-knowledge-tree-quality`: 明确知识树末端语义，知识树只负责到知识点，不把能力点或易错点作为知识树节点。
- `standard-library-normalized-schema`: 明确规范标准库是知识点以下的诊断层，能力点必须挂到知识点，易错点必须挂到能力点。
- `single-agent-ai-diagnosis`: 明确单诊断 Agent 接收的标准库上下文必须按知识点、能力点、易错点组织。
- `ai-prompt-context-quality`: 明确正式 prompt 使用统一树语义，不再表达为知识树和标准库两套平行结构。

## Impact

- 后端：`SearchLocationPackSelector`、规范标准库 seed 校验、知识树/标准库服务与相关 DTO。
- AI 链路：`StandardLibraryPack.knowledgeGroups` 的中文路径和层级结构，`diagnosis-report-v2` prompt 里的结构说明。
- 测试：增加知识点主锚点、候选包中文路径、prompt 结构语义的回归测试。
- 数据库：复用现有 `informatics_knowledge_nodes`、`ai_standard_skill_units`、`ai_standard_mistake_points` 和 `ai_standard_improvement_points`，不新增表，不拆库。
