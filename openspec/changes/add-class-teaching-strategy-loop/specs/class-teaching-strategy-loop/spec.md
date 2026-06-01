## ADDED Requirements

### Requirement: 系统必须输出结构化班级教学策略信号
系统 SHALL 输出 `classTeachingStrategySignal`，用于把班级范围内的学生风险、错因分布和讲评建议转成一个当前最高优先级课堂策略。

#### Scenario: 多个学生集中在同一错因
- **GIVEN** 当前作业中同一细粒度错因或能力点影响多个学生
- **WHEN** 教师读取作业概览
- **THEN** 响应 SHALL 包含 `classTeachingStrategySignal`
- **AND** 信号 SHALL 输出策略状态、策略类型、聚焦能力点或错因、影响学生数、风险等级、教师动作、证据引用和来源信号

#### Scenario: 没有足够班级证据
- **GIVEN** 当前作业没有学生提交或没有可用诊断
- **WHEN** 教师读取作业概览
- **THEN** `classTeachingStrategySignal.status` SHALL 为 `NO_SIGNAL` 或 `WATCH`
- **AND** 系统 SHALL NOT 输出误导性的全班讲评或小组复盘策略

### Requirement: 班级策略必须包含可执行课堂行动
班级教学策略 SHALL 包含教师可执行动作、分组计划和退出题验证，帮助教师把 AI 诊断转成课堂组织。

#### Scenario: 生成全班小讲评策略
- **GIVEN** 班级高频问题达到全班讲评阈值
- **WHEN** 系统生成班级教学策略
- **THEN** 策略 SHALL 推荐 `WHOLE_CLASS_MINI_LESSON`
- **AND** 策略 SHALL 包含一个不泄露答案的讲评动作
- **AND** 策略 SHALL 包含课末退出题或检查任务

#### Scenario: 生成小组复盘策略
- **GIVEN** 少数学生存在复发误区、成长停滞或高风险教学动作
- **WHEN** 系统生成班级教学策略
- **THEN** 策略 SHALL 推荐 `SMALL_GROUP_REVIEW`
- **AND** 策略 SHALL 返回包含学生标识、学生名称、分组焦点和行动建议的分组计划

### Requirement: 班级策略必须复用学生级教学动作和学习状态
班级教学策略 SHALL 消费学生级 `teachingActionDecision`、复发误区、自解释、AI 依赖和长期成长信号，而不是只统计提交错误数量。

#### Scenario: 学生级高风险触发小组策略
- **GIVEN** 多名学生的 `teachingActionDecision` 标记为教师关注或高风险
- **WHEN** 系统生成班级教学策略
- **THEN** 策略 SHALL 将这些学生纳入分组计划
- **AND** 策略 SHALL 在 `sourceSignals` 中引用学生级教学动作或学习状态

### Requirement: 教师端必须展示班级教学策略
教师工作台 SHALL 展示 `classTeachingStrategySignal` 的核心信息，使老师能在查看作业概览时直接看到本节课建议。

#### Scenario: 教师查看作业概览
- **GIVEN** 作业概览返回可执行班级教学策略
- **WHEN** 教师打开工作台
- **THEN** 页面 SHALL 展示策略标题、状态、聚焦点、教师动作、退出题和分组数量
- **AND** 页面 SHALL 保留现有 KPI、讲评建议和学生过程列表

### Requirement: AI 质量概览必须包含班级教学策略闭环维度
AI 质量概览 SHALL 增加 `CLASS_TEACHING_STRATEGY_LOOP` 维度，用于评估班级风险是否已经转成可执行课堂策略。

#### Scenario: 班级风险已转成策略
- **GIVEN** 当前作业存在多个学生级风险信号
- **AND** 系统生成了带证据、教师动作和退出题的班级教学策略
- **WHEN** 教师读取 AI 质量概览
- **THEN** 质量维度 SHALL 包含 `CLASS_TEACHING_STRATEGY_LOOP`
- **AND** 该维度 SHALL 给出状态、分数、摘要、证据引用和推荐改进动作

#### Scenario: 班级风险没有可执行策略
- **GIVEN** 当前作业存在多个高风险学生级信号
- **AND** 班级教学策略缺少教师动作、分组计划或证据引用
- **WHEN** 教师读取 AI 质量概览
- **THEN** `CLASS_TEACHING_STRATEGY_LOOP` SHALL 标记为 `ACTION_NEEDED` 或 `WATCH`
- **AND** 建议动作 SHALL 要求补齐班级策略证据或课堂验证任务

### Requirement: 班级教学策略闭环必须可验证
班级教学策略闭环 SHALL 有自动化测试覆盖策略选择、教师概览接入、AI 质量维度和前端类型。

#### Scenario: 执行验证
- **WHEN** 运行相关后端测试、OpenSpec 校验和前端类型检查
- **THEN** 检查 SHALL 通过
- **AND** 测试 SHALL 覆盖全班小讲评、小组复盘、证据不足兼容、教师概览返回和 AI 质量维度
