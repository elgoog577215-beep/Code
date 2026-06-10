## ADDED Requirements

### Requirement: 教师端必须按屏幕尺寸切换工作台密度

教师端 SHALL 在手机、中屏和桌面宽屏下使用不同的布局密度，而不是所有尺寸都采用同一种纵向或横向平铺。

#### Scenario: 手机视口
- **WHEN** 浏览器宽度为 390px
- **THEN** `/app/teacher`、`/app/teacher/assignment/:id` 和 `/app/teacher-management` SHALL 使用单列主流程
- **AND** 页面 SHALL 不产生横向溢出
- **AND** 主要操作按钮 SHALL 不低于 44px

#### Scenario: 中屏视口
- **WHEN** 浏览器宽度为 820px
- **THEN** 教师端 SHALL 保留可读的两列布局用于比较型区域
- **AND** 表单、学生报告和任务卡 SHALL 不因横向压缩而遮挡文本或按钮

#### Scenario: 桌面视口
- **WHEN** 浏览器宽度为 1440px
- **THEN** 教师端 SHALL 使用更宽的工作台容器
- **AND** 管理任务、学生观察和题目表现 SHALL 尽量横向展开，减少无效纵向滚动

### Requirement: 教师管理页必须在桌面并排主任务

教师管理页 SHALL 根据屏幕宽度调整创建班级、导入名单和导入题目的任务平铺方式。

#### Scenario: 桌面查看教师管理
- **WHEN** 教师在 1440px 宽度打开 `/app/teacher-management`
- **THEN** 创建班级、导入名单和导入题目 SHALL 处于同一工作台网格
- **AND** 至少两个任务卡 SHALL 在同一横向行

#### Scenario: 手机查看教师管理
- **WHEN** 教师在 390px 宽度打开 `/app/teacher-management`
- **THEN** 三个任务 SHALL 按创建班级、导入名单、导入题目的顺序纵向展示
- **AND** 导入表单和操作按钮 SHALL 不横向溢出

### Requirement: 作业详情必须在桌面形成教学工作台

作业详情页 SHALL 在桌面宽度下把学生观察和题目表现并排展示，并在小屏保留顺序明确的单列流程。

#### Scenario: 桌面查看作业详情
- **WHEN** 教师在 1440px 宽度打开 `/app/teacher/assignment/:id`
- **THEN** 学生情况和作业题目 SHALL 不全部纵向堆叠
- **AND** 学生情况内部 SHALL 保留学生列表和学生报告的并排关系

#### Scenario: 手机查看作业详情
- **WHEN** 教师在 390px 宽度打开 `/app/teacher/assignment/:id`
- **THEN** 页面 SHALL 按整体统计、学生情况、作业题目、高级分析的顺序单列展示
- **AND** 学生卡片、学生报告和题目行 SHALL 不横向溢出

### Requirement: 响应式改造不得改变教师端接口和路由

响应式工作台改造 SHALL 保持教师端现有接口、路由和关键 smoke 选择器兼容。

#### Scenario: 执行前端验收
- **WHEN** 运行前端类型检查和浏览器 smoke
- **THEN** `/app/teacher`、`/app/teacher/assignment/:id`、`/app/teacher-management` 的关键选择器 SHALL 继续存在
- **AND** 验证 SHALL 不要求任何后端 API、共享类型或数据库迁移变化
