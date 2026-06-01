## ADDED Requirements

### Requirement: 班级策略必须提供可反馈标识
系统 SHALL 为 `classTeachingStrategySignal` 输出稳定 `strategyKey`，用于教师对班级策略本身记录采纳、调整或忽略。

#### Scenario: 策略可被教师反馈
- **GIVEN** 作业概览生成了可执行班级教学策略
- **WHEN** 教师读取作业概览
- **THEN** `classTeachingStrategySignal` SHALL 包含非空 `strategyKey`
- **AND** `strategyKey` SHALL 能稳定区分当前作业、策略状态和策略焦点

### Requirement: 教师端必须能记录班级策略反馈
教师工作台 SHALL 允许教师对班级策略执行采纳、调整或忽略，反馈 SHALL 复用现有课堂复盘反馈接口。

#### Scenario: 教师采纳班级策略
- **GIVEN** 当前策略包含 `strategyKey`
- **WHEN** 教师点击采纳策略
- **THEN** 系统 SHALL 保存一条 `ClassReviewFeedback`
- **AND** 保存的 `suggestionKey` SHALL 等于策略的 `strategyKey`
- **AND** 保存的 evidence tags SHALL 包含策略聚焦标签或来源信号

### Requirement: 班级策略必须输出执行成效
系统 SHALL 为 `classTeachingStrategySignal` 输出可选 `impact`，表达策略是否已被教师处理以及后续提交是否改善。

#### Scenario: 策略尚无教师反馈
- **GIVEN** 班级策略没有对应 `strategyKey` 的教师反馈
- **WHEN** 教师读取作业概览
- **THEN** `classTeachingStrategySignal.impact.status` SHALL 为 `NO_FEEDBACK`
- **AND** impact SHALL 给出建议教师先确认是否执行策略

#### Scenario: 策略执行后改善
- **GIVEN** 教师采纳或调整了班级策略
- **AND** 反馈后出现相关 AC 提交
- **WHEN** 教师读取作业概览
- **THEN** `classTeachingStrategySignal.impact.status` SHALL 为 `IMPROVED`
- **AND** impact SHALL 引用后续提交作为证据

#### Scenario: 策略执行后仍卡同类问题
- **GIVEN** 教师采纳或调整了班级策略
- **AND** 反馈后的相关提交仍命中策略聚焦标签或证据标签
- **WHEN** 教师读取作业概览
- **THEN** `classTeachingStrategySignal.impact.status` SHALL 为 `STILL_STUCK`
- **AND** impact SHALL 标记需要升级或更小粒度复盘

### Requirement: AI 质量概览必须评估班级策略成效闭环
`CLASS_TEACHING_STRATEGY_LOOP` SHALL 同时评估策略生成、教师反馈、后续成效和证据引用。

#### Scenario: 策略已执行且改善
- **GIVEN** 班级策略已被教师采纳或调整
- **AND** 后续提交显示改善
- **WHEN** 教师读取 AI 质量概览
- **THEN** `CLASS_TEACHING_STRATEGY_LOOP` SHALL 标记为 `HEALTHY`
- **AND** 摘要 SHALL 说明班级策略已形成执行后改善证据

#### Scenario: 策略仍卡同类问题
- **GIVEN** 班级策略已被教师采纳或调整
- **AND** 后续提交仍命中策略原标签
- **WHEN** 教师读取 AI 质量概览
- **THEN** `CLASS_TEACHING_STRATEGY_LOOP` SHALL 标记为 `ACTION_NEEDED`
- **AND** 建议动作 SHALL 要求升级为更小粒度复盘或教师点对点检查

### Requirement: 班级策略成效闭环必须可验证
班级策略成效闭环 SHALL 有自动化测试覆盖反馈记录、无反馈、改善、仍卡同类问题、教师概览和质量维度。

#### Scenario: 执行验证
- **WHEN** 运行相关后端测试、OpenSpec 校验和前端类型检查
- **THEN** 检查 SHALL 通过
- **AND** 测试 SHALL 覆盖策略反馈与成效状态
