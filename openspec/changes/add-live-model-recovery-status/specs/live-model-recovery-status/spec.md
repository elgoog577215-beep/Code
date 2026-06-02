## ADDED Requirements

### Requirement: live-model-eval report 必须输出 recovery status

系统 SHALL 在 live-model-eval report 顶层输出 recovery status summary，用于判断本次 recovery smoke 是否证明外部模型恢复。

#### Scenario: recovery smoke 成功

- **GIVEN** a live-model-eval report contains an entry with `modelCompleted=true`
- **AND** `fallbackUsed=false`
- **AND** `modelIssueTagHit=true` or `modelFineTagHit=true`
- **AND** `evidenceValid=true`
- **AND** `safetyPassed=true`
- **WHEN** the report is summarized
- **THEN** `recoveryStatus` SHALL be `RECOVERED`
- **AND** `recoveryPassedChecks` SHALL include model completion, no fallback, model hit, evidence and safety checks

#### Scenario: stream recovery smoke requires content chunk

- **GIVEN** a recovered candidate entry has `transportMode=stream`
- **WHEN** the report is summarized
- **THEN** recovery SHALL require `streamContentChunkCount>0`

#### Scenario: recovery smoke 仍被 quota 阻塞

- **GIVEN** a live-model-eval report contains runtime fixture draft with `recoverySmokeRecommended=true`
- **AND** the current entry has `fallbackUsed=true`
- **WHEN** the report is summarized
- **THEN** `recoveryStatus` SHALL be `BLOCKED`
- **AND** `recoveryBlockedReasons` SHALL include runtime fallback or quota/rate limit evidence

#### Scenario: no recovery context

- **GIVEN** a report has no recovery smoke recommended draft
- **AND** has no runtime fallback or runtime failure entry
- **WHEN** the report is summarized
- **THEN** `recoveryStatus` SHALL be `NOT_APPLICABLE`

### Requirement: live-model-eval 控制台摘要必须展示 recovery status

系统 SHALL include recovery status in the human-readable live-model-eval summary line.

#### Scenario: summary line exposes recovery status

- **GIVEN** a live-model-eval report has `recoveryStatus=BLOCKED`
- **WHEN** the summary line is generated
- **THEN** the summary SHALL include `recoveryStatus=BLOCKED`
- **AND** the summary SHALL include the blocked reason count
