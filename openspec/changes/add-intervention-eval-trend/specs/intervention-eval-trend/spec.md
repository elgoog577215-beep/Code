## ADDED Requirements

### Requirement: 跨作业趋势必须统计课堂介入 eval 候选
系统 SHALL 在 AI 质量趋势中统计教师讲评建议和班级策略成效形成的 intervention eval candidate。

#### Scenario: 统计跨作业 intervention candidates
- **GIVEN** 多个作业中存在教师采纳或调整的课堂反馈
- **AND** 反馈后存在 `IMPROVED`、`SHIFTED` 或 `STILL_STUCK` 成效
- **WHEN** 教师读取 AI 质量趋势
- **THEN** 响应 SHALL 包含总 `interventionEvalCandidateCount`
- **AND** 每个 assignment point SHALL 包含对应介入候选数量

### Requirement: 趋势必须区分等待后续和可沉淀成效
系统 SHALL 将等待后续证据的课堂反馈与已可沉淀的成效候选分开统计。

#### Scenario: 等待后续证据不计入 candidate
- **GIVEN** 教师已采纳课堂反馈
- **AND** 反馈后没有后续提交
- **WHEN** 教师读取 AI 质量趋势
- **THEN** `interventionWaitingFollowupCount` SHALL 增加
- **AND** `interventionEvalCandidateCount` SHALL 不因该反馈增加

### Requirement: 趋势必须输出成效状态分布
系统 SHALL 输出 improved、shifted 和 stillStuck 计数，帮助老师识别教学建议长期效果。

#### Scenario: 统计仍卡同类问题
- **GIVEN** 教师介入后后续提交仍命中原证据标签
- **WHEN** 教师读取 AI 质量趋势
- **THEN** `interventionStillStuckCount` SHALL 增加
- **AND** summary SHALL 提示需要优先沉淀或复盘仍卡同类问题

### Requirement: 前端类型必须兼容趋势新字段
前端 API 类型 SHALL 包含趋势总览和作业点上的 intervention eval 字段。

#### Scenario: 类型检查
- **WHEN** 运行前端 typecheck
- **THEN** 新字段 SHALL 类型正确且不破坏现有调用

### Requirement: intervention eval trend 必须可验证
系统 SHALL 有自动化测试覆盖跨作业聚合、assignment point 字段和验证命令。

#### Scenario: 执行验证
- **WHEN** 运行相关后端测试、OpenSpec 校验、后端编译和前端类型检查
- **THEN** 检查 SHALL 通过
