## ADDED Requirements

### Requirement: 教师工作台必须展示推荐效果总览
教师工作台 SHALL 加载推荐效果总览，并在 AI 质量区域展示推荐学习闭环摘要。

#### Scenario: 展示推荐效果指标
- **WHEN** 推荐效果 API 返回数据
- **THEN** 页面 SHALL 展示曝光、点击、后续提交、后续通过、同类错因未解和教师介入建议计数
- **AND** 页面 SHALL 展示推荐效果 summary

### Requirement: 推荐效果失败不能阻塞其他 AI 质量信号
教师工作台 MUST 将推荐效果的 loading/error 状态与当前作业 AI 质量、跨作业趋势隔离。

#### Scenario: 推荐效果读取失败
- **WHEN** 当前作业 AI 质量读取成功但推荐效果读取失败
- **THEN** 页面 SHALL 继续展示当前作业 AI 质量和跨作业趋势
- **AND** 页面 SHALL 在推荐效果区域展示独立错误信息

### Requirement: 推荐效果必须暴露可行动反馈信号
教师工作台 SHALL 展示 recommendation feedback signals，帮助老师决定是否降低练习台阶或介入。

#### Scenario: 展示反馈信号
- **WHEN** 推荐效果响应包含 feedback signals
- **THEN** 页面 SHALL 展示 signal summary、severity、evidence count 和 recommended action

### Requirement: 推荐效果必须暴露策略或焦点片段
教师工作台 SHALL 展示推荐策略或焦点标签片段，帮助老师发现需要校准的推荐类型。

#### Scenario: 展示策略片段
- **WHEN** 推荐效果响应包含 byStrategy 或 focusTags
- **THEN** 页面 SHALL 展示若干 segment 的 label
- **AND** 每个 segment SHALL 展示点击、后续提交、通过或同类错因未解计数

### Requirement: 推荐效果 UI 必须通过前端验证
新增推荐效果 UI MUST 使用现有 TypeScript 类型并通过前端验证。

#### Scenario: 执行验证
- **WHEN** 运行 OpenSpec 严格校验、前端 typecheck、视觉检查和 diff 检查
- **THEN** 检查 SHALL 通过
