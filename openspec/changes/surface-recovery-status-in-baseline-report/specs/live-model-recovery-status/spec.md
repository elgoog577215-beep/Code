## MODIFIED Requirements

### Requirement: live-model-eval report 必须输出 recovery status

系统 SHALL 在 live-model-eval report 顶层输出 recovery status summary，用于判断本次 recovery smoke 是否证明外部模型恢复。

#### Scenario: recovery status feeds baseline regression report

- **GIVEN** a live-model-eval report has `recoveryStatus`
- **WHEN** it is used as the current report for model baseline regression
- **THEN** the baseline regression report SHALL include the current recovery status summary
