## MODIFIED Requirements

### Requirement: 教师端必须使用统一侧栏工作台
教师端页面 SHALL 使用统一左侧栏表达教师主导航，并在右侧内容区展示当前页面任务；工作台 SHALL 显示当前教师身份、账号状态和剩余额度，并 SHALL 只呈现该教师可访问的数据。

#### Scenario: 教师进入任意教师页面
- **WHEN** 已审核教师访问作业中心、班级学情、题库管理、AI 标准库、新建作业、作业详情或学生诊断页面
- **THEN** 页面 SHALL 显示同一套教师侧栏导航
- **AND** 当前模块 SHALL 具有清晰 active 状态
- **AND** 页面 SHALL 显示当前教师身份与本月 AI 剩余额度

#### Scenario: 未认证教师访问工作台
- **WHEN** 没有有效教师会话访问教师路由
- **THEN** 页面 SHALL 导向用户名密码登录与注册入口

## ADDED Requirements

### Requirement: 教师题库必须统一展示范围和版本
教师题库界面 SHALL 分组展示公共、共建和我的题库，并 SHALL 标明来源、范围、状态和版本。

#### Scenario: 教师浏览可见题目
- **WHEN** 教师打开题库管理页
- **THEN** 页面 SHALL 隐藏其他教师私有题
- **AND** 每道题 SHALL 显示可布置性和可编辑性

### Requirement: 管理员必须具有独立治理界面
管理员界面 SHALL 支持教师申请、账号状态、题目审核、额度调整和数据转交，且 SHALL 与普通教师教学工作台分离。

#### Scenario: 管理员处理待审核教师
- **WHEN** 管理员打开教师申请列表
- **THEN** 页面 SHALL 显示待审核申请及批准、拒绝操作
