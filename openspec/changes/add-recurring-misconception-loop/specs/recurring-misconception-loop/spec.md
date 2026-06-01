## ADDED Requirements

### Requirement: 系统必须输出长期复发误区信号
系统 SHALL 在学生能力画像中输出结构化 `recurringMisconceptionSignal`，用于区分暂无复发、需要观察、明确复发和需要升级干预。

#### Scenario: 跨题反复命中同一细粒度误区
- **GIVEN** 同一学生在至少两道题中命中同一细粒度错因
- **WHEN** 读取学生能力画像
- **THEN** 系统 SHALL 输出 `recurringMisconceptionSignal.status` 为 `RECURRING` 或 `ESCALATE`
- **AND** 信号 SHALL 包含细粒度错因、能力点、题目数、提交数、证据引用和建议动作

#### Scenario: 单题重复调试不误报为长期复发
- **GIVEN** 学生只在同一道题多次命中同一细粒度错因
- **WHEN** 读取学生能力画像
- **THEN** 系统 SHALL 输出 `recurringMisconceptionSignal.status` 为 `WATCH` 或 `NONE`
- **AND** 系统 SHALL NOT 将其标记为跨题长期复发

### Requirement: 推荐系统必须消费复发误区信号
推荐系统 SHALL 在长期复发误区明显时生成修复型推荐，并保持现有推荐字段兼容。

#### Scenario: 明确复发时优先生成修复推荐
- **GIVEN** 学生存在 `RECURRING` 或 `ESCALATE` 的复发误区信号
- **WHEN** 获取学生推荐
- **THEN** 推荐列表 SHALL 包含 `MISCONCEPTION_REPAIR` 或 `TEACHER_REVIEW_RECOMMENDED` 策略
- **AND** 推荐 SHALL 包含学习假设、预期完成信号、证据题目和 fallback 动作

#### Scenario: 没有复发时不挤占推荐槽
- **GIVEN** 学生没有跨题或跨作业复发误区
- **WHEN** 获取学生推荐
- **THEN** 系统 SHALL NOT 生成复发误区修复推荐

### Requirement: 教师端必须可见复发误区缺口
教师工作台 SHALL 展示当前作业中存在长期复发误区的学生数量、班级摘要和学生行信号。

#### Scenario: 教师查看课堂过程中的复发学生
- **GIVEN** 当前作业有学生的历史提交显示跨题或跨作业复发误区
- **WHEN** 教师读取作业概览
- **THEN** 系统 SHALL 返回复发误区学生数和班级摘要
- **AND** 对应学生行 SHALL 包含 `recurringMisconceptionSignal`

### Requirement: AI 质量概览必须包含复发误区闭环维度
AI 质量概览 SHALL 增加 `RECURRING_MISCONCEPTION_LOOP` 维度，用于评估系统是否发现长期复发误区并转化为复盘、推荐或教师介入动作。

#### Scenario: 复发误区未被转成动作
- **GIVEN** 当前作业存在多个 `RECURRING` 或 `ESCALATE` 信号
- **WHEN** 教师读取 AI 质量概览
- **THEN** 质量维度 SHALL 包含 `RECURRING_MISCONCEPTION_LOOP`
- **AND** 该维度 SHALL 给出状态、分数、摘要、证据引用和推荐改进动作

### Requirement: 复发误区能力必须可验证
复发误区闭环 SHALL 有后端测试和前端类型验证，覆盖主要状态和推荐消费场景。

#### Scenario: 执行验证
- **WHEN** 运行相关后端测试和前端类型检查
- **THEN** 检查 SHALL 通过
- **AND** 测试 SHALL 覆盖跨题复发、单题不误报、推荐消费、教师概览和 AI 质量维度
