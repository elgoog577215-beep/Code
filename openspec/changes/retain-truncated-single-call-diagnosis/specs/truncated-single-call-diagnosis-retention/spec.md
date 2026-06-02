## ADDED Requirements

### Requirement: Retain valid diagnosis from truncated single-call output

系统 SHALL 在 single-call 外部模型输出因 length 截断但 `diagnosisDecision` 子对象完整可校验时，保留该外部模型错因裁决。

#### Scenario: Truncated teaching hint preserves diagnosis decision

- **WHEN** single-call runtime receives stream output with `streamFinishReason=length`
- **AND** the root `CombinedOutput` JSON cannot be parsed
- **AND** the raw content contains a complete `diagnosisDecision` object
- **AND** the extracted diagnosis decision passes normalization and validation
- **THEN** the submission analysis MUST use the extracted issue tag, fine-grained tag and evidence refs
- **AND** `aiInvocation.status` MUST be `MODEL_PARTIAL_COMPLETED`
- **AND** `aiInvocation.fallbackUsed` MUST be false
- **AND** `aiInvocation.failureReason` MUST be `OUTPUT_TRUNCATED`
- **AND** the student-facing hint MUST come from the local safe teaching template rather than the truncated `teachingHint`

#### Scenario: Invalid extracted diagnosis still falls back

- **WHEN** single-call runtime receives truncated output containing a `diagnosisDecision` object
- **AND** the extracted diagnosis decision fails validation
- **THEN** the system MUST use runtime fallback
- **AND** it MUST NOT mark the result as `MODEL_PARTIAL_COMPLETED`

### Requirement: Live eval reports partial completion separately

系统 SHALL 在 live model eval report 中区分完全完成、部分完成和 fallback。

#### Scenario: Partial completion is not counted as full completion

- **WHEN** a live eval entry has `status=MODEL_PARTIAL_COMPLETED`
- **THEN** report `partialCount` MUST include the entry
- **AND** `completedCount` MUST NOT include the entry unless status is `MODEL_COMPLETED`
- **AND** `fallbackCount` MUST NOT include the entry when `fallbackUsed=false`
