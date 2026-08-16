# teacher-ai-quota-metering Specification

## Purpose
在平台统一持有模型密钥的前提下，为课堂 AI 调用提供按教师隔离、幂等、并发安全且可审计的月度免费额度。
## Requirements
### Requirement: 外部模型调用必须经过统一网关
业务链路 SHALL 通过统一 AI Provider 网关调用外部模型，并 SHALL 携带教师、学生、作业、提交、用途和幂等键上下文。

#### Scenario: 课堂 AI 业务动作
- **WHEN** 学生在有效教师作业中请求 AI 反馈或 Coach 交互
- **THEN** 网关 SHALL 将该动作关联到作业所有者和唯一幂等键

### Requirement: 教师必须获得自然月额度
教师每个上海自然月的初始额度 SHALL 为 0，学校管理员 SHALL 从本校当月硬额度池中分配额度；外部模型调用 SHALL 同时校验学校、教师和业务幂等键。

#### Scenario: 已审核教师尚未获配额度
- **WHEN** 教师账号为 `ACTIVE` 但本月分配为 0
- **THEN** 外部模型调用 SHALL 返回 `QUOTA_EXHAUSTED`
- **AND** 代码评测 SHALL 继续正常执行

#### Scenario: 教师查看本月用量
- **WHEN** 教师查询当前额度
- **THEN** 系统 SHALL 返回已分配、已用、预留、剩余和下次重置时间

### Requirement: 额度扣减必须幂等且成功后计费
一次学生反馈或 Coach 业务动作 SHALL 计 1 个额度；供应商重试和故障转移 SHALL 共用幂等键，只有至少一次外部调用成功后才扣减。

#### Scenario: 供应商失败后重试成功
- **WHEN** 同一业务动作发生多次供应商尝试且最终成功
- **THEN** 系统 SHALL 记录所有尝试
- **AND** 教师已用额度 SHALL 只增加 1

#### Scenario: 所有供应商尝试失败
- **WHEN** 同一业务动作没有外部调用成功
- **THEN** 系统 SHALL 记录失败原因
- **AND** 教师已用额度 SHALL 不增加

### Requirement: 额度耗尽必须安全降级
额度不足时系统 MUST NOT 发起外部模型调用，并 SHALL 返回 `QUOTA_EXHAUSTED`，同时保留代码评测和本地基础提示。

#### Scenario: 额度耗尽后提交代码
- **WHEN** 学生教师额度为零且提交代码
- **THEN** 代码评测 SHALL 正常完成
- **AND** 外部 AI 调用数 SHALL 为零

### Requirement: 匿名公共练习不得消耗付费模型
匿名及公共题库练习 SHALL 只提供免费判题与本地基础提示，MUST NOT 调用外部模型。

#### Scenario: 匿名用户练习公共题
- **WHEN** 匿名用户提交公共题练习
- **THEN** 系统 SHALL 执行代码评测
- **AND** MUST NOT 创建收费额度事件
