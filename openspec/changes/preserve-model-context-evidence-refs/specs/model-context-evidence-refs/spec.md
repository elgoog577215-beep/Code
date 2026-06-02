## ADDED Requirements

### Requirement: 外部模型成功后必须保留上下文证据引用

系统 SHALL 在外部模型诊断成功后，将模型输出证据引用与规则信号、判题事实和 evidence package 摘要证据合并到最终诊断结果中。

#### Scenario: 模型输出缺少 verdict 证据但上下文包含

- **GIVEN** evidence package contains submission verdict `WRONG_ANSWER`
- **AND** 外部模型诊断成功且输出合法 evidence refs
- **WHEN** 系统生成最终 `SubmissionAnalysisResponse`
- **THEN** response `evidenceRefs` SHALL include the model evidence refs
- **AND** response `evidenceRefs` SHALL include `verdict:wrong_answer`
- **AND** response SHALL NOT mark the case as fallback

### Requirement: 上下文证据保留不得改变模型质量归因

系统 SHALL only add stable evidence refs and MUST NOT use context evidence preservation to hide model runtime fallback or overwrite model-selected diagnosis tags.

#### Scenario: 模型成功标签保持模型结果

- **GIVEN** model diagnosis selected a valid issue tag and fine-grained tag
- **WHEN** context evidence refs are merged
- **THEN** final tags SHALL still come from the validated model result and existing calibration pipeline
- **AND** `aiInvocation.fallbackUsed` SHALL remain false when the model succeeded

### Requirement: live eval baseline 必须能观察保留后的上下文证据

系统 SHALL 让 assistant live eval report 的 `actualEvidenceRefs` 反映最终诊断结果中的保留证据，以支持 baseline regression gate。

#### Scenario: baseline mustKeep 包含 verdict ref

- **GIVEN** baseline draft mustKeep includes `verdict:wrong_answer`
- **AND** current external model run succeeds for the same case
- **WHEN** live eval writes the current report
- **THEN** current entry `actualEvidenceRefs` SHALL contain `verdict:wrong_answer`
- **AND** baseline regression gate SHALL not fail only because the verdict ref was dropped from model text

### Requirement: 上下文证据保留必须可结构化验证

系统 SHALL 通过无需真实 API Key 的测试验证上下文证据保留。

#### Scenario: 结构测试覆盖 verdict 保留

- **WHEN** 执行诊断 agent 或外部 runtime 相关测试
- **THEN** 测试 SHALL assert final `evidenceRefs` contains both model refs and stable context refs such as `verdict:wrong_answer`
