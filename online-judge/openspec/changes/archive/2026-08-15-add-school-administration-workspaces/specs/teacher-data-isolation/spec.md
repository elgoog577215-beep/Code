## MODIFIED Requirements

### Requirement: 教学数据必须归属于教师
班级、作业和私有题目 SHALL 具有具体且不可为空的教师所有者，学生 SHALL 通过班级继承所有者；服务端 MUST 从教师会话写入所有者而不得接受客户端指定。

#### Scenario: 教师创建班级或作业
- **WHEN** 已认证教师创建教学资源
- **THEN** 系统 SHALL 从当前会话写入所有者
- **AND** 系统 MUST NOT 接受客户端指定所有者

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
