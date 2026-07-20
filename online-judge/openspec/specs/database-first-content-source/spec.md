# database-first-content-source Specification

## Purpose
TBD - created by archiving change migrate-seeded-content-to-database. Update Purpose after archive.
## Requirements
### Requirement: 正式内容必须以数据库为主库
系统 SHALL 将正式题库、知识树和 AI 标准库内容保存在正式数据库中，并 SHALL 将 GitHub 仓库限制为结构、代码、治理工具、迁移工具和审计文档来源。

#### Scenario: 应用启动不播种正式内容
- **WHEN** 应用在 `school` 或正式部署配置下启动
- **THEN** 系统 SHALL NOT 从 Java seed、资源 seed 或 `CommandLineRunner` 内容播种器写入题库、知识树或标准库正式表
- **AND** 系统 SHALL 只读取数据库中已经存在的正式内容

#### Scenario: 新增正式内容
- **WHEN** 需要新增题目、知识节点、能力点、易错点或提升点
- **THEN** 系统 SHALL 通过管理 API、教师治理流程或可审计迁移脚本写入正式数据库
- **AND** 系统 SHALL NOT 要求开发者新增 `SeedCatalog`、`VxExpansionSeeds` 或资源 seed 文件

### Requirement: 一次性迁移必须可审计且不可作为长期内容源
系统 SHALL 允许一次性迁移工具把历史 seed 中仍有价值的内容写入正式数据库，但这些迁移工具 MUST 有明确的一次性执行语义，不得成为应用运行时内容来源。

#### Scenario: 执行一次性迁移
- **WHEN** 运维人员执行历史 seed 内容迁移
- **THEN** 迁移 SHALL 先创建数据库备份
- **AND** SHALL 输出写入、跳过、更新和失败的数量摘要
- **AND** SHALL 提供执行后验证查询

#### Scenario: 迁移完成后重启
- **WHEN** 迁移完成后应用重启
- **THEN** 应用 SHALL NOT 再执行历史 seed 导入逻辑
- **AND** 数据库内容数量 SHALL NOT 因重启重复增加

### Requirement: 内容型 seed 必须从运行时项目中移除
系统 SHALL 移除运行时依赖的内容型 seed 文件和 seed 类；如需保留历史材料，只能放入归档或迁移记录，且不得被生产代码加载。

#### Scenario: 运行时代码扫描
- **WHEN** 检查 `src/main` 运行时代码
- **THEN** 不应存在用于正式内容写入的 `PublicProblemSeedCatalog`、`InformaticsKnowledgeSeedCatalog`、`AiStandardLibrarySeedCatalog`、`AiStandardLibraryV*Seeds` 或资源 seed 读取路径

#### Scenario: 测试夹具隔离
- **WHEN** 测试需要构造题目、知识节点或标准库条目
- **THEN** 测试 MAY 使用测试 fixture 或 repository 直接写入测试数据库
- **AND** 测试 fixture SHALL NOT 作为正式内容来源

### Requirement: 数据库迁移后必须验证读取链路
迁移完成后，系统 SHALL 通过数据库查询、API 查询和 AI 导航读取验证正式内容可用。

#### Scenario: 验证题库内容
- **WHEN** 迁移题库内容后
- **THEN** 系统 SHALL 验证公共题库列表和代表性题目详情可以从数据库读取
- **AND** 历史学生提交引用的题目 SHALL 保持可解析

#### Scenario: 验证标准库导航
- **WHEN** 迁移知识树和标准库内容后
- **THEN** 系统 SHALL 验证根知识区、代表性知识点、能力点、易错点和提升点可以从数据库导航 API 读取
- **AND** 代表性 `if 条件` 路径 SHALL 不再返回旧 `KB_*` 模板化掌握偏差条目作为启用正式内容

### Requirement: 正式测试点语义必须以数据库为主库
系统 SHALL 将正式测试点的评测意图、学习目标、竞赛角色、揭示策略和标准库映射保存在正式数据库中，并 SHALL 只通过教师治理流程或可审计版本化迁移发布。

#### Scenario: 发布一批正式测试点语义
- **WHEN** 开发者精修现有正式题目的测试点语义
- **THEN** 内容 SHALL 通过未发布的 Flyway 迁移写入
- **AND** Java seed、测试 fixture 和运行时初始化器 SHALL NOT 成为生产语义来源

#### Scenario: 教师新建未评审测试点
- **WHEN** 教师通过管理流程新建测试点但尚未完成语义审核
- **THEN** 系统 MAY 暂存缺少正式语义的测试点
- **AND** 该测试点 SHALL NOT 通过正式内容发布门禁
