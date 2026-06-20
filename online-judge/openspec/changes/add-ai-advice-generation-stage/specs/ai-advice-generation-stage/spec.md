## ADDED Requirements

### Requirement: 完整诊断与建议生成
系统 SHALL 在搜索定位后的完整模型阶段生成结构化 advice 输出，包含题目理解、代码理解、行为差距、基础层建议、提高层建议和下一步行动。

#### Scenario: 生成完整 advice 输出
- **WHEN** 搜索定位阶段成功或回退后仍得到可用 `StandardLibraryPack`
- **THEN** 第二次完整模型调用 MUST 请求 `diagnosis-and-advice-v1` 输出结构化 advice JSON

#### Scenario: 输出区分基础层和提高层
- **WHEN** 模型返回 advice 输出
- **THEN** 系统 MUST 能区分基础层阻塞问题和提高层提升建议

### Requirement: Advice 输出证据与标准库绑定
系统 SHALL 校验 advice 输出中的证据引用和标准库条目绑定。

#### Scenario: 证据引用合法
- **WHEN** advice 输出引用 `evidenceRefs`
- **THEN** 所有引用 MUST 来自 `ModelDiagnosisBrief.evidenceRefs` 或候选信号证据

#### Scenario: 标准库 ID 合法
- **WHEN** advice 输出包含 `mistakePointId`、`skillUnitId` 或 `improvementPointId`
- **THEN** 对应 ID MUST 存在于当前完整诊断使用的 `StandardLibraryPack`

### Requirement: Advice 输出安全校验
系统 SHALL 防止 advice 输出泄露完整答案、完整代码、隐藏测试或可直接替换的解法。

#### Scenario: 拒绝答案泄露
- **WHEN** advice 输出包含完整代码、完整答案、隐藏测试推测或高风险替换逻辑
- **THEN** advice 阶段 MUST 判定失败并回退现有诊断反馈

#### Scenario: 拒绝空基础层建议
- **WHEN** 当前提交未 AC 且 advice 输出没有基础层建议
- **THEN** advice 阶段 MUST 判定失败并回退现有诊断反馈

### Requirement: Advice 输出兼容学生反馈
系统 SHALL 将通过校验的 advice 输出映射到现有学生反馈结构。

#### Scenario: 映射基础层建议
- **WHEN** advice 输出包含基础层建议
- **THEN** 系统 MUST 将其映射到 `StudentFeedback.blockingIssues`

#### Scenario: 映射提高层建议
- **WHEN** advice 输出包含提高层建议
- **THEN** 系统 MUST 将其映射到 `StudentFeedback.improvementOpportunities`

#### Scenario: 映射下一步行动
- **WHEN** advice 输出包含下一步行动
- **THEN** 系统 MUST 将第一条行动映射到 `StudentFeedback.nextLearningAction`

### Requirement: Advice 阶段可观测
系统 SHALL 在 AI 调用信息中记录 advice 阶段状态。

#### Scenario: 记录 advice 成功
- **WHEN** advice 阶段成功参与诊断
- **THEN** `AiInvocation` MUST 记录 advice 状态、prompt 版本、基础层建议数量和提高层建议数量

#### Scenario: 记录 advice 回退
- **WHEN** advice 阶段失败并使用回退反馈
- **THEN** `AiInvocation` MUST 记录 advice 失败原因，不得伪装为 advice 成功
