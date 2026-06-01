## ADDED Requirements

### Requirement: 班级概览必须汇总 Coach 回答质量
系统 SHALL 在作业概览中输出班级级 Coach 回答质量摘要。

#### Scenario: 输出班级 Coach 质量摘要
- **WHEN** 教师读取作业概览
- **THEN** 响应 SHALL 包含 `coachAnswerQualitySummary`
- **AND** 该字段 SHALL 包含 prompted、answered、verifiable、transferReady、evidenceInsufficient、safetyRisk 和 teacherAttention 计数

### Requirement: 汇总必须给出主导缺口和行动建议
系统 SHALL 基于学生最新 Coach 回答质量信号生成主导缺口、摘要和教师行动建议。

#### Scenario: 存在证据不足回答
- **GIVEN** 多个学生最新 Coach 回答为 `VAGUE_ACK` 或 `DIRECTION_ONLY`
- **WHEN** 教师读取作业概览
- **THEN** `coachAnswerQualitySummary.dominantGap` SHALL 表示证据不足
- **AND** summary/recommendedAction SHALL 提醒教师追问最小样例、输出对比或变量轨迹

### Requirement: 汇总必须暴露证据引用
系统 SHALL 为 Coach 回答质量摘要提供可追溯 evidence refs。

#### Scenario: 生成证据引用
- **GIVEN** 学生存在最新 Coach interaction
- **WHEN** 系统生成 `coachAnswerQualitySummary`
- **THEN** evidenceRefs SHALL 包含若干 submission 或 coach evidence ref

### Requirement: 教师工作台必须展示 Coach 回答质量摘要
教师工作台 SHALL 展示班级级 Coach 回答质量摘要，帮助老师判断是否继续追问、复盘或介入。

#### Scenario: 展示 Coach 质量卡片
- **WHEN** 作业概览包含 `coachAnswerQualitySummary`
- **THEN** 页面 SHALL 展示已追问、已回答、可验证、可迁移、证据不足、疑似越界和需关注计数
- **AND** 页面 SHALL 展示 summary 或 recommendedAction

### Requirement: Coach 回答质量汇总必须可验证
系统 MUST 提供后端测试和前端验证覆盖新增汇总字段与展示。

#### Scenario: 执行验证
- **WHEN** 运行相关后端测试、后端编译、前端 typecheck、视觉检查和 diff 检查
- **THEN** 检查 SHALL 通过
