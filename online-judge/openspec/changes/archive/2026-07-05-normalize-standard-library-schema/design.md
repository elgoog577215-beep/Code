## Context

项目已经有 `informatics_knowledge_nodes` 作为信息学知识树，节点类型覆盖大章节、小章节、主题和知识点。AI 标准库目前主要由 `ai_standard_library_items` 承载，这张表用 `layer` 同时区分 `SKILL_UNIT`、`MISTAKE_POINT`、`BASIC_CAUSE` 和 `IMPROVEMENT_POINT`，并混合保存学生解释、证据信号、代码模式、提示语、知识节点 code 等字段。这个结构可以快速扩条目，但长期会让数据库语义不清、兼容层和主模型混在一起。

这次收纳的核心判断是：知识树已经存在，标准库不需要再复制一棵树；标准库专注描述知识点下面的能力、易错和提升。外接大模型负责基于当前提交做证据分析，因此新主结构不增加独立“证据模式”表。

## Goals / Non-Goals

**Goals:**

- 复用 `informatics_knowledge_nodes` 作为知识树。
- 新增独立表保存能力点、易错点、提升点、标准库关系和旧库映射。
- 从现有种子目录同步填充新结构，避免已有标准库资产丢失。
- 让 AI 候选读取优先使用新结构，旧扁平表只作为兼容回退。
- 保持现有旧表、旧 API 和历史数据不被破坏。

**Non-Goals:**

- 不删除 `ai_standard_library_items`。
- 不引入独立证据模式表。
- 不重写完整 AI 诊断输出协议。
- 不做人工审核界面的完整重做。

## Decisions

### 复用既有知识树

使用 `InformaticsKnowledgeNode` 作为标准库上层知识树，能力点用 `knowledgeNodeCode` 指向末端或近末端知识节点。这样避免出现两棵知识树不一致的问题。

备选方案是新增 `standard_knowledge_nodes`，但会立刻带来同步、迁移和命名冲突，所以不采用。

### 新增规范标准库表

新增以下主表：

- `ai_standard_skill_units`: 知识点下的能力点。
- `ai_standard_mistake_points`: 能力点下的易错点和错因。
- `ai_standard_improvement_points`: 能力点或知识点下的提升点。
- `ai_standard_library_relations`: 前置、相关、易混淆、可迁移等关系。
- `ai_standard_library_legacy_mappings`: 旧 `layer/code` 到新结构的映射。

新结构中不保存 `evidenceSignals/commonCodePatterns/judgeSignals/requiredEvidence` 这类证据模式字段。必要的轻量诊断提示可放在易错点的 `symptom`、`misconception`、`repairStrategy` 等自然语言字段中，作为模型参考而不是规则引擎。

### 旧种子同步到新结构

保留现有 `AiStandardLibrarySeedCatalog` 作为兼容来源，并新增规范结构同步器：

- `SKILL_UNIT` 生成或更新 `AiStandardSkillUnit`。
- `MISTAKE_POINT` 和 `BASIC_CAUSE` 生成或更新 `AiStandardMistakePoint`。
- `IMPROVEMENT_POINT` 生成或更新 `AiStandardImprovementPoint`。
- 每个旧条目写入 `AiStandardLibraryLegacyMapping`。

同步器应幂等执行。对已有正式数据只补缺失或刷新种子来源字段，避免删除用户维护内容。

### 读取路径优先新结构

`AiStandardLibraryService` 继续保留旧 API，但用于 AI 候选包的读取方法优先从新结构构造兼容对象。这样 `StandardLibraryPackBuilder`、`SearchLocationRetrievalService` 和现有 prompt 可以逐步迁移，不需要一次性推翻诊断链路。

## Risks / Trade-offs

- [Risk] 两套表短期共存会增加维护面。→ 用映射表明确旧结构只是兼容来源，并让 AI 读取优先走新结构。
- [Risk] 旧种子里部分 `BASIC_CAUSE` 没有真实能力点。→ 迁移时挂到可解释的兼容能力点，后续人工整理时再细化。
- [Risk] 新结构引用知识节点 code，但数据库没有强外键。→ 通过种子测试校验知识节点 code 必须存在，避免 H2/PostgreSQL 兼容问题。
- [Risk] 诊断链路仍接收旧 `StandardLibraryPack` DTO。→ 本次先构造兼容包，下一阶段再讨论 AI 链路协议收敛。

## Migration Plan

1. 新增规范结构实体和仓储。
2. 新增规范标准库同步器，从现有种子填充新表和映射表。
3. 调整 `AiStandardLibraryService`，让 AI 候选读取优先使用新结构。
4. 增加结构、幂等、迁移映射和候选包测试。
5. 保留旧表和旧接口，确认测试通过后再进入下一阶段 AI 链路设计。
