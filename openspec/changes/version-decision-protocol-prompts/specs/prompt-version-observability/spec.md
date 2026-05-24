## ADDED Requirements

### Requirement: prompt 语义变化必须更新版本号

When a prompt template gains new behavior-affecting instructions, the system SHALL expose a new stable promptVersion.

#### Scenario: 诊断裁决协议加入诊断 prompt

- **GIVEN** diagnosis judge prompt includes `standardLibrary.decisionProtocol`
- **WHEN** runtime prepares an external diagnosis plan
- **THEN** the diagnosis prompt version SHALL be `diagnosis-judge-v2`
- **AND** completed staged analysis SHALL record `diagnosis-judge-v2+teaching-hint-v1`.

#### Scenario: 诊断裁决协议加入 single-call prompt

- **GIVEN** single-call prompt includes `standardLibrary.decisionProtocol`
- **WHEN** runtime prepares a single-call external diagnosis plan
- **THEN** the single-call prompt version SHALL be `diagnosis-and-teaching-v2`
- **AND** completed single-call analysis SHALL record `diagnosis-and-teaching-v2`.

### Requirement: 版本记录必须服务评测和教师端分析

AI invocation records and live eval reports SHALL preserve the effective promptVersion used by the model call.

#### Scenario: live eval 运行 single-call 诊断

- **GIVEN** live eval runs `SUBMISSION_DIAGNOSIS` with `AI_EVAL_EXTERNAL_RUNTIME_MODE=single-call`
- **WHEN** an external model completed output is produced
- **THEN** the report entry SHALL include `promptVersion=diagnosis-and-teaching-v2`.

#### Scenario: single-call 诊断失败后进入本地兜底

- **GIVEN** single-call runtime uses `diagnosis-and-teaching-v2`
- **WHEN** model output fails validation and the system falls back to local diagnosis
- **THEN** the fallback invocation record SHALL still include `promptVersion=diagnosis-and-teaching-v2`.
