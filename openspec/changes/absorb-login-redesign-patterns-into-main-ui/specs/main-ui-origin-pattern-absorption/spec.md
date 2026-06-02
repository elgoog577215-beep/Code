## ADDED Requirements

### Requirement: 角色入口必须清楚区分主要路径
系统 SHALL 在 `/app` 提供清楚的角色与任务入口，用户能够进入学生端、教师端或公共题库。

#### Scenario: 访问主入口
- **WHEN** 用户访问 `/app`
- **THEN** 页面展示学生、教师和公共题库三个主要入口

#### Scenario: 入口路径兼容
- **WHEN** 用户点击入口中的学生、教师或公共题库
- **THEN** 系统导航到 `/app/student`、`/app/teacher` 或 `/app/problems`

### Requirement: 公共题库必须支持低噪音浏览
系统 SHALL 提供 `/app/problems` 公共题库页面，允许用户搜索题目并按难度筛选。

#### Scenario: 浏览题库
- **WHEN** 后端返回题库列表
- **THEN** 页面以紧凑卡片展示题号、标题、难度、摘要和进入按钮

#### Scenario: 搜索和筛选题目
- **WHEN** 用户输入关键词或选择难度
- **THEN** 页面只展示匹配的题目

#### Scenario: 题库为空或加载失败
- **WHEN** 题库接口返回空列表或加载失败
- **THEN** 页面展示空状态，不展示 mock 题目

### Requirement: 全局导航必须保留教学主流程
系统 SHALL 在全局导航中提供题库、学生、教师和邀请码快捷入口，同时保持当前页面主操作不被导航抢占。

#### Scenario: 导航访问
- **WHEN** 用户在任意主页面查看顶部导航
- **THEN** 可以看到题库、学生、教师和邀请码入口

#### Scenario: 邀请码快捷入口
- **WHEN** 用户选择邀请码入口
- **THEN** 系统将用户带到学生端身份确认与邀请码区域，或展示等价的轻量入口

### Requirement: 教师总览必须聚焦作业与课堂状态
系统 SHALL 让 `/app/teacher` 首屏优先展示作业选择、课堂 KPI、需关注学生和高频问题。

#### Scenario: 教师访问总览
- **WHEN** 教师访问 `/app/teacher`
- **THEN** 页面展示作业列表和当前作业课堂摘要

#### Scenario: 进入单作业详情
- **WHEN** 教师点击某个作业详情入口
- **THEN** 系统导航到 `/app/teacher/assignment/:id`

### Requirement: 单作业详情必须承载课堂操作
系统 SHALL 在 `/app/teacher/assignment/:id` 展示单作业邀请码、课堂 KPI、高频问题、学生列表和教师校正动作。

#### Scenario: 查看作业详情
- **WHEN** 教师访问有效作业详情页
- **THEN** 页面展示该作业标题、邀请码、KPI、高频问题和学生列表

#### Scenario: 校正学生错因
- **WHEN** 教师点击学生行的校正动作
- **THEN** 页面展示校正表单，并继续使用现有校正 API 保存

#### Scenario: 无效作业
- **WHEN** 教师访问不存在的作业详情
- **THEN** 页面展示空状态并提供返回教师总览的路径
