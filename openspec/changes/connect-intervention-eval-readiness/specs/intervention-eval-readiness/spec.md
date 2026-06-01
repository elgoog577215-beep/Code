## ADDED Requirements

### Requirement: 评测沉淀 readiness 必须统计课堂介入候选
系统 SHALL 在 `evalReadiness` 中统计教师讲评建议和班级教学策略成效形成的 eval 候选数量。

#### Scenario: 课堂介入成效可沉淀
- **GIVEN** 作业中存在教师讲评建议或班级策略反馈
- **AND** 后续提交成效状态为 `IMPROVED`、`SHIFTED` 或 `STILL_STUCK`
- **WHEN** 教师读取 AI 质量概览
- **THEN** `evalReadiness.interventionCandidateCount` SHALL 大于 0
- **AND** `evalReadiness.status` SHALL 为 `READY`
- **AND** evidence refs SHALL 引用课堂介入或班级策略 impact

### Requirement: 等待后续证据只能作为部分 readiness
系统 SHALL 区分已可沉淀的课堂介入成效和等待后续证据的介入反馈。

#### Scenario: 介入反馈等待后续提交
- **GIVEN** 教师已经采纳或调整课堂建议
- **AND** 尚无后续提交证据
- **WHEN** 教师读取 AI 质量概览
- **THEN** `evalReadiness.status` SHALL 为 `PARTIAL`
- **AND** summary SHALL 说明课堂介入还在等待后续证据

### Requirement: readiness 必须输出推荐动作
系统 SHALL 在 `evalReadiness` 中输出面向教师的评测沉淀推荐动作。

#### Scenario: 同时存在诊断和介入候选
- **GIVEN** 作业同时存在教师校正 eval candidate 和课堂介入 eval candidate
- **WHEN** 教师读取 AI 质量概览
- **THEN** `evalReadiness.recommendedAction` SHALL 建议同时沉淀诊断 fixture 和课堂介入 fixture

### Requirement: 教师端必须展示课堂介入 readiness
教师工作台 SHALL 在 AI 质量面板的评测沉淀区展示课堂介入候选数量和推荐动作。

#### Scenario: 展示介入候选数量
- **GIVEN** `evalReadiness.interventionCandidateCount` 大于 0
- **WHEN** 教师查看 AI 质量面板
- **THEN** 页面 SHALL 显示课堂介入候选数量
- **AND** 页面 SHALL 显示 `evalReadiness.recommendedAction`

### Requirement: intervention eval readiness 必须可验证
系统 SHALL 有自动化测试覆盖课堂介入候选影响 readiness、等待后续 partial、前端类型和验证命令。

#### Scenario: 执行验证
- **WHEN** 运行相关后端测试、OpenSpec 校验、后端编译和前端类型检查
- **THEN** 检查 SHALL 通过
