## ADDED Requirements

### Requirement: AI 质量趋势必须统计 Coach 安全拒绝

系统 SHALL 在 AI 质量趋势中统计 Coach 模型追问被安全门拒绝的次数。

#### Scenario: 跨作业存在 Coach 安全拒绝 prompt

- **GIVEN** 作业提交下存在 `CoachPrompt.modelFailureReason=SAFETY_REJECTED`
- **WHEN** 系统构建 AI 质量趋势
- **THEN** 总览响应 SHALL 包含 `coachSafetyRejectionCount`
- **AND** 对应作业趋势点 SHALL 包含 `coachSafetyRejectionCount`
- **AND** `promptSafetyIncidentCount` SHALL 包含 Coach 安全拒绝次数
- **AND** `promptSafetyDowngradeCount` SHALL 不因 Coach 安全拒绝增加

### Requirement: 来源分段必须保留 Coach 安全拒绝计数

系统 SHALL 在 AI 质量趋势的 source segment 中按 submission analysis 来源归属 Coach 安全拒绝事件。

#### Scenario: Coach 安全拒绝提交有诊断来源

- **GIVEN** 发生 Coach 安全拒绝的 submission 有对应 `SubmissionAnalysis`
- **WHEN** 系统生成 sourceSegments
- **THEN** 对应 source segment SHALL 包含 `coachSafetyRejectionCount`
- **AND** 该 source segment 的 `promptSafetyIncidentCount` SHALL 包含 Coach 安全拒绝次数

### Requirement: 教师端趋势卡片必须展示 Coach 安全回退

教师端 SHALL 在 AI 质量趋势卡片中展示 Coach 安全回退计数，并在作业趋势点上显示 Coach 安全 badge。

#### Scenario: 趋势响应包含 Coach 安全拒绝

- **WHEN** 教师查看 AI 质量趋势卡片
- **THEN** 顶部指标 SHALL 显示 Coach 安全回退计数
- **AND** 存在 Coach 安全拒绝的作业趋势点 SHALL 显示对应 badge
