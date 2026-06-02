## ADDED Requirements

### Requirement: live-model-eval 必须记录实际标签和证据 refs

系统 SHALL 在 `LiveModelEvalReport.Entry` 中记录外部模型诊断最终结果的实际 issue tags、fine-grained tags 和 evidence refs。

#### Scenario: 模型诊断成功写入结构化输出

- **GIVEN** live model eval case completed without fallback
- **WHEN** report entry is written
- **THEN** entry SHALL include `actualIssueTags`
- **AND** entry SHALL include `actualFineGrainedTags`
- **AND** entry SHALL include `actualEvidenceRefs`

### Requirement: live-model-eval baseline 草稿必须包含结构化标签和证据

系统 SHALL 从成功的 live-model-eval entry 生成包含实际标签和证据 refs 的 quality baseline draft。

#### Scenario: 成功 model entry 生成强 baseline

- **GIVEN** model entry `fallbackUsed=false`
- **AND** expected issue or fine tag hit is true
- **AND** `evidenceValid=true`
- **AND** `safetyPassed=true`
- **WHEN** baseline draft is generated
- **THEN** draft `mustKeep` SHALL include actual `issue:*` or `fine:*` signals when present
- **AND** draft `evidenceRefs` SHALL include current actual evidence refs

### Requirement: live-model-eval 必须支持 baseline regression gate

系统 SHALL 支持使用上一份 live-model-eval report 的 `qualityBaselineDrafts` 对当前 live-model-eval report 做逐 case regression 检查。

#### Scenario: 当前 model 输出保持 baseline

- **GIVEN** baseline report contains model quality baseline drafts
- **AND** current report contains the same caseId
- **AND** current entry is not fallback, JSON valid, issue/fine signal hit, evidence valid and safety passed
- **WHEN** model baseline regression gate evaluates the report
- **THEN** gate SHALL return no violations

#### Scenario: 当前 model 输出丢失细粒度标签

- **GIVEN** baseline mustKeep includes `fine:OFF_BY_ONE`
- **AND** current entry actual fine-grained tags do not include `OFF_BY_ONE`
- **WHEN** gate evaluates the report
- **THEN** violations SHALL include the caseId
- **AND** violations SHALL mention missing mustKeep `fine:OFF_BY_ONE`

### Requirement: model baseline regression gate 必须显式启用

系统 SHALL 仅在 `AI_EVAL_MODEL_BASELINE_REPORT` 指向可读取 report 时执行 live-model-eval baseline regression gate。

#### Scenario: 未配置 model baseline report

- **WHEN** live model smoke finishes
- **AND** `AI_EVAL_MODEL_BASELINE_REPORT` is blank
- **THEN** live model eval SHALL skip model baseline regression gate

### Requirement: model baseline regression 必须可结构化验证

系统 SHALL 通过无需真实外部模型的测试验证 model baseline regression gate。

#### Scenario: 结构测试覆盖通过和退化

- **WHEN** 执行 baseline regression 结构测试
- **THEN** 测试 SHALL assert no violation for preserved model baseline
- **AND** 测试 SHALL assert concrete violations for fallback, JSON, tag, evidence or safety regression
