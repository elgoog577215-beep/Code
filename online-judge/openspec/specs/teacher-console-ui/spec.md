# teacher-console-ui Specification

## Purpose
TBD - created by archiving change redesign-teacher-console-ui. Update Purpose after archive.
## Requirements
### Requirement: 教师端必须使用统一侧栏工作台
教师端页面 SHALL 使用统一左侧栏表达教师主导航，并在右侧内容区展示当前页面任务。

#### Scenario: 教师进入任意教师页面
- **WHEN** 教师访问作业中心、班级学情、题库管理、AI 标准库、新建作业、作业详情或学生诊断页面
- **THEN** 页面 SHALL 显示同一套教师侧栏导航
- **AND** 当前模块 SHALL 具有清晰 active 状态

### Requirement: 作业路径必须按教学对象分层下钻
教师端作业路径 SHALL 明确区分作业列表、作业内题目列表、题目分析和学生提交诊断。

#### Scenario: 教师打开作业详情
- **WHEN** 路由只包含作业 ID
- **THEN** 页面 SHALL 以题目列表为主
- **AND** SHALL 展示每道题的提交、正确率、需关注和 AI 易错点摘要

#### Scenario: 教师打开题目详情
- **WHEN** 路由包含作业 ID 和题目 ID
- **THEN** 页面 SHALL 展示该题正确率、提交率、学生情况、高频易错点和讲评建议

#### Scenario: 教师打开学生提交诊断
- **WHEN** 路由包含作业 ID、题目 ID 和学生 ID
- **THEN** 页面 SHALL 展示代码证据、评测结果、AI 诊断、教师校正和给学生反馈入口

### Requirement: AI 信息必须作为教学辅助
教师端 SHALL 将 AI 信息展示为易错点、讲评建议、诊断校正和标准库治理辅助，不得在无提交或无证据时压过传统教学进度信息。

#### Scenario: 作业尚无学生提交
- **WHEN** 作业没有提交数据
- **THEN** 作业页 SHALL 优先展示题目列表、提交状态和学生入口动作
- **AND** AI 摘要 SHALL 使用等待提交后的辅助空状态

### Requirement: 班级学情必须跨作业展示学生状态
班级学情页面 SHALL 跨作业展示学生推进、优先关注学生、薄弱点和教学建议。

#### Scenario: 教师打开班级学情
- **WHEN** 存在多个作业和学生提交数据
- **THEN** 页面 SHALL 展示学生 x 作业矩阵
- **AND** SHALL 展示优先关注学生与班级薄弱点摘要

### Requirement: 学生端必须轻量协调视觉边界
学生端 SHALL 保持现有做题主体流程，同时在返回入口、按钮、边界、间距和表面层次上与教师端新视觉保持一致。

#### Scenario: 学生打开作业或题目页面
- **WHEN** 页面展示返回链接、主要按钮、面板或列表边界
- **THEN** 这些元素 SHALL 使用与新教师端一致的圆角、边界、按钮高度和清晰焦点状态
- **AND** 学生端做题、提交和 AI 反馈主体流程 SHALL 不被重排

