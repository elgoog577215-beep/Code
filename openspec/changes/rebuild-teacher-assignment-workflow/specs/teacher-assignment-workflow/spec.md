## ADDED Requirements

### Requirement: 教师端必须以作业管理为首页主线

教师端作业中心 SHALL 作为作业管理首页展示，不得把 AI/Coach、教师校正或复杂诊断内容放在首屏主流程。

#### Scenario: 打开作业中心
- **WHEN** 教师打开 `/app/teacher`
- **THEN** 页面 SHALL 显示作业列表、状态筛选、核心指标和新建作业主按钮
- **AND** 每条作业 SHALL 只展示作业名、班级、题数、状态、参与、提交、通过率、需关注和进入作业动作
- **AND** 页面 SHALL 不展示邀请码、AI/Coach 细节或教师校正表单

### Requirement: 新建作业必须使用三段式创建流程

新建作业页 SHALL 以基本信息、选择题目、确认发布三段组织创建流程，并复用现有创建接口。

#### Scenario: 创建作业
- **WHEN** 教师打开 `/app/teacher/assignment/new`
- **THEN** 页面 SHALL 依次展示基本信息、选择题目和确认发布三个区域
- **AND** 题目区域 SHALL 提供关键字搜索、难度筛选和已选题目摘要
- **AND** 提交作业 SHALL 使用现有 `title / description / classGroupId / hintPolicy / status / problemIds` payload

### Requirement: 作业详情必须使用标签式结构

作业详情页 SHALL 使用概览、学生、题目、诊断四个标签组织内容，默认显示概览标签。

#### Scenario: 查看作业详情默认页
- **WHEN** 教师打开 `/app/teacher/assignment/:assignmentId`
- **THEN** 页面 SHALL 默认停留在概览标签
- **AND** 概览 SHALL 回答完成情况、需关注学生和讲评优先题目
- **AND** 学生、题目和诊断内容 SHALL 通过标签切换进入

### Requirement: AI 和教师校正必须进入二级诊断区域

AI/Coach 信号、错因证据和教师校正 SHALL 位于诊断标签内，不得抢占作业中心或作业详情概览首屏。

#### Scenario: 查看诊断内容
- **WHEN** 教师在作业详情点击诊断标签
- **THEN** 页面 SHALL 显示 AI/Coach 摘要、教师校正入口和错因相关证据
- **AND** 教师 SHALL 能从诊断区域发起错因校正

### Requirement: 教师作业工作流必须保持现有接口兼容

教师端作业工作流重构 SHALL 保持现有路由、接口、共享类型和数据库兼容。

#### Scenario: 执行前端验收
- **WHEN** 运行前端类型检查和浏览器 smoke
- **THEN** `/app/teacher`、`/app/teacher/assignment/new` 和 `/app/teacher/assignment/:assignmentId` SHALL 通过验收
- **AND** 验收 SHALL 不要求任何后端 API、共享类型或数据库迁移变化
