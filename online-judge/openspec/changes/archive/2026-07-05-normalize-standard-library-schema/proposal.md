## Why

当前 AI 标准库主表把能力点、易错点、旧基础错因、旧提升建议和若干证据信号字段混在一张扁平表里，导致数据库结构难以解释、扩张和迁移。现在需要把标准库收纳成更符合教学认知的结构：复用既有知识树，在末端知识点下组织能力点、易错点和提升点，让数据库负责存知识结构，让外接大模型负责基于当前提交做智能分析。

## What Changes

- 新增标准库规范结构：能力点、易错点、提升点、标准库关系和旧库映射独立存储。
- 复用既有 `informatics_knowledge_nodes` 作为知识树，不再新建重复知识树表。
- 将现有 `AiStandardLibrarySeedCatalog` 的扁平种子同步整理进新结构，保留旧表用于兼容现有接口和历史数据。
- 新结构不引入独立“证据模式”表；证据来自学生当前提交、评测结果、错误日志和 AI 分析输出。
- 主 AI 标准库读取路径应优先使用新结构，再在必要时回退到旧扁平表。
- 旧 `BASIC_CAUSE`、旧 `IMPROVEMENT_POINT` 通过映射表迁移到新结构，不再作为长期主模型。

## Capabilities

### New Capabilities

- `standard-library-normalized-schema`: 定义 AI 标准库应以知识树、能力点、易错点、提升点和旧库映射的规范结构存储，并支持兼容旧扁平条目。

### Modified Capabilities

- `ai-diagnosis-orchestrator-v2`: 诊断编排读取标准库候选时，应优先使用规范结构生成候选包，保留旧表兼容回退。

## Impact

- 影响 `learning.standardlibrary` 下的领域实体、仓储、种子同步、服务读取和质量测试。
- 影响 `submission` 下标准库候选包构建、检索定位候选获取和 AI 诊断上下文。
- 不做破坏性删除，不清空旧表；本次只新增规范结构并接入读取优先级。
- 数据库仍由 JPA `ddl-auto=update` 维护表结构，兼容当前 H2/PostgreSQL 开发部署方式。
