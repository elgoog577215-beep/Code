## ADDED Requirements

### Requirement: AI quality overview must expose structured quality dimensions
系统 SHALL 在教师 AI 质量概览中输出结构化质量维度，覆盖诊断置信、证据链、提示安全、学习动作、模型运行和教师纠错沉淀。

#### Scenario: Quality dimensions are computed for analyzed submissions
- **WHEN** 教师请求某个作业的 AI 质量概览
- **THEN** 响应包含每个质量维度的状态、分数、摘要、证据引用和建议动作
- **AND** 维度状态 MUST 能区分健康、需要观察和需要行动

### Requirement: AI quality overview must rank improvement priorities
系统 SHALL 基于质量维度生成改进优先级，帮助老师或开发者判断下一步应先处理哪个 AI 能力短板。

#### Scenario: Runtime failure and safety risk coexist
- **WHEN** 同一作业同时存在模型运行失败和高泄题风险
- **THEN** 改进优先级 MUST 优先暴露模型运行失败和教学安全风险
- **AND** 每个优先级 MUST 包含原因、建议动作和证据引用

### Requirement: AI quality overview must report eval readiness
系统 SHALL 判断教师纠错和风险样本是否已经足够进入评测沉淀，并输出评测就绪状态。

#### Scenario: Teacher corrections include eval candidates
- **WHEN** 教师已经标记至少一个诊断纠错为 eval candidate
- **THEN** 响应 MUST 标记评测沉淀处于就绪或部分就绪状态
- **AND** 响应 MUST 说明应优先沉淀的标签纠错方向

### Requirement: Learning action evidence must affect quality feedback
系统 SHALL 将学习动作执行证据纳入 AI 质量反馈，但不得让它覆盖当前提交诊断证据。

#### Scenario: Previous learning action is contradicted
- **WHEN** 最近分析中存在学习动作执行状态为 CONTRADICTED 的证据
- **THEN** 学习动作质量维度 MUST 标记为需要行动
- **AND** 改进建议 MUST 要求降低提示粒度或调整教学动作

### Requirement: Teacher workspace must consume AI quality signals
教师工作台 SHALL 展示当前作业的 AI 质量维度、改进优先级和评测沉淀状态，让老师能直接判断哪些 AI 能力需要复核或沉淀。

#### Scenario: Teacher opens an assignment with quality signals
- **WHEN** 教师打开某个作业的课堂过程视图
- **THEN** 页面 MUST 请求该作业的 AI 质量概览
- **AND** 页面 MUST 展示维度状态、分数、摘要、建议动作和证据引用数量
- **AND** 页面 MUST 展示最高优先级改进项和 eval readiness

#### Scenario: Quality overview is temporarily unavailable
- **WHEN** 作业过程数据可用但 AI 质量概览读取失败
- **THEN** 页面 MUST 保留作业过程视图
- **AND** 页面 MUST 显示 AI 质量信号暂不可用，而不是阻塞老师查看课堂过程数据

### Requirement: Teacher workspace must surface diagnosis eval candidates
教师工作台 SHALL 展示当前作业已标记为 eval candidate 的教师纠错样本，让 AI 质量风险可以被继续沉淀为可复核样本。

#### Scenario: Assignment has diagnosis eval candidates
- **WHEN** 教师打开某个作业的课堂过程视图
- **THEN** 页面 MUST 请求该作业的诊断 eval 候选样本
- **AND** 页面 MUST 展示候选数量、题目、原始错因、教师修正错因、教师备注和代码预览
- **AND** 候选样本 MUST 与当前作业绑定，不能混入其他作业的纠错样本

#### Scenario: Eval candidate list is temporarily unavailable
- **WHEN** 作业过程和 AI 质量概览可用但 eval 候选样本读取失败
- **THEN** 页面 MUST 保留 AI 质量和课堂过程视图
- **AND** 页面 MUST 显示候选样本暂不可用，而不是阻塞老师继续查看当前作业

### Requirement: Diagnosis eval candidates must export fixture drafts
系统 SHALL 将当前作业的诊断 eval 候选样本导出为教师校正评测 fixture 草稿，但不得在运行时直接改写测试资源文件。

#### Scenario: Teacher exports fixture draft
- **WHEN** 教师请求某个作业的诊断 eval fixture 草稿
- **THEN** 响应 MUST 包含与 `diagnosis-eval-fixtures/teacher-corrections.json` 兼容的 fixture 列表
- **AND** 每条 fixture MUST 包含题目、提交代码、测试点结果、原始诊断、教师修正、期望标签、禁用短语、来源说明和质量元数据
- **AND** 响应 MUST 保留 assignmentId、candidateCount 和可读导出说明，方便人工审查后再沉淀进测试资源

#### Scenario: Assignment has no eval candidates
- **WHEN** 当前作业没有诊断 eval 候选样本
- **THEN** 导出响应 MUST 返回空 fixture 列表
- **AND** 响应 MUST 说明需要先保存教师校正并标记 eval candidate
