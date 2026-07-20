# test-case-semantic-evidence Specification

## Purpose
TBD - created by archiving change add-pedagogical-test-case-intents. Update Purpose after archive.
## Requirements
### Requirement: 正式测试点必须具有可审计的学科语义
系统 SHALL 为每个正式测试点保存稳定语义身份、评测意图、学习目标、竞赛角色、揭示策略、标准库知识点和能力点映射，并 SHALL 保证语义描述的是测试覆盖范围而不是学生错因。

#### Scenario: 精修现有正式测试点
- **WHEN** V8 迁移完成
- **THEN** 现有 16 道题的 45 个测试点 SHALL 全部具有唯一语义身份
- **AND** 每个测试点 SHALL 具有经过审核的意图类型、学习目标、竞赛角色和合法标准库映射

#### Scenario: 同题测试点形成互补覆盖
- **WHEN** 审计一道包含两个及以上测试点的正式题目
- **THEN** 测试点集合 SHALL 至少覆盖两种不同评测意图
- **AND** SHALL 能区分代表性样例与边界、结构、规模或性能类覆盖

### Requirement: 判题结果必须快照测试点语义
系统 SHALL 在执行测试点时把测试点身份和当时的安全语义快照保存到 `submission_case_results`，不得只保存易漂移的顺序编号。

#### Scenario: 新提交执行测试点
- **WHEN** 判题器保存一个测试点执行结果
- **THEN** 结果 SHALL 保存 `test_case_id`、稳定语义 code、意图类型、标题、泛化说明、学习目标、竞赛角色和揭示策略
- **AND** 后续修改测试点内容 SHALL NOT 改写该历史快照

#### Scenario: 回填历史判题结果
- **WHEN** 历史结果只有 `problem_id` 对应的测试点顺序编号
- **THEN** 迁移 SHALL 按题目内稳定顺序回填测试点身份和语义快照
- **AND** 无法唯一映射的结果 SHALL 保持可识别的未映射状态，不得猜测关联

### Requirement: 测试点语义必须遵守隐藏数据边界
系统 SHALL 允许 AI 读取隐藏测试点的泛化评测意图，但 MUST NOT 暴露隐藏输入、期望输出、实际输出、具体数值组合或足以反推测试数据的描述。

#### Scenario: 隐藏测试点失败
- **WHEN** 一个隐藏测试点执行失败且其揭示策略为 `AI_GENERALIZED`
- **THEN** AI 上下文 SHALL 只包含语义 code、意图类型、泛化说明、学习目标和竞赛角色
- **AND** 输入、期望输出和实际输出 SHALL 为空或隐藏占位

#### Scenario: 学生可见反馈引用隐藏语义
- **WHEN** 模型使用隐藏测试点语义生成学生反馈
- **THEN** 反馈 SHALL 只要求学生自构同类边界或状态检查
- **AND** SHALL NOT 原样复述隐藏测试点说明或推测具体隐藏数据

### Requirement: 测试点语义必须具有独立质量门禁
系统 SHALL 提供可重复执行的测试点语义质量检查，并 SHALL 以非零退出码阻止缺失、重复、断链、薄内容或隐藏数据策略不一致的正式内容进入生产。

#### Scenario: 正式测试点语义不完整
- **WHEN** 正式测试点缺少语义 code、意图、学习目标、竞赛角色、审核状态或库版本
- **THEN** 质量门禁 SHALL 失败

#### Scenario: 标准库映射断链
- **WHEN** 测试点引用不存在的知识点或能力点，或能力点不属于所引用知识路径
- **THEN** 质量门禁 SHALL 失败

#### Scenario: 隐藏策略与测试点可见性冲突
- **WHEN** 隐藏测试点标记为公开样例，或公开测试点标记为隐藏泛化
- **THEN** 质量门禁 SHALL 失败
