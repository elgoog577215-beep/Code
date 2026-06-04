## ADDED Requirements

### Requirement: 学生端与教师端必须使用固定网址分离

系统 SHALL 将 `/app/student` 作为学生端主入口，将 `/app/teacher` 作为教师端主入口。正式使用时 SHALL 向学生和老师分别提供对应网址，不要求用户先经过角色选择首页。`/app` SHALL 作为兼容路径进入学生端。

#### Scenario: 用户访问兼容首页

- **WHEN** 用户打开 `/app`
- **THEN** 系统跳转或进入 `/app/student`

#### Scenario: 老师访问教师网址

- **WHEN** 老师打开 `/app/teacher`
- **THEN** 系统展示教师端工作台
- **THEN** 页面不要求老师先选择角色

#### Scenario: 学生端导航不混入教师任务

- **WHEN** 用户位于 `/app/student`、`/app/student/problems`、`/app/student/assignments/:assignmentId` 或 `/app/problem/:problemId`
- **THEN** 顶部导航 SHALL 优先展示学生首页、公共题库和我的作业
- **THEN** 教师管理入口 SHALL 不作为学生主导航项出现

### Requirement: 学生未登录时必须可以使用公共题库

学生未登录时，系统 SHALL 允许浏览公共题库并进入题目练习。公共题库 SHALL 不要求邀请码或班级身份。

#### Scenario: 未登录学生进入学生端

- **WHEN** 未登录学生打开 `/app/student`
- **THEN** 页面展示公共题库入口
- **THEN** 页面展示班级登录入口
- **THEN** 页面不展示老师作业列表为可进入状态

#### Scenario: 未登录学生进入公共题库

- **WHEN** 未登录学生打开 `/app/student/problems`
- **THEN** 系统 SHALL 兼容进入 `/app/student/assignments/public`
- **THEN** 页面展示公共题库做题工作台
- **THEN** 提交请求 SHALL 不强制包含 `assignmentId`

### Requirement: 学生登录必须基于班级和姓名

系统 SHALL 提供学生登录页面，允许学生选择或填写班级，并输入姓名和可选学号/座号。登录成功后，系统 SHALL 保存当前学生画像，并在学生端展示该学生所属班级。

#### Scenario: 学生使用班级和姓名登录

- **WHEN** 学生在 `/app/student/login` 提交班级、姓名和可选学号
- **THEN** 后端返回学生画像
- **THEN** 前端保存当前学生画像
- **THEN** 学生被带回 `/app/student`

#### Scenario: 学生登录信息不完整

- **WHEN** 学生提交登录表单但没有选择或填写班级、姓名
- **THEN** 系统 SHALL 给出简短错误提示
- **THEN** 系统 SHALL 不进入老师作业列表

### Requirement: 登录学生只能看到本班可见作业

登录学生的“我的作业” SHALL 只展示其 `classGroupId` 对应班级下的可见作业。作业详情页 SHALL 展示作业包含的多道题、完成状态和下一题入口。

#### Scenario: 登录学生进入学生首页

- **WHEN** 登录学生打开 `/app/student`
- **THEN** 页面展示当前学生身份
- **THEN** 页面 SHALL 将公共题库作为第一份公开作业展示
- **THEN** 页面展示该学生所属班级的作业列表

#### Scenario: 登录学生进入作业详情

- **WHEN** 登录学生打开 `/app/student/assignments/:assignmentId`
- **THEN** 系统 SHALL 自动选择下一道未完成题
- **THEN** 系统 SHALL 进入 `/app/student/assignments/:assignmentId/problems/:problemId`
- **THEN** 页面 SHALL 展示左侧题目列表、中间题目内容、右侧代码编辑

#### Scenario: 学生尝试访问不可见作业

- **WHEN** 登录学生打开不属于本班或不可见的作业详情
- **THEN** 页面 SHALL 显示不可进入状态
- **THEN** 页面 SHALL 提供返回我的作业的入口

### Requirement: 学生做题工作台必须采用 PTA 式三栏主流程

公共题库和老师作业 SHALL 复用同一套做题工作台。工作台 SHALL 将题目列表、题面和代码编辑分开，不在主页面常驻提交反馈栏。

#### Scenario: 学生进入公共题库公开作业

- **WHEN** 学生打开 `/app/student/assignments/public`
- **THEN** 系统 SHALL 自动进入第一道公共题
- **THEN** 页面 SHALL 显示公共题库题目列表
- **THEN** 页面 SHALL 显示当前题面和代码编辑区

#### Scenario: 学生从旧题库链接进入

- **WHEN** 用户打开 `/app/problems` 或 `/app/student/problems`
- **THEN** 系统 SHALL 跳转到 `/app/student/assignments/public`

#### Scenario: 学生提交代码

- **WHEN** 学生在工作台提交代码
- **THEN** 页面 SHALL 打开提交结果弹窗
- **THEN** 弹窗 SHALL 展示测试点情况、错误提示和优化提示
- **THEN** Coach 下一问 SHALL 位于弹窗底部
- **THEN** 关闭弹窗后页面 SHALL 只保留查看上次结果的入口

### Requirement: 教师端必须承担班级、题目和作业管理

教师端 SHALL 保持课堂工作台、班级/名单管理、题目编辑和作业布置能力，并在导航和入口上与学生端分离。

#### Scenario: 教师进入教师端

- **WHEN** 用户打开 `/app/teacher`
- **THEN** 页面展示课堂作业选择和课堂过程
- **THEN** 页面提供进入管理页和题目编辑页的入口

#### Scenario: 教师布置作业

- **WHEN** 教师在教师端创建或更新作业
- **THEN** 作业 SHALL 可以绑定班级和多道题
- **THEN** 绑定班级的活跃作业 SHALL 出现在该班学生的“我的作业”中

### Requirement: 旧邀请码链路必须保留兼容但不得作为主入口

系统 SHALL 保留旧邀请码解析和兼容路由，避免历史链接失效。学生主入口和顶部导航 SHALL 不再以邀请码作为主要任务名。

#### Scenario: 用户访问旧学生链接

- **WHEN** 用户打开 `/student` 或 `/student.html`
- **THEN** 系统 SHALL 进入新的学生端主链路

#### Scenario: 用户通过邀请码参数进入

- **WHEN** 用户打开带 `code` 参数的学生端链接
- **THEN** 系统 MAY 继续尝试解析邀请码
- **THEN** 页面 SHALL 仍提供班级登录和公共题库入口
