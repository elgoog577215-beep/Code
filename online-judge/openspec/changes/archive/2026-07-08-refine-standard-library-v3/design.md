## Context

当前系统已有两层数据：

- `informatics_knowledge_nodes`：信息学知识树，能表达章节、主题、知识点、路径、阶段、难度、前置知识和典型题型。
- `ai_standard_library_items`：AI 标准库条目，当前以 `BASIC_CAUSE` 和 `IMPROVEMENT_POINT` 区分基础层错因与提高层条目，同时包含提示、证据信号、判题信号、教学动作等字段。

用户已经明确新的产品定位：标准库不应替 AI 做诊断，也不应成为固定提示库。标准库只需要把信息学知识体系、能力点和易错点拆得足够细，让外部大模型按标准语言返回；题目理解、代码分析、个性化修正建议和提高建议交给 AI。

约束：

- 本项目当前没有 Flyway/Liquibase，继续使用 JPA `ddl-auto=update`。
- 当前 AI 链路仍依赖 `StandardLibraryPack`，不能本阶段整体重写。
- 教师端已经有 AI 标准库管理入口，改造后仍应保持简单可用。

## Goals / Non-Goals

**Goals:**

- 把标准库核心结构收束为知识树、能力点、易错点。
- 让每个最细知识点至少能挂载能力点和易错点。
- 对高频核心知识点提供更高质量、更细颗粒的人工样板内容。
- 弱化教师端和 DTO 中的固定提示、证据信号、固定提高建议等早期字段。
- 保持旧 `StandardLibraryPack` 可用，避免现有 AI 诊断测试大面积失效。

**Non-Goals:**

- 本阶段不重写外部模型调用协议。
- 本阶段不实现完整 RAG 召回。
- 本阶段不重做学生端结构化诊断报告 UI。
- 本阶段不自动迁移或删除生产数据库里的旧列。
- 本阶段不做复杂多版本 diff、批量导入导出或教师审批流。

## Decisions

### 1. 复用现有标准库表，先不新增独立表

本轮先把 `AiStandardLibraryItem` 从混合提示库改为 v3 条目承载模型，而不是立刻新增 `skill_units` 和 `mistake_points` 两张表。

原因：

- 当前教师端、权限、CRUD、知识点关联和 AI pack 兼容都已围绕 `ai_standard_library_items` 建好。
- 直接新增两张表会牵动更多 API 和 UI，容易让本阶段范围过大。
- JPA `ddl-auto=update` 不会自动删除旧列，保留旧表并新增/重命名核心字段更适合学校试点阶段。

设计方式：

- 扩展 `AiStandardLibraryLayer`：
  - `SKILL_UNIT`：能力点。
  - `MISTAKE_POINT`：易错点。
  - 暂保留 `BASIC_CAUSE`、`IMPROVEMENT_POINT` 兼容旧数据和旧测试。
- 新增/重定义核心字段：
  - `knowledgeNodeCodes`：关联最细知识点。
  - `skillUnitCode`：易错点所属能力点。
  - `mistakeType`：易错类型。
  - `commonMisconception`：常见误解。
  - `prerequisiteKnowledgeCodes`：前置知识。
- 旧字段如 `hintL1`、`hintL2`、`hintL3`、`evidenceSignals` 等不作为 v3 教师端核心字段展示；表列可暂存兼容。

替代方案：

- 立即新增 `informatics_skill_units` 和 `informatics_mistake_points`：结构最干净，但改动面较大。
- 继续使用 `BASIC_CAUSE` 表达易错点：短期省事，但语义仍混乱。

### 2. 内容生成采用“全量铺底 + 高频精修”

每个最细知识点自动生成：

- 至少 1 个能力点。
- 至少 1 个易错点。

高频知识点额外增加人工样板：

- 循环边界。
- 数组下标。
- 输入输出。
- 二分答案。
- DP 状态定义。
- 递归终止。
- 搜索访问标记。
- 复杂度估算。

原因：

- 全量铺底保证 AI 可以按知识树返回标准 ID。
- 高频精修保证真实教学中最常见问题的质量。
- 不追求本轮一夜之间把所有 700 多个节点都人工精修到极致，避免内容质量不可控。

### 3. 旧 AI pack 通过适配层继续输出 basic causes

`StandardLibraryPackBuilder` 现有逻辑仍依赖 `BasicCauseOption`、`ImprovementPointOption`、issue tags 和 fine tags。v3 后：

- `MISTAKE_POINT` 可映射为 `BasicCauseOption`。
- `SKILL_UNIT` 不直接进入旧 pack，主要作为易错点的能力维度。
- 旧 `BASIC_CAUSE` 仍可被读取，保证历史兼容。
- 提高层固定建议不再扩充，后续由 AI 个性化生成。

### 4. 教师端只展示 v3 核心字段

教师端 AI 标准库编辑器改为重点维护：

- 类型：能力点 / 易错点。
- ID、名称、分类。
- 关联知识点。
- 所属能力点。
- 易错类型。
- 定义。
- 常见误解。
- 学生可见标准名称。
- 教师说明。
- 适用语言。
- 严重度。
- 前置知识。
- 版本。
- 启用状态。

不再展示：

- L1/L2/L3。
- 证据信号。
- 代码形态信号。
- 判题信号。
- 固定提高建议。
- 教学动作。

## Risks / Trade-offs

- [Risk] 旧表字段仍存在，代码层面看起来不是完全删除。  
  → Mitigation：从 DTO、教师端核心表单和种子内容中移除旧字段语义；生产数据库列删除留到正式迁移阶段。

- [Risk] 自动生成的能力点和易错点质量不如人工内容。  
  → Mitigation：自动内容只作为覆盖底座，高频章节加入人工样板；后续根据 AI 诊断效果逐步精修。

- [Risk] `MISTAKE_POINT` 映射到旧 `BasicCauseOption` 会有语义折中。  
  → Mitigation：这是过渡兼容层；下一阶段升级 AI 输出协议后可直接使用 `matchedMistakePointId`。

- [Risk] 标准库条目增加后上下文会变长。  
  → Mitigation：本阶段只保证结构可用；下一阶段必须做检索召回，只传相关能力点和易错点。

## Migration Plan

1. 扩展实体字段和枚举。
2. 更新 DTO、服务、种子器和教师端类型。
3. 生成 v3 能力点和易错点种子内容。
4. 让 `StandardLibraryPackBuilder` 同时兼容旧 `BASIC_CAUSE` 和新 `MISTAKE_POINT`。
5. 更新测试，验证种子规模、挂载关系、教师 CRUD 和旧 AI pack 输出。
6. 后续如果正式引入迁移工具，再清理数据库旧列。

## Open Questions

- 是否在下一阶段把 `skill_unit` 和 `mistake_point` 拆成独立物理表。
- 是否在 AI 输出协议中新增 `matchedSkillUnitId`、`matchedMistakePointId`。
- 高频章节人工精修的优先级是否按课堂题库数据动态调整。
