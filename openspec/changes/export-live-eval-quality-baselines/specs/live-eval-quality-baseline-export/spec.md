## ADDED Requirements

### Requirement: live eval 报告必须输出成功质量 baseline 草稿

系统 SHALL 在 assistant live eval 和 model diagnosis live eval 报告中输出顶层 `qualityBaselineDrafts` 与 `qualityBaselineDraftCount`。

#### Scenario: assistant live eval 存在高质量成功样本

- **GIVEN** assistant live eval entry `completedOutput=true`
- **AND** `fallbackUsed=false`
- **AND** `expectedSignalHit=true`
- **AND** `evidenceValid=true`
- **AND** `safetyPassed=true`
- **WHEN** 系统生成 live eval 报告
- **THEN** 报告 SHALL include `qualityBaselineDraftCount`
- **AND** `qualityBaselineDrafts` SHALL include 对应 case 的 baseline 草稿
- **AND** 草稿 SHALL include `caseId`、`assistantType`、`model`、`promptVersion`、`mustKeep`、`evidenceRefs` and `regressionPurpose`

#### Scenario: model diagnosis live eval 存在高质量成功样本

- **GIVEN** model diagnosis live eval entry `fallbackUsed=false`
- **AND** issue tag or fine tag hit is true
- **AND** `evidenceValid=true`
- **AND** `safetyPassed=true`
- **WHEN** 系统生成 live eval 报告
- **THEN** `qualityBaselineDrafts` SHALL include 对应诊断 case 的 baseline 草稿

### Requirement: quality baseline 草稿必须排除失败和低质量样本

系统 SHALL NOT 将 fallback、runtime failure、安全失败、信号未命中或证据无效样本导出为 quality baseline 草稿。

#### Scenario: 样本完成但质量未命中

- **GIVEN** entry `completedOutput=true`
- **AND** `expectedSignalHit=false`
- **WHEN** 生成 quality baseline 草稿
- **THEN** 该 entry SHALL NOT appear in `qualityBaselineDrafts`

### Requirement: quality baseline 草稿必须保护敏感内容

系统 SHALL 对 baseline 草稿中的 teacherExpectation、outputSummary 和 outputDetail 做截断与脱敏。

#### Scenario: 成功输出含敏感片段

- **GIVEN** entry output contains API Key or token-like text
- **WHEN** 生成 quality baseline 草稿
- **THEN** 草稿 SHALL replace sensitive values with redacted markers
- **AND** `mustNotMention` SHALL include API Key, token, 完整代码, 参考答案 and 隐藏测试点

### Requirement: quality baseline 导出必须可结构化验证

系统 SHALL 通过无需真实外部模型的测试验证 quality baseline 草稿生成。

#### Scenario: 结构测试生成 baseline 草稿

- **WHEN** 执行 live eval 报告结构测试
- **THEN** 测试 SHALL construct successful assistant and model entries
- **AND** 测试 SHALL assert baseline count, mustKeep, evidenceRefs, regressionPurpose and redaction
- **AND** 测试 SHALL assert failed or low-quality entries are excluded
