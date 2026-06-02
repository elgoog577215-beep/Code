## Why

assistant live eval 已经可以用上一份 `qualityBaselineDrafts` 做逐 case baseline regression gate，但核心的 `live-model-eval` 仍只导出正向 baseline 草稿，没有在后续真实模型 smoke 中自动比对。更关键的是，当前 `LiveModelEvalReport.Entry` 只记录标签命中布尔值，不记录实际 issue/fine 标签和 evidence refs，导致 baseline 草稿只能保留 `expectedIssueTagHit` 这类弱信号，无法判断“模型是否仍保留同一个细粒度诊断和证据链”。

本变更把 model diagnosis live eval 从“汇总布尔质量”推进到“可逐 case 回归的结构化质量基线”。

## What Changes

- 扩展 `LiveModelEvalReport.Entry`，记录实际 `actualIssueTags`、`actualFineGrainedTags` 和 `actualEvidenceRefs`。
- `ModelDiagnosisEvalTest` 写报告时把最终诊断结果的实际标签和证据 refs 写入 entry。
- `LiveEvalQualityBaselineDraftFactory` 为 live-model-eval baseline 草稿保留实际标签和证据 refs，而不是只保留命中布尔值。
- `LiveEvalBaselineRegressionGate` 新增 live-model-eval 比较入口，检查同 case 是否保持模型完成、未 fallback、JSON 有效、标签命中、证据有效、安全通过和 mustKeep。
- `ModelDiagnosisEvalTest` 支持通过 `AI_EVAL_MODEL_BASELINE_REPORT` 显式开启 model baseline regression gate。
- 扩展无需 API Key 的结构测试，覆盖 model baseline 保持、fallback/标签/证据退化和缺失 case。

## Capabilities

### New Capabilities

- `live-model-eval-baseline-regression-gate`: 覆盖 live-model-eval 如何使用上一份 model diagnosis baseline 草稿检测后续真实外部模型输出退化。

### Modified Capabilities

- `live-eval-quality-baseline-export`: live-model-eval baseline 草稿从弱布尔信号升级为包含实际标签和证据 refs 的结构化 baseline。

## Impact

- 测试报告 DTO：`LiveModelEvalReport.Entry`
- 评测链路：`ModelDiagnosisEvalTest`
- 测试工具：`LiveEvalBaselineRegressionGate`、`LiveEvalQualityBaselineDraftFactory`
- 测试：`AssistantLiveEvalQualityGateTest`
- 配置：新增 `AI_EVAL_MODEL_BASELINE_REPORT`
- 生产代码：不改动生产服务
