## ADDED Requirements

### Requirement: live eval 必须支持 baseline regression gate

系统 SHALL 支持使用上一份 assistant live eval report 中的 `qualityBaselineDrafts` 对当前 assistant live eval report 做逐 case regression 检查。

#### Scenario: 当前输出保持 baseline

- **GIVEN** baseline report contains `qualityBaselineDrafts`
- **AND** 当前 report 中存在相同 `caseId` 的 entry
- **AND** 当前 entry completed, not fallback, safety passed, signal hit and evidence valid
- **WHEN** baseline regression gate evaluates the report
- **THEN** gate SHALL return no violations

### Requirement: baseline regression gate 必须输出具体退化原因

系统 SHALL 在 baseline regression 失败时输出 caseId 与具体退化原因。

#### Scenario: 当前 case 发生 fallback 退化

- **GIVEN** baseline contains case `diagnosis-good`
- **AND** 当前 entry for `diagnosis-good` has `fallbackUsed=true`
- **WHEN** gate evaluates the report
- **THEN** violations SHALL include `diagnosis-good`
- **AND** violations SHALL mention fallback regression

#### Scenario: 当前 case 丢失 baseline 证据引用

- **GIVEN** baseline mustKeep includes evidence ref `case:input:1`
- **AND** 当前 entry actual evidence refs do not include `case:input:1`
- **WHEN** gate evaluates the report
- **THEN** violations SHALL include missing mustKeep or missing evidence

### Requirement: baseline regression gate 必须通过环境变量显式启用

系统 SHALL 仅在 `AI_EVAL_BASELINE_REPORT` 指向可读取报告时执行 baseline regression gate。

#### Scenario: 未配置 baseline report

- **WHEN** assistant live eval finishes
- **AND** `AI_EVAL_BASELINE_REPORT` is blank
- **THEN** live eval SHALL skip baseline regression gate

### Requirement: baseline regression gate 必须可结构化验证

系统 SHALL 通过无需真实外部模型的测试验证 baseline regression gate。

#### Scenario: 结构测试覆盖通过和退化

- **WHEN** 执行 baseline regression 结构测试
- **THEN** 测试 SHALL construct baseline drafts and current entries
- **AND** 测试 SHALL assert no violation for preserved baseline
- **AND** 测试 SHALL assert concrete violations for fallback, safety, evidence or mustKeep regression
