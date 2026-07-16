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

### Requirement: 学科质量批次必须以同口径生产基线证明债务下降
每个学科内容精修批次 SHALL 在迁移前固化生产数据库、数据粒度、指标定义和债务数量，并 SHALL 在迁移后用相同查询证明目标债务下降且结构阻断项没有回归。

#### Scenario: 第二批精修完成
- **WHEN** Flyway V3 在生产备份的隔离恢复库执行完成
- **THEN** `informatics-knowledge-discipline-v2` 知识点 SHALL 不少于 30 个
- **AND** 累计人工精修知识点 SHALL 不少于 54 个
- **AND** 模板化知识点描述 SHALL 从 562 降到不高于 532
- **AND** 缺少提升点的启用能力点 SHALL 从 65 降到不高于 53

#### Scenario: 结构问题在内容批次中回归
- **WHEN** 第二批内容迁移造成孤儿引用、重复启用条目、无效前置、失效兼容映射或分类实现词重新出现
- **THEN** 学科质量门禁 SHALL 失败
- **AND** 系统 SHALL NOT 用内容债务允许值掩盖结构错误

### Requirement: 正式提升点三表一致性必须成为发布门禁
质量检查 SHALL 比较第二批正式提升点在规范化主表、平铺兼容快照和 legacy mapping 中的启用 code 集合及关键归属，任一缺失或错配 SHALL 阻断发布。

#### Scenario: 规范化提升点缺少兼容快照
- **WHEN** `informatics-discipline-quality-v2` 提升点存在于规范化主表但不存在同 code 的启用 `IMPROVEMENT_POINT` 快照
- **THEN** 学科质量门禁 SHALL 失败

#### Scenario: 兼容映射目标错误
- **WHEN** 第二批提升点的 legacy mapping 不是同 code、`MAPPED` 且目标类型不是 `IMPROVEMENT_POINT`
- **THEN** 学科质量门禁 SHALL 失败

### Requirement: 第三批学科质量选择必须区分结构引用与有限行为证据
第三批候选 SHALL 以启用规范能力点、易错点和提升点的去重结构引用为主证据，并 SHALL 只把诊断路径出现次数作为有上限的加权证据；报告必须分别披露两种口径和行为样本限制。

#### Scenario: 行为路径重复出现
- **WHEN** 一个模板知识点同时具有规范结构引用并在诊断路径中重复出现
- **THEN** 选择排序 SHALL 增加最多 4 次行为路径的有限权重
- **AND** 人工审校 SHALL 验证完整路径，不能只按中文名称自动入选

#### Scenario: 行为证据不足
- **WHEN** 诊断事实样本小或多数路径不是正式标准库挂接
- **THEN** 系统 SHALL NOT 把诊断次数解释为学生错误发生率
- **AND** 结构引用和学科分支完整性 SHALL 继续作为主要选择依据

### Requirement: 第三批迁移必须证明质量债务下降且业务数据稳定
Flyway V4 SHALL 在同一生产 V3 基线上新增不少于 30 个 `informatics-knowledge-discipline-v3` 精修知识点和 12 个 `informatics-discipline-quality-v3` 正式提升点，并 SHALL 保持结构门禁与业务计数稳定。

#### Scenario: 第三批隔离恢复完成
- **WHEN** V4 在生产备份的隔离恢复库执行完成
- **THEN** 累计人工精修知识点 SHALL 不少于 84 个
- **AND** 模板化知识点描述 SHALL 从 532 降到不高于 502
- **AND** 缺少提升点的启用能力点 SHALL 从 53 降到不高于 41
- **AND** V2/V3 已归零的结构问题 SHALL 继续为零

#### Scenario: 业务表出现非预期变化
- **WHEN** V4 前后题目、测试用例、提交、提交分析、诊断事实或学生反馈计数发生变化
- **THEN** 发布门禁 SHALL 失败
- **AND** 系统 SHALL NOT 用标准库内容增量解释业务事实变化

### Requirement: 兼容占位能力不得计入正式提升点补齐
第三批质量审计 SHALL 单独识别 `SK_COMPAT_*` 或明确声明用于兼容旧标签的能力点，并 SHALL 禁止为这些占位能力新增 `informatics-discipline-quality-v3` 提升点。

#### Scenario: 第三批提升点关联兼容能力
- **WHEN** 一个 V4 提升点的 `skill_unit_code` 指向兼容占位能力
- **THEN** 学科质量门禁 SHALL 失败
- **AND** 报告 SHALL 把该能力保留在待合并或退役清单而不是正式教学缺口
