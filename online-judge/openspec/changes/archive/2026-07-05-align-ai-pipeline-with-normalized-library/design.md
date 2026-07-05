## Context

当前 AI 主链路已经包含证据包、规则信号、本地召回、可选搜索定位、单诊断 Agent、校验归一化和标准库增长候选。上一轮已经将标准库数据库收纳为规范结构，但 `StandardLibraryPack` 仍以旧兼容字段为主，模型看见的是 `basicCauses`、`improvementPoints`、`skillUnits` 和 `mistakePoints` 的平铺列表，而不是“知识点下面有哪些能力、能力下面有哪些易错点和提升方向”的结构。

这会让模型虽然读到新库数据，却仍按旧标签方式理解。当前要做的是让标准库结构以明确视图进入 AI 链路，同时保持旧输出 schema 和前端兼容。

## Goals / Non-Goals

**Goals:**

- 在模型输入中提供结构化标准库视图，表达知识节点、能力点、易错点、提升点的父子关系。
- 召回阶段补全父能力、同能力易错和相关提升，避免模型只看到孤立候选。
- 搜索定位选择器输出 selected pack 时保留结构上下文。
- prompt 明确以新结构为主，要求 AI 基于证据选择多个真实错因和多个真实提升方向。
- 旧兼容字段继续保留，避免破坏现有 validator、mapper、前端和历史结果。

**Non-Goals:**

- 不重写 `AdviceGenerationOutput` 对外 JSON schema。
- 不删除 `basicCauses`、`improvementPoints` 等旧兼容字段。
- 不新增证据模式表。
- 不把学生快反馈旁路线完全并入主链路；本次只增强主诊断 runtime。

## Decisions

### 在 `StandardLibraryPack` 增加结构视图

新增 `structureVersion` 和 `knowledgeGroups`。每个 `KnowledgeGroup` 对应一个知识节点，包含该节点下的 `SkillUnitGroup`；每个能力点组包含相关易错点和提升点。旧字段仍然保留，用于现有 validator 和 mapper。

备选方案是新建 `StandardLibraryPackV2`，但会导致 runtime、selector、validator 和测试大面积迁移。当前选择在原 DTO 上扩展字段，风险更低。

### 召回上下文从“候选列表”升级为“候选加邻域”

`SearchLocationRetrievalService` 仍返回候选列表，但每个候选会携带：

- `primaryKnowledgeNodeCode`
- `parentSkillUnitId`
- `siblingMistakePointIds`
- `extensionCandidateIds`
- `structurePath`

这些字段不是证据规则，只是标准库结构邻域，帮助模型区分相邻错因。

### 选择器补全结构邻域

`SearchLocationPackSelector` 不只选择模型或本地召回直接点中的 id。它会：

- 选中易错点时补父能力点。
- 选中能力点时补该能力点下的候选易错点。
- 选中易错点或能力点时补同能力点下少量相邻易错点。
- 补同知识点下少量提升方向。

这样最终 `standardLibrary` 进入诊断阶段时，既有精选项，也有可对照的结构上下文。

### prompt 用新结构做主语

搜索定位 prompt 和诊断 prompt 都应明确：标准库主结构是知识节点、能力点、易错点和提升点；旧 `basicCauses` 只是兼容视图。模型必须先读结构视图，再根据当前提交证据选择多个真实命中项，不允许因为结构存在就硬套弱候选。

## Risks / Trade-offs

- [Risk] 结构视图增加 prompt token。→ runtime compact 时压缩每组数量和文本长度。
- [Risk] 候选邻域补全可能带入弱相关项。→ selector 控制每组数量，prompt 继续要求证据优先，不得硬套。
- [Risk] 旧 validator 仍校验旧字段。→ 保留旧字段并同步填充，新增结构视图只增强模型理解。
- [Risk] 改动主链路影响面较大。→ 增加 retrieval、selector、runtime compact 和 prompt 测试，先保持输出 schema 兼容。

## Migration Plan

1. 扩展 `StandardLibraryPack` 和 `SearchLocationCandidate` 的结构字段。
2. 调整召回服务和选择器，生成结构上下文和 `knowledgeGroups`。
3. 调整 runtime compact 和 prompt 模板。
4. 增加针对结构视图、邻域补全和 prompt 语义的测试。
5. 运行 OpenSpec 校验和 AI 链路相关后端测试。
