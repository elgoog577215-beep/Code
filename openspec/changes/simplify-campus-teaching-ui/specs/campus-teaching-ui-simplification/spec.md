## ADDED Requirements

### Requirement: 学生端必须突出当前作业动作

学生端 SHALL 将当前作业与题目列表作为身份确认后的主要内容，并将身份详情与提交统计作为辅助信息。

#### Scenario: 已确认身份后进入作业
- **GIVEN** 学生已经确认身份
- **WHEN** 打开 `/app/student`
- **THEN** 页面 SHALL 优先展示当前作业题目列表
- **AND** 题目项 SHALL 包含题号、标题、难度或状态、开始或继续按钮
- **AND** 学生身份 SHALL 不作为独立主视觉卡片抢占题目列表权重

#### Scenario: 未确认身份进入作业
- **GIVEN** 学生已输入有效邀请码但未确认身份
- **WHEN** 打开作业页
- **THEN** 页面 SHALL 优先展示身份确认表单
- **AND** 题目入口 SHALL 明确提示先确认身份

### Requirement: 题目页必须突出一个主要处理项

题目页 SHALL 保持题目、代码、反馈三段式结构，并在提交后优先展示一个主要处理项。

#### Scenario: 提交后查看反馈
- **GIVEN** 学生提交代码并获得结果
- **WHEN** 查看 `/app/problem/:id`
- **THEN** 反馈区 SHALL 先展示处理项
- **AND** 作业记录与尝试记录 SHALL 默认作为二级折叠信息
- **AND** 下一问 SHALL 与处理项和 AI 反馈保持邻近

### Requirement: 教师端必须优先服务课堂介入

教师工作台 SHALL 将作业选择、课堂 KPI、需关注学生和高频问题放在 AI 质量详情之前。

#### Scenario: 教师打开课堂过程页
- **GIVEN** 当前作业有提交过程数据
- **WHEN** 教师打开 `/app/teacher`
- **THEN** 首屏 SHALL 展示课堂 KPI
- **AND** 需关注学生 SHALL 比 AI 质量详情更靠前
- **AND** 高频问题 SHALL 保留可讲评的错因与行动信息

#### Scenario: 查看 AI 质量
- **GIVEN** 当前作业有 AI 质量数据
- **WHEN** 教师查看课堂过程页
- **THEN** AI 质量 SHALL 默认展示摘要状态
- **AND** 质量维度、eval 候选和 fixture 草稿 SHALL 位于折叠详情中

### Requirement: 界面简化不得破坏现有能力

UI 简化 SHALL 保持现有路由、后端接口、AI 字段语义和浏览器 smoke 关键选择器兼容。

#### Scenario: 执行前端验证
- **WHEN** 运行前端类型检查、构建和浏览器 smoke
- **THEN** 检查 SHALL 通过
- **AND** 390px 移动端 SHALL 无横向溢出
- **AND** 主要触控控件 SHALL 不低于 44px
