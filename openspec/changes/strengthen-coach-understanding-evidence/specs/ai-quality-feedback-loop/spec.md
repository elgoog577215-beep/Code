## MODIFIED Requirements

### Requirement: AI quality overview must expose structured quality dimensions
系统 SHALL 在教师 AI 质量概览中输出结构化质量维度，覆盖诊断置信、证据链、提示安全、学习动作、模型运行、教师纠错沉淀和 Coach 理解证据。

#### Scenario: Quality dimensions are computed for analyzed submissions
- **WHEN** 教师请求某个作业的 AI 质量概览
- **THEN** 响应包含每个质量维度的状态、分数、摘要、证据引用和建议动作
- **AND** 维度状态 MUST 能区分健康、需要观察和需要行动
- **AND** Coach 理解维度 MUST 反映学生回答是否包含可验证证据或存在安全风险

### Requirement: AI quality overview must rank improvement priorities
系统 SHALL 基于质量维度生成改进优先级，帮助老师或开发者判断下一步应先处理哪个 AI 能力短板。

#### Scenario: Runtime failure and safety risk coexist
- **WHEN** 同一作业同时存在模型运行失败和高泄题风险
- **THEN** 改进优先级 MUST 优先暴露模型运行失败和教学安全风险
- **AND** 每个优先级 MUST 包含原因、建议动作和证据引用

#### Scenario: Coach answers lack verifiable evidence
- **WHEN** 学生 Coach 回答多数缺少可验证证据
- **THEN** 改进优先级 MUST 包含 Coach 理解证据短板
- **AND** 建议动作 MUST 要求降低提示粒度或补充可观察产出
