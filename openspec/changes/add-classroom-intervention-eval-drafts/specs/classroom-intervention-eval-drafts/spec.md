## ADDED Requirements

### Requirement: 系统必须导出课堂介入 eval 草稿
系统 SHALL 在诊断 eval fixture 草稿响应中新增课堂介入成效草稿，用于沉淀教师讲评建议和班级策略的执行成效。

#### Scenario: 导出教师讲评建议成效草稿
- **GIVEN** 教师对课堂复盘建议记录了采纳或调整反馈
- **AND** 反馈后存在后续提交证据
- **WHEN** 教师导出诊断 eval fixture 草稿
- **THEN** 响应 SHALL 包含一条 `class-review-intervention-draft`
- **AND** 草稿 SHALL 包含建议 key、教师反馈动作、impact 状态、后续提交、证据标签和 eval 目的

#### Scenario: 导出班级策略成效草稿
- **GIVEN** 教师对 `strategy:` 前缀的班级策略记录了采纳或调整反馈
- **AND** 反馈后存在后续提交证据
- **WHEN** 教师导出诊断 eval fixture 草稿
- **THEN** 响应 SHALL 包含一条 `class-strategy-intervention-draft`
- **AND** 草稿 SHALL 包含策略 key、策略焦点、impact 状态和后续提交证据

### Requirement: 草稿必须表达可回归评测期望
课堂介入 eval 草稿 SHALL 输出可用于人工沉淀 eval 的期望字段。

#### Scenario: 草稿包含评测期望
- **WHEN** 系统生成课堂介入 eval 草稿
- **THEN** 草稿 SHALL 包含 `mustMention`
- **AND** 草稿 SHALL 包含 `mustNotMention`
- **AND** 草稿 SHALL 包含 `expectedTeachingActions`
- **AND** 草稿 SHALL 包含 `quality.evalPurpose`

### Requirement: 无教师反馈的策略不得导出为成效草稿
系统 SHALL 只导出已被教师处理的课堂介入成效草稿。

#### Scenario: 无反馈不导出
- **GIVEN** 作业中存在 AI 课堂建议或班级策略
- **AND** 教师尚未记录采纳、调整或忽略
- **WHEN** 教师导出诊断 eval fixture 草稿
- **THEN** 响应中的课堂介入草稿列表 SHALL 不包含该建议或策略

### Requirement: 教师端必须展示课堂介入草稿数量
教师工作台 SHALL 在 fixture 草稿预览中展示课堂介入成效草稿数量和草稿 JSON。

#### Scenario: 预览课堂介入草稿
- **GIVEN** 后端响应包含课堂介入成效草稿
- **WHEN** 教师点击预览草稿
- **THEN** 页面 SHALL 显示课堂介入草稿数量
- **AND** JSON 预览 SHALL 包含课堂介入草稿内容

### Requirement: 课堂介入 eval 草稿必须可验证
课堂介入 eval 草稿 SHALL 有自动化测试覆盖导出内容、策略草稿、仍卡同类问题和前端类型。

#### Scenario: 执行验证
- **WHEN** 运行相关后端测试、OpenSpec 校验、后端编译和前端类型检查
- **THEN** 检查 SHALL 通过
