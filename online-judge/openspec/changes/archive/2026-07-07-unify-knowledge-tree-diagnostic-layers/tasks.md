## 1. 结构约束

- [x] 1.1 增加标准库 seed/规范结构测试，校验能力点主知识节点必须是 `KNOWLEDGE_POINT`。
- [x] 1.2 修正不符合“能力点挂知识点”语义的代表性 seed 或映射。

## 2. AI 候选包

- [x] 2.1 调整 `SearchLocationPackSelector`，使用知识树仓库把 knowledgeGroups 的 name/path 补成中文知识点名称和路径。
- [x] 2.2 确保 knowledgeGroups 按知识点分组，并把易错点、提升点挂到所属能力点下。

## 3. Prompt 与链路

- [x] 3.1 修改 `diagnosis-report-v2` prompt，明确“知识点 -> 能力点 -> 易错点/提升点”的统一树语义。
- [x] 3.2 增加 AI 上下文或 prompt 回归测试，防止重新把知识树和标准库表达为平行库。

## 4. 验证与归档

- [x] 4.1 运行相关标准库、知识树、AI prompt 和候选包测试。
- [x] 4.2 运行 `openspec validate --all` 和 `git diff --check`，完成 OpenSpec 归档。
