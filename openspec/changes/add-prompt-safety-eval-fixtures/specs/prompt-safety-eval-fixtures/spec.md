## ADDED Requirements

### Requirement: 默认诊断评测资源包含提示安全 fixture

系统 SHALL 提供默认提示安全 eval fixture 资源，覆盖完整代码泄露、直接给出改法和隐藏测试点泄露等典型提示安全风险。

#### Scenario: 加载默认提示安全 fixture

- **GIVEN** 默认测试资源目录中存在提示安全 fixture 文件
- **WHEN** 测试加载默认提示安全 fixture
- **THEN** 至少加载 3 条样本
- **AND** 每条样本包含 `riskLevel`、`blockedReasons`、`mustNotMention`、`expectedSafetyAction`、`requiredEvidenceRefs`
- **AND** 每条样本包含可转换的题目、提交和 unsafe analysis

### Requirement: 提示安全 fixture 约束本地安全降级

系统 SHALL 使用提示安全 fixture 验证本地安全门会把越界提示降级为证据驱动的学习动作。

#### Scenario: unsafe analysis 包含完整代码或直接改法

- **GIVEN** 一条提示安全 fixture 的 unsafe analysis 包含完整代码或直接改法
- **WHEN** `HintSafetyService` 处理该 unsafe analysis
- **THEN** 输出的 `answerLeakRisk` 不低于 fixture 期望风险等级
- **AND** 输出文本 SHALL NOT 包含 fixture 的 `mustNotMention`
- **AND** 输出的学生提示动作 SHALL 使用 `COLLECT_EVIDENCE`

#### Scenario: unsafe analysis 包含隐藏测试点泄露

- **GIVEN** 一条提示安全 fixture 的 unsafe analysis 包含隐藏测试点输入或输出
- **WHEN** `HintSafetyService` 处理该 unsafe analysis
- **THEN** 输出 SHALL 不包含隐藏测试点细节
- **AND** 输出 SHALL 要求学生构造最小样例或提供输出对比
