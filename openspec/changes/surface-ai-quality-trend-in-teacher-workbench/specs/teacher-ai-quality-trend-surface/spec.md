## ADDED Requirements

### Requirement: 教师工作台必须展示跨作业 AI 质量趋势
教师工作台 SHALL 加载跨作业 AI 质量趋势，并在当前作业 AI 质量区展示趋势摘要。

#### Scenario: 展示趋势摘要
- **WHEN** 教师打开工作台且趋势 API 返回数据
- **THEN** 页面 SHALL 展示跨作业分析样本、诊断 eval 候选、课堂介入 eval 候选、still stuck 和 waiting followup 计数
- **AND** 页面 SHALL 展示趋势 summary

### Requirement: 趋势失败不能阻塞当前作业 AI 质量
教师工作台 MUST 将跨作业趋势的 loading/error 状态与当前作业 AI 质量状态隔离。

#### Scenario: 趋势读取失败
- **WHEN** 当前作业 AI 质量读取成功但跨作业趋势读取失败
- **THEN** 页面 SHALL 继续展示当前作业 AI 质量
- **AND** 页面 SHALL 在趋势区域展示独立错误信息

### Requirement: 趋势必须暴露课堂介入成效来源
教师工作台 SHALL 展示最近作业点的课堂介入成效指标，帮助老师定位 eval 候选来源。

#### Scenario: 展示作业点介入指标
- **WHEN** 趋势响应包含 assignment points
- **THEN** 页面 SHALL 展示若干作业点的 assignment title
- **AND** 每个作业点 SHALL 展示 intervention eval candidate、still stuck、waiting followup 或相关质量计数

### Requirement: 趋势必须暴露 AI 来源质量片段
教师工作台 SHALL 展示 source segment 摘要，帮助老师理解模型、提示词或运行状态来源的质量分布。

#### Scenario: 展示 source segment
- **WHEN** 趋势响应包含 source segments
- **THEN** 页面 SHALL 展示 source segment 的版本标签或状态
- **AND** 页面 SHALL 展示该 segment 的样本、校正、低置信或高泄题风险计数

### Requirement: 趋势 UI 必须通过前端类型检查
新增趋势 UI MUST 使用现有 TypeScript 类型并通过前端 typecheck。

#### Scenario: 执行类型检查
- **WHEN** 运行前端 typecheck
- **THEN** 趋势 UI SHALL 类型正确且不破坏现有教师端调用
