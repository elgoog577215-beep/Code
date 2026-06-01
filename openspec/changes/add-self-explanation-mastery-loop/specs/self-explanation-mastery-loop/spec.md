## ADDED Requirements

### Requirement: 系统必须输出长期自解释能力信号
系统 SHALL 在学生能力画像中输出结构化 `selfExplanationMasterySignal`，用于区分暂无回答证据、正在形成、证据扎实、可迁移解释、需要 Coach 降粒度和需要教师关注。

#### Scenario: 学生持续给出可验证解释
- **GIVEN** 同一学生近期多次 Coach 回答包含最小样例、变量轨迹、输出对比、复杂度估算、反例或迁移解释
- **WHEN** 读取学生能力画像
- **THEN** 系统 SHALL 输出 `selfExplanationMasterySignal.status` 为 `EVIDENCE_GROUNDED` 或 `TRANSFER_READY`
- **AND** 信号 SHALL 包含证据完整度、可验证回答数、证据类型、证据引用和建议动作

#### Scenario: 学生持续空泛确认
- **GIVEN** 同一学生近期多次 Coach 回答只是表示知道、会改或试试，缺少可验证证据
- **WHEN** 读取学生能力画像
- **THEN** 系统 SHALL 输出 `selfExplanationMasterySignal.status` 为 `NEEDS_COACHING`
- **AND** 系统 SHALL NOT 将其标记为证据扎实或可迁移解释

### Requirement: 学习轨迹必须展示当前作业自解释状态
学习轨迹 SHALL 输出当前作业范围内的 `selfExplanationMasterySignal`，用于解释当前下一步是否应补证据、继续验证或进入迁移。

#### Scenario: 当前作业存在解释证据
- **GIVEN** 学生在当前作业 Coach 追问中给出可验证回答
- **WHEN** 读取学生作业轨迹
- **THEN** 系统 SHALL 返回当前作业的 `selfExplanationMasterySignal`
- **AND** `nextStep` SHALL 能在证据不足时优先提示补最小样例、变量轨迹或输出对比

### Requirement: 推荐系统必须消费自解释能力信号
推荐系统 SHALL 在自解释证据不足或存在安全风险时生成解释练习或教师复盘推荐，并保持现有推荐字段兼容。

#### Scenario: 解释证据不足时优先生成自解释练习
- **GIVEN** 学生存在 `NEEDS_COACHING` 或 `EMERGING` 的自解释信号
- **WHEN** 获取学生推荐
- **THEN** 推荐列表 SHALL 包含 `SELF_EXPLANATION_PRACTICE` 策略
- **AND** 推荐 SHALL 包含学习假设、预期完成信号、证据引用和 fallback 动作

#### Scenario: 安全风险或长期无回答时建议教师示范
- **GIVEN** 学生存在 `SAFETY_RISK` 或长期没有回答证据但已有多轮 Coach 追问
- **WHEN** 获取学生推荐
- **THEN** 推荐列表 SHALL 包含 `TEACHER_EXPLANATION_REVIEW` 策略
- **AND** 推荐 SHALL 标记高风险或教师关注

### Requirement: 教师端必须可见自解释能力缺口
教师工作台 SHALL 展示当前作业中自解释证据不足或有风险的学生数量、班级摘要和学生行信号。

#### Scenario: 教师查看自解释薄弱学生
- **GIVEN** 当前作业有学生 Coach 回答显示证据不足、持续空泛或安全风险
- **WHEN** 教师读取作业概览
- **THEN** 系统 SHALL 返回自解释薄弱学生数和班级摘要
- **AND** 对应学生行 SHALL 包含 `selfExplanationMasterySignal`

### Requirement: AI 质量概览必须包含自解释能力闭环维度
AI 质量概览 SHALL 增加 `SELF_EXPLANATION_MASTERY_LOOP` 维度，用于评估系统是否发现学生理解证据不足并转化为 Coach、推荐或教师动作。

#### Scenario: 自解释缺口未被转成动作
- **GIVEN** 当前作业存在多个 `NEEDS_COACHING` 或 `SAFETY_RISK` 信号
- **WHEN** 教师读取 AI 质量概览
- **THEN** 质量维度 SHALL 包含 `SELF_EXPLANATION_MASTERY_LOOP`
- **AND** 该维度 SHALL 给出状态、分数、摘要、证据引用和推荐改进动作

### Requirement: 自解释能力必须可验证
自解释能力闭环 SHALL 有后端测试和前端类型验证，覆盖主要状态和推荐消费场景。

#### Scenario: 执行验证
- **WHEN** 运行相关后端测试和前端类型检查
- **THEN** 检查 SHALL 通过
- **AND** 测试 SHALL 覆盖证据充足、证据不足、持续空泛、推荐消费、教师概览和 AI 质量维度
