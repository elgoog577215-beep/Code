# discipline-data-quality-audit Specification

## Purpose
TBD - created by archiving change refine-discipline-data-quality. Update Purpose after archive.
## Requirements
### Requirement: 学科范围必须支持重叠映射与来源追溯
系统 SHALL 允许同一信息学知识节点同时映射到高中课程框架、NOI 竞赛框架和本地统一学科框架，并 SHALL 保存范围、覆盖角色、来源和审核状态。

#### Scenario: 同一知识节点覆盖多个框架
- **WHEN** 一个算法概念同时属于高中课程和 NOI 入门级范围
- **THEN** 数据库 SHALL 为同一 `knowledge_node_code` 保存两条不同框架映射
- **AND** 系统 SHALL NOT 创建两套平行知识节点

#### Scenario: 下级节点继承范围
- **WHEN** 叶子知识点没有逐条直接映射但其最近祖先存在启用范围映射
- **THEN** 质量报告 SHALL 将该叶子标记为继承覆盖
- **AND** SHALL 区分直接映射与继承映射

### Requirement: 学科质量检查必须区分阻断项与质量债务
系统 SHALL 提供可重复运行的数据库质量检查，输出数据集粒度、总量、阻断性完整性错误和渐进式质量债务，并以非零退出码阻止阻断项进入发布。

#### Scenario: 存在启用孤儿关系
- **WHEN** 启用标准库关系的源或目标不存在或已停用
- **THEN** 学科质量门禁 SHALL 失败
- **AND** 输出 SHALL 包含失效源和失效目标数量

#### Scenario: 仍有待精修模板内容
- **WHEN** 数据库不存在阻断项但仍有知识点使用已知模板描述
- **THEN** 学科质量门禁 MAY 通过
- **AND** 报告 SHALL 输出模板数量、人工精修数量和剩余比例

### Requirement: 学科质量报告必须可复核
学科质量报告 SHALL 记录检查时间、数据库、口径、关键 SQL 指标、严重度、修正范围和剩余债务，使后续批次可以按同一口径比较。

#### Scenario: 生成迁移前后报告
- **WHEN** 运维人员在同一数据库迁移前后运行质量报告
- **THEN** 两次结果 SHALL 使用相同指标名称与计算口径
- **AND** SHALL 能比较阻断项是否清零以及模板债务是否下降
