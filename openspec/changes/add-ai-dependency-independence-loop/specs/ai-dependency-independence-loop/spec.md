## ADDED Requirements

### Requirement: 系统必须输出 AI 依赖度与自主推进信号
系统 SHALL 在学生能力画像中输出结构化 `aiDependencySignal`，用于区分暂无证据、独立推进、支架有效、支架过密、依赖风险和需要教师撤支架复盘。

#### Scenario: 学生在低支架下独立推进
- **GIVEN** 同一学生近期存在没有推荐点击或 Coach 追问牵引的提交
- **AND** 这些提交出现通过、错因改善或稳定自解释证据
- **WHEN** 读取学生能力画像
- **THEN** 系统 SHALL 输出 `aiDependencySignal.status` 为 `INDEPENDENT_PROGRESS`
- **AND** 信号 SHALL 包含独立提交数、独立通过数、自主性分数、证据引用和建议动作

#### Scenario: 学生需要过密 AI 支架
- **GIVEN** 同一学生近期频繁生成 Coach 追问或点击推荐
- **AND** 独立提交数量不足或支架后仍反复失败
- **WHEN** 读取学生能力画像
- **THEN** 系统 SHALL 输出 `aiDependencySignal.status` 为 `SCAFFOLD_DENSE`、`DEPENDENCY_RISK` 或 `TEACHER_FADE_REVIEW`
- **AND** 系统 SHALL NOT 将其标记为独立推进

### Requirement: 学习轨迹必须展示当前作业 AI 支架状态
学习轨迹 SHALL 输出当前作业范围内的 `aiDependencySignal`，用于解释当前下一步是否应继续支架、先独立尝试或需要教师撤支架。

#### Scenario: 当前作业出现支架过密
- **GIVEN** 学生在当前作业中多次使用 Coach 或推荐
- **AND** 当前作业缺少独立提交或支架后仍未改善
- **WHEN** 读取学生作业轨迹
- **THEN** 系统 SHALL 返回当前作业的 `aiDependencySignal`
- **AND** `nextStep` SHALL 能在支架过密时优先提示先做一次独立最小尝试

### Requirement: 推荐系统必须消费 AI 依赖度信号
推荐系统 SHALL 在 AI 支架过密或依赖风险时生成独立尝试或教师撤支架复盘推荐，并保持现有推荐字段兼容。

#### Scenario: 支架过密时生成独立尝试推荐
- **GIVEN** 学生存在 `SCAFFOLD_DENSE` 或 `DEPENDENCY_RISK` 的 AI 依赖信号
- **WHEN** 获取学生推荐
- **THEN** 推荐列表 SHALL 包含 `INDEPENDENT_ATTEMPT` 策略
- **AND** 推荐 SHALL 包含学习假设、预期完成信号、证据引用和 fallback 动作

#### Scenario: 长期依赖时建议教师撤支架复盘
- **GIVEN** 学生存在 `TEACHER_FADE_REVIEW` 的 AI 依赖信号
- **WHEN** 获取学生推荐
- **THEN** 推荐列表 SHALL 包含 `TEACHER_SCAFFOLD_FADE_REVIEW` 策略
- **AND** 推荐 SHALL 标记高风险或教师关注

### Requirement: 教师端必须可见 AI 依赖风险
教师工作台 SHALL 展示当前作业中 AI 支架过密或依赖风险学生数量、班级摘要和学生行信号。

#### Scenario: 教师查看 AI 依赖风险学生
- **GIVEN** 当前作业有学生出现支架过密、依赖风险或需要教师撤支架
- **WHEN** 教师读取作业概览
- **THEN** 系统 SHALL 返回 AI 依赖风险学生数和班级摘要
- **AND** 对应学生行 SHALL 包含 `aiDependencySignal`

### Requirement: AI 质量概览必须包含自主性闭环维度
AI 质量概览 SHALL 增加 `AI_DEPENDENCY_INDEPENDENCE_LOOP` 维度，用于评估系统是否发现过度支架、独立推进和支架退场需求。

#### Scenario: 依赖风险进入质量维度
- **GIVEN** 当前作业存在多个 `SCAFFOLD_DENSE`、`DEPENDENCY_RISK` 或 `TEACHER_FADE_REVIEW` 信号
- **WHEN** 教师读取 AI 质量概览
- **THEN** 质量维度 SHALL 包含 `AI_DEPENDENCY_INDEPENDENCE_LOOP`
- **AND** 该维度 SHALL 给出状态、分数、摘要、证据引用和推荐改进动作

### Requirement: AI 依赖度闭环必须可验证
AI 依赖度与自主推进闭环 SHALL 有后端测试和前端类型验证，覆盖主要状态和推荐消费场景。

#### Scenario: 执行验证
- **WHEN** 运行相关后端测试和前端类型检查
- **THEN** 检查 SHALL 通过
- **AND** 测试 SHALL 覆盖独立推进、支架有效、支架过密、依赖风险、推荐消费、教师概览和 AI 质量维度
