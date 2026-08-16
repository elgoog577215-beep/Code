## Purpose

保证每位教师只能访问自己拥有的教学数据，即使请求者猜中其他教师资源的数据库 ID 也不能越权读取或修改。

## ADDED Requirements

### Requirement: 教学数据必须归属于教师
班级和作业 SHALL 具有不可为空的教师所有者，学生 SHALL 通过班级继承所有者，历史无主数据 SHALL 迁移给 bootstrap 管理员。

#### Scenario: 教师创建班级或作业
- **WHEN** 已认证教师创建教学资源
- **THEN** 系统 SHALL 从当前会话写入所有者
- **AND** 系统 MUST NOT 接受客户端指定所有者

### Requirement: 教师查询与按 ID 访问必须校验所有权
教师列表查询 SHALL 按当前教师过滤，按 ID 读取、修改、分析或导出 SHALL 在服务层重新校验资源所有权。

#### Scenario: 教师猜测其他教师资源 ID
- **WHEN** 教师请求不属于自己的班级、学生、作业、提交或分析 ID
- **THEN** 系统 SHALL 返回 403 或 404
- **AND** 响应 MUST NOT 泄漏资源内容或所有者信息

### Requirement: 管理员教学工作台不得获得全局旁路
管理员在 `/api/admin/**` 可执行治理动作，但进入普通教师工作台后 SHALL 只看见自己拥有的教学数据。

#### Scenario: 管理员查看教师班级列表
- **WHEN** 管理员通过普通教师班级接口查询
- **THEN** 系统 SHALL 只返回管理员拥有的班级
