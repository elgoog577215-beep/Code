## ADDED Requirements

### Requirement: 系统必须输出结构化教学动作决策
系统 SHALL 输出 `teachingActionDecision`，用于把多个 AI 教育信号编排成一个当前最高优先级教学动作。

#### Scenario: 多个风险信号同时出现
- **GIVEN** 同一学生同时存在成长停滞、AI 支架过密和自解释证据不足
- **WHEN** 系统生成教学动作决策
- **THEN** 决策 SHALL 输出最高优先级动作
- **AND** 决策 SHALL 包含动作类型、执行者、优先级、风险等级、推荐动作、fallback 动作、证据引用和来源信号

#### Scenario: 缺少明显风险信号
- **GIVEN** 学生没有明显复发误区、成长风险、AI 依赖风险或自解释风险
- **WHEN** 系统生成教学动作决策
- **THEN** 决策 SHALL 输出 `CONTINUE_DIAGNOSIS` 或低风险继续观察动作
- **AND** 系统 SHALL NOT 标记教师关注

### Requirement: 学习轨迹必须复用教学动作决策
学习轨迹 SHALL 输出当前作业范围内的 `teachingActionDecision`，并用该决策统一解释 `nextStep` 和 `attentionReason`。

#### Scenario: 当前作业需要教师复盘
- **GIVEN** 当前作业存在需要教师介入的复发误区、成长螺旋复习或 AI 长期依赖信号
- **WHEN** 读取学生作业轨迹
- **THEN** 响应 SHALL 包含 `teachingActionDecision`
- **AND** `nextStep` SHALL 使用该决策的推荐动作
- **AND** `attentionReason` SHALL 使用该决策的原因或摘要

### Requirement: 学生画像必须输出长期教学动作决策
学生能力画像 SHALL 输出跨作业范围内的 `teachingActionDecision`，用于说明长期画像的下一步教学动作。

#### Scenario: 长期画像出现成长回退
- **GIVEN** 学生长期能力画像存在 `REGRESSION` 或 `SPIRAL_REVIEW_NEEDED`
- **WHEN** 读取学生能力画像
- **THEN** 响应 SHALL 包含 `teachingActionDecision`
- **AND** 决策 SHALL 引用长期成长信号作为来源证据

### Requirement: 推荐系统必须消费教学动作决策
推荐系统 SHALL 优先消费 `teachingActionDecision`，生成与最高优先级动作一致的推荐，并避免重复插入同类高风险推荐。

#### Scenario: 编排决策要求教师复盘
- **GIVEN** 学生的 `teachingActionDecision.actionType` 为 `TEACHER_REVIEW` 或 `SPIRAL_REVIEW`
- **WHEN** 获取学生推荐
- **THEN** 推荐列表 SHALL 包含对应 `TEACHING_ACTION_*` 策略
- **AND** 该推荐 SHALL 包含来源信号、证据引用、学习假设和预期完成信号

### Requirement: 教师端必须可见教学动作决策
教师工作台 SHALL 展示当前作业中需要明确教学动作的学生数量、班级摘要和学生行决策。

#### Scenario: 教师查看需行动学生
- **GIVEN** 当前作业有学生的 `teachingActionDecision.needsTeacherAttention` 为 true 或风险等级为 HIGH
- **WHEN** 教师读取作业概览
- **THEN** 系统 SHALL 返回教学动作风险学生数和班级摘要
- **AND** 对应学生行 SHALL 包含 `teachingActionDecision`

### Requirement: AI 质量概览必须包含教学动作编排维度
AI 质量概览 SHALL 增加 `TEACHING_ACTION_ORCHESTRATION_LOOP` 维度，用于评估系统是否能把局部信号转成明确教学动作。

#### Scenario: 风险信号已转成动作
- **GIVEN** 当前作业存在多个高风险教育信号
- **WHEN** 教师读取 AI 质量概览
- **THEN** 质量维度 SHALL 包含 `TEACHING_ACTION_ORCHESTRATION_LOOP`
- **AND** 该维度 SHALL 给出状态、分数、摘要、证据引用和推荐改进动作

### Requirement: 教学动作编排闭环必须可验证
教学动作编排闭环 SHALL 有后端测试和前端类型验证，覆盖候选动作排序和主要消费入口。

#### Scenario: 执行验证
- **WHEN** 运行相关后端测试和前端类型检查
- **THEN** 检查 SHALL 通过
- **AND** 测试 SHALL 覆盖教师复盘、螺旋复习、回退修复、独立尝试、自解释练习、AC 后复盘、推荐消费、教师概览和 AI 质量维度
