## ADDED Requirements

### Requirement: 诊断 eval 草稿必须导出提示安全 fixture
系统 SHALL 在诊断 eval 草稿响应中导出提示安全 fixture draft，覆盖高泄题诊断和提示安全降级事件。

#### Scenario: 高泄题诊断生成安全草稿
- **GIVEN** 作业内某条诊断的 `answerLeakRisk` 为 `HIGH`
- **WHEN** 教师导出诊断 eval fixture draft
- **THEN** 响应 SHALL 包含 `safetyFixtures`
- **AND** 对应 safety fixture SHALL 包含 `riskSources` 中的 `DIAGNOSIS_HIGH_LEAK_RISK`
- **AND** `mustNotMention` SHALL 包含完整代码、参考答案和隐藏测试点

#### Scenario: 安全降级记录生成安全草稿
- **GIVEN** 作业内某次提交存在 `HintSafetyCheck.riskLevel` 为 `MEDIUM` 或 `HIGH`
- **WHEN** 教师导出诊断 eval fixture draft
- **THEN** 对应 safety fixture SHALL 包含 `riskSources` 中的 `HINT_SAFETY_CHECK`
- **AND** evidenceRefs SHALL 包含 `hint_safety_check:<id>` 或 `hint_safety_submission:<submissionId>`
- **AND** safety fixture SHALL 包含 blocked reasons、original hint preview 和 safe hint preview

#### Scenario: LOW 安全检查不生成草稿
- **GIVEN** 作业内某次提交只有 `HintSafetyCheck.riskLevel` 为 `LOW`
- **WHEN** 教师导出诊断 eval fixture draft
- **THEN** `safetyFixtures` SHALL NOT 包含该提交

### Requirement: 安全 fixture 草稿必须按提交合并风险来源
系统 SHALL 将同一 submissionId 下的高泄题诊断和安全降级记录合并为一条 safety fixture draft。

#### Scenario: 同一提交有多类安全来源
- **GIVEN** 同一提交同时存在 `answerLeakRisk=HIGH` 和 `HintSafetyCheck.riskLevel=HIGH`
- **WHEN** 系统生成 safety fixture draft
- **THEN** 只 SHALL 生成一条该提交的 safety fixture
- **AND** `riskSources` SHALL 同时包含 `DIAGNOSIS_HIGH_LEAK_RISK` 和 `HINT_SAFETY_CHECK`
- **AND** evidenceRefs SHALL 合并诊断证据和安全检查证据

### Requirement: 安全 eval 草稿必须保持现有导出兼容
系统 MUST 保持原诊断 fixture、课堂介入 fixture 和响应字段兼容。

#### Scenario: 没有安全事件
- **GIVEN** 作业内没有高泄题诊断或中高风险安全降级
- **WHEN** 教师导出诊断 eval fixture draft
- **THEN** 响应 SHALL 保留原有 `candidateCount`、`fixtureCount`、`interventionFixtureCount`、`fixtures` 和 `interventionFixtures`
- **AND** `safetyFixtureCount` SHALL 为 0

### Requirement: 安全 eval 草稿必须可验证
系统 MUST 提供测试覆盖安全草稿导出、来源合并、LOW 检查过滤和类型兼容。

#### Scenario: 执行相关验证
- **WHEN** 运行 OpenSpec 严格校验、ClassroomService eval 草稿测试、后端编译、前端 typecheck 和 diff 检查
- **THEN** 检查 SHALL 通过
