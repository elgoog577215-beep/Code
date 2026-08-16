# teacher-data-isolation Specification

## Purpose
保证每位教师只能访问自己拥有的教学数据，即使请求者猜中其他教师资源的数据库 ID 也不能越权读取或修改。
## Requirements
### Requirement: 教学数据必须归属于教师
班级、作业和私有题目 SHALL 具有具体且不可为空的教师所有者，学生 SHALL 通过班级继承所有者；服务端 MUST 从教师会话写入所有者而不得接受客户端指定。

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
本校学校管理员 MAY 通过专用只读接口查看本校教学资源，但 MUST NOT 获得教师写权限；平台管理员 MUST NOT 获得普通教师和学生教学明细读取旁路。

#### Scenario: 管理员查看教师班级列表
- **WHEN** 任一管理员通过普通教师班级接口查询
- **THEN** 系统 SHALL 拒绝请求且 MUST NOT 返回教师教学数据

#### Scenario: 校管查看本校教师班级
- **WHEN** 学校管理员通过校管教学接口请求本校班级
- **THEN** 系统 SHALL 返回只读投影
- **AND** 任一写请求 SHALL 被拒绝

#### Scenario: 校管查看其他学校班级
- **WHEN** 学校管理员猜测其他学校班级 ID
- **THEN** 系统 SHALL 返回 403 或 404
