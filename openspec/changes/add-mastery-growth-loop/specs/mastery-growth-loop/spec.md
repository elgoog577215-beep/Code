## ADDED Requirements

### Requirement: 系统必须输出长期能力成长信号
系统 SHALL 在学生能力画像中输出结构化 `masteryGrowthSignal`，用于区分暂无证据、正在增长、迁移已验证、停滞、回退和需要螺旋复习。

#### Scenario: 学生近期形成能力增长
- **GIVEN** 同一学生近期存在从失败到通过的提交序列
- **AND** 近期失败数下降或通过数增加
- **WHEN** 读取学生能力画像
- **THEN** 系统 SHALL 输出 `masteryGrowthSignal.status` 为 `GROWING`
- **AND** 信号 SHALL 包含成长分数、近期提交数、通过数、失败数、证据引用和建议动作

#### Scenario: 学生跨题迁移已验证
- **GIVEN** 同一学生近期在多个题目上形成通过证据
- **AND** 这些证据覆盖同一能力点或同类诊断标签
- **WHEN** 读取学生能力画像
- **THEN** 系统 SHALL 输出 `masteryGrowthSignal.status` 为 `TRANSFER_CONFIRMED`
- **AND** 信号 SHALL 包含跨题证据数和主能力点

#### Scenario: 学生长期停滞或需要螺旋复习
- **GIVEN** 同一学生近期多次失败
- **AND** 同一能力点或细分错因跨多题重复出现
- **WHEN** 读取学生能力画像
- **THEN** 系统 SHALL 输出 `masteryGrowthSignal.status` 为 `PLATEAU` 或 `SPIRAL_REVIEW_NEEDED`
- **AND** 系统 SHALL NOT 将其标记为已增长

### Requirement: 学习轨迹必须展示当前作业成长状态
学习轨迹 SHALL 输出当前作业范围内的 `masteryGrowthSignal`，用于解释当前下一步是否应复盘、对比回退或做迁移验证。

#### Scenario: 当前作业出现回退
- **GIVEN** 学生在当前作业中曾经通过过题目
- **AND** 后续提交又出现连续失败或同类错误复现
- **WHEN** 读取学生作业轨迹
- **THEN** 系统 SHALL 返回当前作业的 `masteryGrowthSignal`
- **AND** `nextStep` SHALL 能在回退时优先提示对比上次通过与当前失败差异

### Requirement: 推荐系统必须消费长期成长信号
推荐系统 SHALL 在成长停滞、回退或需要螺旋复习时生成对应学习推荐，并保持现有推荐字段兼容。

#### Scenario: 停滞时生成最小复盘推荐
- **GIVEN** 学生存在 `PLATEAU` 的长期成长信号
- **WHEN** 获取学生推荐
- **THEN** 推荐列表 SHALL 包含 `MASTERY_PLATEAU_REPAIR` 策略
- **AND** 推荐 SHALL 包含学习假设、预期完成信号、证据标签和 fallback 动作

#### Scenario: 需要螺旋复习时生成跨题复盘推荐
- **GIVEN** 学生存在 `SPIRAL_REVIEW_NEEDED` 的长期成长信号
- **WHEN** 获取学生推荐
- **THEN** 推荐列表 SHALL 包含 `MASTERY_SPIRAL_REVIEW` 策略
- **AND** 推荐 SHALL 标记较高风险或教师关注

### Requirement: 教师端必须可见成长风险
教师工作台 SHALL 展示当前作业中成长停滞、回退或需要螺旋复习的学生数量、班级摘要和学生行信号。

#### Scenario: 教师查看成长风险学生
- **GIVEN** 当前作业有学生出现 `PLATEAU`、`REGRESSION` 或 `SPIRAL_REVIEW_NEEDED`
- **WHEN** 教师读取作业概览
- **THEN** 系统 SHALL 返回成长风险学生数和班级摘要
- **AND** 对应学生行 SHALL 包含 `masteryGrowthSignal`

### Requirement: AI 质量概览必须包含长期成长闭环维度
AI 质量概览 SHALL 增加 `MASTERY_GROWTH_LOOP` 维度，用于评估系统是否能把局部诊断沉淀为长期成长判断。

#### Scenario: 成长风险进入质量维度
- **GIVEN** 当前作业存在多个 `PLATEAU`、`REGRESSION` 或 `SPIRAL_REVIEW_NEEDED` 信号
- **WHEN** 教师读取 AI 质量概览
- **THEN** 质量维度 SHALL 包含 `MASTERY_GROWTH_LOOP`
- **AND** 该维度 SHALL 给出状态、分数、摘要、证据引用和推荐改进动作

### Requirement: 长期成长闭环必须可验证
长期能力成长闭环 SHALL 有后端测试和前端类型验证，覆盖主要状态和推荐消费场景。

#### Scenario: 执行验证
- **WHEN** 运行相关后端测试和前端类型检查
- **THEN** 检查 SHALL 通过
- **AND** 测试 SHALL 覆盖增长、迁移已验证、停滞、回退、螺旋复习、推荐消费、教师概览和 AI 质量维度
