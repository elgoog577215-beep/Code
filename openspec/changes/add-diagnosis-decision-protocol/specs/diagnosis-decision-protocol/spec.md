## ADDED Requirements

### Requirement: 标准库必须提供诊断裁决协议

`StandardLibraryPack` SHALL include a `decisionProtocol` object that explains how the external model should choose diagnosis tags, evidence references, uncertainty, and teaching actions.

#### Scenario: 构建外部模型标准库

- **GIVEN** runtime 正在为一次提交诊断构建 `StandardLibraryPack`
- **WHEN** 标准库构建完成
- **THEN** 标准库 SHALL include non-empty `decisionProtocol.globalRules`
- **AND** it SHALL include evidence priority, tag selection, conflict handling, and teaching action rules.

### Requirement: 诊断提示词必须引用诊断裁决协议

Diagnosis judge and single-call prompts SHALL instruct the external model to follow `standardLibrary.decisionProtocol` before selecting diagnosis tags.

#### Scenario: 诊断阶段调用外部模型

- **GIVEN** diagnosis judge prompt is loaded
- **WHEN** the prompt is inspected
- **THEN** it SHALL mention `standardLibrary.decisionProtocol`
- **AND** it SHALL require evidence-supported tag selection.

#### Scenario: single-call 阶段调用外部模型

- **GIVEN** single-call prompt is loaded
- **WHEN** the prompt is inspected
- **THEN** it SHALL mention `standardLibrary.decisionProtocol`
- **AND** it SHALL require conservative `NEEDS_MORE_EVIDENCE` when evidence cannot distinguish candidates.

### Requirement: 裁决协议不得放宽安全与证据边界

The decision protocol SHALL NOT allow the model to invent evidence, reveal hidden data, output full code, or choose tags outside the provided standard library.

#### Scenario: 证据不足

- **GIVEN** available evidence cannot distinguish candidate diagnosis tags
- **WHEN** the model follows the decision protocol
- **THEN** the model SHALL choose `NEEDS_MORE_EVIDENCE` if it is available
- **AND** uncertainty SHALL state the missing evidence rather than guessing hidden data.
