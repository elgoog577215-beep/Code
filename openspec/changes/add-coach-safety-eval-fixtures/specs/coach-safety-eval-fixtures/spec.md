## ADDED Requirements

### Requirement: Coach 安全拒绝 fixture 必须覆盖典型越界草稿

系统 SHALL 提供默认 Coach 安全拒绝 eval fixture，覆盖模型追问草稿中的完整答案泄露、隐藏测试泄露、英文直接修复提示和证据缺失风险。

#### Scenario: 加载默认安全拒绝 fixture

- **WHEN** 测试加载默认 Coach 安全拒绝 fixture
- **THEN** 每条 fixture SHALL 包含名称、turnType、submissionId、primaryTag、hintPolicy、contextSummary、evidenceRefs、unsafeModelResponse、expectedFailureReason、expectedModelAnswerLeakRisk、requiredFallbackQuestion 和 forbiddenPhrases
- **AND** fixture SHALL 至少覆盖完整答案泄露、隐藏测试泄露、英文直接修复提示和证据缺失四类风险

### Requirement: Coach 安全拒绝 fixture 必须触发规则回退

系统 SHALL 使用默认 Coach 安全拒绝 fixture 验证本地安全门会拒绝越界模型草稿，并回退到安全规则追问。

#### Scenario: 越界模型草稿被安全门拒绝

- **GIVEN** 某条 Coach 安全拒绝 fixture 的 unsafeModelResponse 被模型 stub 返回
- **WHEN** 系统生成 Coach 追问
- **THEN** 返回的 CoachDraft SHALL 使用 `source=RULE`
- **AND** CoachDraft SHALL 包含 `failureReason=SAFETY_REJECTED`
- **AND** CoachDraft SHALL 保留或提升模型草稿的 `modelAnswerLeakRisk`
- **AND** CoachDraft 的学生可见文本 SHALL 不包含 fixture 定义的 forbiddenPhrases
- **AND** CoachDraft 的 question SHALL 等于 fixture 定义的 requiredFallbackQuestion

### Requirement: Coach 本地安全门必须识别中英文泄题短语

系统 SHALL 在 Coach 本地安全门中识别中文和英文的直接答案、直接修复、隐藏测试、参考答案和代码泄露表达。

#### Scenario: 模型自报 LOW 但文本包含英文直接修复

- **GIVEN** 模型草稿的 `answerLeakRisk=LOW`
- **AND** 草稿文本包含英文直接修复、完整答案或隐藏测试泄露短语
- **WHEN** 本地安全门校验该草稿
- **THEN** 系统 SHALL 拒绝该草稿
- **AND** 回退结果 SHALL 标记 `SAFETY_REJECTED`
