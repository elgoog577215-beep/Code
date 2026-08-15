## MODIFIED Requirements

### Requirement: 教师额度必须来自所属学校分配
教师每个上海自然月的初始额度 SHALL 为 0，学校管理员 SHALL 从本校当月硬额度池中分配额度；外部模型调用 SHALL 同时校验学校、教师和业务幂等键。

#### Scenario: 已审核教师尚未获配额度
- **WHEN** 教师账号为 `ACTIVE` 但本月分配为 0
- **THEN** 外部模型调用 SHALL 返回 `QUOTA_EXHAUSTED`
- **AND** 代码评测 SHALL 继续正常执行
