## Why

上一轮已经让课堂数据库里的外部模型 fallback/partial 分析可以导出为 runtime fixture 草稿，但真实 live eval 的失败样本仍只停留在 `target/ai-eval-reports/*.json` 的 per-case 结果里。每次 ModelScope 真实评测暴露的 quota、budget guard、timeout、结构校验或安全回退，还需要人工再读报告、再手工整理成可沉淀样本。

本轮把 live eval 报告本身升级为“报告 + 可审查 runtime 草稿”的双产物，让真实外部模型失败样本在生成报告时就进入回归沉淀入口。

## What Changes

- 为 live eval 报告新增顶层 `runtimeFixtureDrafts` 和 `runtimeFixtureDraftCount`。
- 新增 test-scope 的 runtime draft DTO/Factory，从 `AssistantLiveEvalReport.Entry` 和 `LiveModelEvalReport.Entry` 中生成草稿。
- 草稿覆盖 `MODEL_RUNTIME_FALLBACK`、`fallbackUsed=true`、`MODEL_PARTIAL_COMPLETED`、`EXCEPTION` 以及安全拒绝等需要复核的真实运行样本。
- 草稿包含 eval source、caseId、assistantType/stage、status、failureStage、failureReason 摘要、failureType、expectedRuntimeAction、mustMention/mustNotMention、evidenceRefs、teacherExpectation 和 iterationSuggestion。
- 草稿对 failureReason/outputSummary 做截断和敏感片段脱敏，不输出 API Key、token 或 provider 原始错误全文。
- 扩展结构测试，确认不需要真实外部模型也能验证草稿生成和分类。

## Capabilities

### New Capabilities

- `live-eval-runtime-draft-export`: 覆盖 assistant live eval 和 model diagnosis live eval 如何把真实运行失败/部分完成样本导出为可审查 runtime fixture 草稿。

### Modified Capabilities

- 无。

## Impact

- 测试报告 DTO：`AssistantLiveEvalReport`、`LiveModelEvalReport` 新增 runtime 草稿字段。
- 测试工具：新增 runtime 草稿 factory/DTO，用于生成可复用的报告内草稿。
- 评测链路：`AssistantLiveEvalTest` 与 `ModelDiagnosisEvalTest` 在 summarize 阶段填充草稿。
- 测试：扩展 `AssistantLiveEvalQualityGateTest` 或新增结构测试，覆盖分类、脱敏和 partial/fallback 选择逻辑。
- 生产代码：不直接改动生产服务；本轮仅增强 test/eval 产物。
