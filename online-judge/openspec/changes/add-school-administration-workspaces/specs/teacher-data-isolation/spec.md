## MODIFIED Requirements

### Requirement: 教学数据必须保持教师所有权并提供校内只读监督
班级、作业和私有题目 SHALL 继续归属于具体教师；本校学校管理员 MAY 通过专用只读接口查看这些资源，但 MUST NOT 获得教师写权限，平台管理员 MUST NOT 获得教学读取旁路。

#### Scenario: 校管查看本校教师班级
- **WHEN** 学校管理员通过校管教学接口请求本校班级
- **THEN** 系统 SHALL 返回只读投影
- **AND** 任一写请求 SHALL 被拒绝

#### Scenario: 校管查看其他学校班级
- **WHEN** 学校管理员猜测其他学校班级 ID
- **THEN** 系统 SHALL 返回 403 或 404
