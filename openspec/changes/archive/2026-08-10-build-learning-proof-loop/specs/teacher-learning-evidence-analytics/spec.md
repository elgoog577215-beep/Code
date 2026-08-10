# teacher-learning-evidence-analytics Specification Delta

## ADDED Requirements

### Requirement: 教师题目页必须展示可下钻的学习阶段人数
教师题目分析 SHALL 在现有问题分析主体上方展示曾未通过、已经修正、已经说明和换题已验证的去重学生人数；每个人数 SHALL 能筛选学生并继续下钻到提交、问题变化或保存说明。

#### Scenario: 学生失败后修正并说明
- **WHEN** 一名学生先未通过、后形成关键修改且保存修正说明
- **THEN** 该学生 SHALL 分别计入曾未通过、已经修正和已经说明
- **AND** 三个集合 SHALL 独立去重且允许重叠

#### Scenario: 点击学习阶段
- **WHEN** 教师点击已经修正、已经说明或换题已验证
- **THEN** 现有学生列表 SHALL 只展示该阶段学生
- **AND** 打开学生 SHALL 进入相同学习证明和成长证据

#### Scenario: 同一学生多次提交
- **WHEN** 同一学生在当前题存在多次提交或多条事件
- **THEN** 教师阶段人数 SHALL 只计一名学生
- **AND** 系统 MUST NOT 用尝试次数放大学生人数

### Requirement: 学习阶段状态不得替代现有问题分析
教师题目页 SHALL 保留遇到过、反复出现和后来解决的问题分析与学生证据列表；学习阶段只作为紧凑筛选，不得新增独立概览页、装饰图表或抽象能力名称。

#### Scenario: 教师选择规范问题
- **WHEN** 教师点击一个具体问题
- **THEN** 页面 SHALL 恢复现有问题学生筛选
- **AND** 学习阶段筛选 SHALL 被清除以避免两个口径混合
