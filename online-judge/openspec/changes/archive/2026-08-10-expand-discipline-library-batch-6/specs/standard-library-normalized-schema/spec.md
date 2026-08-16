## ADDED Requirements

### Requirement: 第六批标准库必须按完整主题包增长
V11 SHALL 为 48 个精修知识点各新增 1 个启用正式能力、3 个启用易错点、1 个启用提升点和 1 组课堂—竞赛应用场景；每个主题包 SHALL 形成知识点到能力、易错、提升和场景的完整归属闭环。

#### Scenario: 保存第六批主题包
- **WHEN** V11 执行完成
- **THEN** `informatics-discipline-quality-v6` SHALL 包含 48 个能力点、144 个易错点和 48 个提升点
- **AND** `informatics-discipline-application-v2` SHALL 包含 96 个场景和 48 个完整迁移对
- **AND** 每个知识点 SHALL 恰好拥有本批 1 个主能力、3 个易错点、1 个提升点、1 个课堂场景和 1 个竞赛场景

#### Scenario: 主题包缺少一层
- **WHEN** 任一主题缺少规范能力、三个不同易错点、提升点或成对场景
- **THEN** 迁移 SHALL 失败
- **AND** 系统 SHALL NOT 发布部分同步内容

### Requirement: 第六批易错点必须描述不同可观察失败
同一主题包的三个易错点 SHALL 分别表达不同的触发条件、状态偏差或验证缺口，并 SHALL 提供具体误区、代码或行为症状和修复动作；不得通过替换名词复制同一模板。

#### Scenario: 三个错因语义重复
- **WHEN** 同一能力下多个易错点拥有相同规范化名称、相同症状或相同修复三元组
- **THEN** 学科质量门禁 SHALL 失败
- **AND** 维护者 SHALL 将其重写为可由不同证据区分的错误行为

#### Scenario: 错因无法由提交证据判断
- **WHEN** 易错点只描述人格、态度或抽象“理解不足”，没有可观察代码、输出、状态或复测证据
- **THEN** 该易错点 SHALL NOT 进入正式库

### Requirement: 第六批提升点和场景必须可执行可验收
每个第六批提升点 SHALL 关联本能力下至少两个启用易错点并描述具体练习、学生收益和教师验收；每组场景 SHALL 分别提供课堂产物与竞赛约束下的迁移任务。

#### Scenario: 课堂与竞赛场景成对
- **WHEN** AI 或教师展开第六批能力的诊断层
- **THEN** 响应 SHALL 返回同一 `transfer_pair_code` 下的课堂与竞赛场景
- **AND** 两个场景 SHALL 复用同一知识点、能力、提升点和合法易错点集合

#### Scenario: 场景被误当作诊断证据
- **WHEN** 场景中的 `common_failure` 与当前提交相似但代码、判题和 evidenceRefs 不支持
- **THEN** AI 诊断 SHALL NOT 因场景文本强制命中该错因
- **AND** 场景 code SHALL NOT 成为正式诊断 ID

### Requirement: 第六批内容必须同步规范主表和兼容层
V11 新增的能力、易错点和提升点 SHALL 使用同一稳定 code 同步规范化主表、启用平铺快照和 `MAPPED` legacy mapping，并 SHALL 保持主知识点、能力归属、相关知识点和启用状态一致。

#### Scenario: 兼容结构缺失或错配
- **WHEN** 任一第六批规范条目缺少同 code 平铺快照或 MAPPED 映射，或关键归属不一致
- **THEN** 数据质量门禁 SHALL 失败
- **AND** 应用 SHALL NOT 带着部分兼容内容发布
