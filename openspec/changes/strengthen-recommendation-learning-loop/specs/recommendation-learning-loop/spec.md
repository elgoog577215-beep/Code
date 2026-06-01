## ADDED Requirements

### Requirement: 推荐项必须包含学习假设和完成信号

系统 SHALL 为每条学生推荐输出结构化学习假设、预期完成信号、推荐策略、风险等级和失败后的 fallback 动作。

#### Scenario: 生成重做推荐

- **WHEN** 学生存在未通过的历史题目且系统生成 `REDO` 推荐
- **THEN** 推荐项 SHALL 包含 `learningHypothesis`
- **AND** 推荐项 SHALL 包含 `expectedCompletionSignal`
- **AND** 推荐项 SHALL 包含 `strategy`
- **AND** 推荐项 SHALL 包含 `riskLevel`
- **AND** 推荐项 SHALL 包含 `fallbackAction`

#### Scenario: 生成复盘降级推荐

- **WHEN** 学生近期推荐后提交仍命中同类错因
- **THEN** 系统 SHALL 生成或提前一条复盘/降级策略推荐
- **AND** 该推荐 SHALL 明确要求补充最小样例、证据解释或教师介入前的可验证材料

### Requirement: 推荐事件必须保留学习闭环字段

系统 SHALL 在推荐曝光事件中保存推荐策略、学习假设、预期完成信号、fallback 动作和作业维度，并在点击、进入题目和提交事件中沿用这些字段。

#### Scenario: 记录推荐曝光

- **WHEN** 系统记录推荐曝光事件
- **THEN** 事件 SHALL 保存推荐策略
- **AND** 事件 SHALL 保存学习假设
- **AND** 事件 SHALL 保存预期完成信号
- **AND** 事件 SHALL 保存 fallback 动作
- **AND** 事件 SHALL 保存可推导的 `assignmentId`

#### Scenario: 记录后续提交

- **WHEN** 学生通过推荐 token 产生后续提交事件
- **THEN** 提交事件 SHALL 沿用曝光事件中的推荐策略和学习闭环字段
- **AND** 提交事件 SHALL 同时保存后续 verdict 与诊断标签
- **AND** 提交事件 SHALL 优先保存后续提交所属作业

### Requirement: 推荐效果必须输出行动型反馈

系统 SHALL 汇总推荐效果中的未完成学习信号、教师介入建议和策略级效果统计。

#### Scenario: 推荐后仍卡同类错因

- **WHEN** 推荐后提交仍命中推荐关注标签
- **THEN** 推荐效果概览 SHALL 增加未完成学习信号计数
- **AND** 推荐效果概览 SHALL 输出一条反馈信号，建议降级复盘、收窄验证或教师介入

#### Scenario: 推荐被点击但没有提交

- **WHEN** 推荐被点击或进入题目但没有后续提交
- **THEN** 推荐效果概览 SHALL 输出反馈信号，说明当前只能判断触达，不能判断学习效果

#### Scenario: 按策略聚合效果

- **WHEN** 推荐事件包含策略字段
- **THEN** 推荐效果概览 SHALL 按策略输出曝光、点击、提交、通过和同类错因统计

#### Scenario: 按作业过滤推荐效果

- **WHEN** 教师查看某个作业的 AI 质量概览
- **THEN** 系统 SHALL 仅使用该作业相关推荐事件计算推荐闭环质量信号
- **AND** 无作业归属的旧推荐事件 MUST NOT 污染该作业的推荐闭环维度
