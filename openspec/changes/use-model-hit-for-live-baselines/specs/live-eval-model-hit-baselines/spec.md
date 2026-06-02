## ADDED Requirements

### Requirement: Model quality baselines use model hit signals

系统 SHALL 只使用真实模型命中信号生成 live-model-eval 模型质量基线。

#### Scenario: Completed model hit creates baseline

- **WHEN** live-model-eval entry has `status=MODEL_COMPLETED`
- **AND** `modelCompleted=true`
- **AND** `fallbackUsed=false`
- **AND** `modelIssueTagHit=true` or `modelFineTagHit=true`
- **AND** evidence and safety pass
- **AND** latency budget is healthy
- **THEN** quality baseline draft MAY be generated
- **AND** expected signals MUST include model hit tokens

#### Scenario: Partial model hit does not create healthy baseline

- **WHEN** live-model-eval entry has `status=MODEL_PARTIAL_COMPLETED`
- **AND** `modelCompleted=true`
- **AND** model hit fields are true
- **THEN** quality baseline draft MUST NOT be generated for that entry

#### Scenario: Fallback-only hit does not create baseline

- **WHEN** live-model-eval entry has final expected tag hits
- **AND** `fallbackUsed=true`
- **AND** `modelIssueTagHit=false`
- **AND** `modelFineTagHit=false`
- **THEN** quality baseline draft MUST NOT be generated for that entry

### Requirement: Model baseline regression gate checks model hit signals

系统 SHALL 使用 model hit 字段判断 live-model-eval 模型质量基线是否回归。

#### Scenario: Model hit keeps baseline

- **WHEN** baseline mustKeep includes `modelIssueTagHit` or `modelFineTagHit`
- **AND** current entry has the corresponding model hit field true
- **THEN** regression gate MUST NOT report that hit token as missing

#### Scenario: Fallback hit does not satisfy model baseline

- **WHEN** baseline mustKeep includes `modelIssueTagHit` or `modelFineTagHit`
- **AND** current entry has fallback hits but the corresponding model hit field is false
- **THEN** regression gate MUST report model hit regression

#### Scenario: Legacy expected hit token maps to model hit

- **WHEN** an existing baseline mustKeep includes `expectedIssueTagHit` or `expectedFineTagHit`
- **THEN** regression gate MUST evaluate those legacy tokens against model hit fields, not final hit fields
