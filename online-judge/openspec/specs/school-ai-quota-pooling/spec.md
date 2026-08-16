# school-ai-quota-pooling Specification

## Purpose
以学校自然月硬额度池约束校内教师 AI 分配，使平台只管理学校总量、校管能够安全回收和重分，并在并发请求下避免超分与超扣。
## Requirements
### Requirement: 平台必须按月授予学校硬额度池
平台管理员 SHALL 能调整学校当月基础与追加额度，但学校总量 MUST NOT 小于已分配给教师的额度总和。

#### Scenario: 平台把学校额度降到已分配以下
- **WHEN** 学校已分配 800 次而平台尝试把总额改为 700 次
- **THEN** 系统 SHALL 返回 `SCHOOL_QUOTA_EXCEEDED`
- **AND** 原额度 SHALL 保持不变

### Requirement: 校管必须在学校池内分配教师额度
教师批准后当月额度 SHALL 为 0，校管 SHALL 能分配或收回本校教师未使用额度，分配总和 MUST NOT 超过学校总量。

#### Scenario: 两个并发分配争用最后额度
- **WHEN** 两个请求并发分配且合计超过学校剩余额度
- **THEN** 至多一个请求 SHALL 成功
- **AND** 数据库中的已分配总量 MUST NOT 超过学校总量

#### Scenario: 收回额度低于已用与预留
- **WHEN** 教师已用与预留合计 20 次而校管尝试设为 19 次
- **THEN** 系统 SHALL 返回 `QUOTA_BELOW_USED`

### Requirement: 学校用量必须可追溯
每个付费 AI 用量事件 SHALL 同时记录教师和学校，学校已消耗量 SHALL 由本校教师成功计费事件汇总。

#### Scenario: 校管更换
- **WHEN** 平台替换学校管理员账号
- **THEN** 学校额度、教师分配和历史用量 SHALL 保持不变
