## ADDED Requirements

### Requirement: live-model-eval 摘要必须区分 final/model/fallback 命中

系统 SHALL 在 live-model-eval 的人读摘要中显式区分最终诊断命中、真实模型命中和 fallback 命中。

#### Scenario: fallback-only 命中不会显示为 model 命中

- **GIVEN** live-model-eval report contains one fallback entry with final issue/fine tag hits
- **AND** the entry has `modelIssueTagHit=false` and `fallbackIssueTagHit=true`
- **WHEN** the human-readable summary is generated
- **THEN** the summary SHALL include `finalIssueHits=1`
- **AND** the summary SHALL include `modelIssueHits=0`
- **AND** the summary SHALL include `fallbackIssueHits=1`

### Requirement: live-model-eval baseline regression 报告必须携带当前命中归因

系统 SHALL 在 live-model-eval baseline regression report 中记录当前 report 的 final/model/fallback 命中计数。

#### Scenario: model baseline 对比报告包含命中归因

- **GIVEN** a live-model-eval current report has final hits, model hits, and fallback hits
- **WHEN** the baseline regression report is built from the current report
- **THEN** the regression report SHALL include current final issue/fine hit counts
- **AND** the regression report SHALL include current model issue/fine hit counts
- **AND** the regression report SHALL include current fallback issue/fine hit counts

### Requirement: 新增摘要归因必须保持兼容

系统 SHALL keep existing live-model-eval report hit fields compatible while adding attribution-specific human summary and regression report fields.

#### Scenario: 原有 final hit 字段保持可用

- **GIVEN** a live-model-eval report is summarized
- **WHEN** final hit fields are read
- **THEN** `issueTagHitCount` and `fineTagHitCount` SHALL still represent final diagnosis hits
- **AND** new attribution fields SHALL NOT remove or rename existing report fields
