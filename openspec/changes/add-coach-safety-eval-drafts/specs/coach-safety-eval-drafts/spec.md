## ADDED Requirements

### Requirement: Coach 安全拒绝必须导出为安全 eval 草稿

系统 SHALL 在诊断 eval fixture 草稿导出中，将 Coach 模型追问安全拒绝事件转换为安全 fixture 草稿。

#### Scenario: 作业内存在 Coach 安全拒绝 prompt

- **GIVEN** 作业内某次提交存在 `CoachPrompt.modelFailureReason=SAFETY_REJECTED`
- **WHEN** 教师预览诊断 eval fixture 草稿
- **THEN** 响应的 `safetyFixtures` SHALL 包含该提交的安全草稿
- **AND** 草稿 SHALL 包含 Coach 安全拒绝 risk source
- **AND** 草稿 SHALL 包含 `coach_prompt:<id>` 和 `coach_safety_rejection:submission:<submissionId>` evidenceRefs
- **AND** 草稿 SHALL 包含 `modelAnswerLeakRisk` 对应的风险等级或不低于 MEDIUM

### Requirement: Coach 安全拒绝草稿不得暴露不安全模型草稿全文

系统 SHALL 在导出的 Coach 安全拒绝草稿中避免输出原始不安全模型草稿全文。

#### Scenario: 生成 Coach 安全拒绝安全草稿

- **WHEN** 系统构建 Coach 安全拒绝对应的 `SafetyFixtureDraft`
- **THEN** `originalHintPreview` SHALL 使用脱敏风险描述
- **AND** `safeHintPreview` SHALL 使用最终安全规则追问或安全回退说明
- **AND** `mustNotMention` SHALL 包含完整代码、参考答案、隐藏测试点、直接改成和最终答案

### Requirement: 多类安全来源必须按提交合并

系统 SHALL 将同一 submissionId 下的诊断高泄题风险、提示安全降级和 Coach 安全拒绝合并为一条安全 fixture 草稿。

#### Scenario: 同一提交同时存在提示安全和 Coach 安全风险

- **GIVEN** 同一 submissionId 同时存在高泄题诊断、提示安全降级或 Coach 安全拒绝
- **WHEN** 系统生成 `safetyFixtures`
- **THEN** 该 submissionId SHALL 只生成一条安全草稿
- **AND** `riskSources` SHALL 包含所有命中的风险来源
- **AND** `blockedReasons` 和 `sourceMaterial.artifacts` SHALL 保留 Coach 安全拒绝来源信息
