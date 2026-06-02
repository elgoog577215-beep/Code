## Why

最新真实 low-latency live eval 返回 ModelScope 429 今日额度超限，但报告仍把规则 fallback 生成的错因命中计入 `issueTagHitCount/fineTagHitCount`。这会让维护者在额度或限流失败时误读外部模型质量，需要把“最终诊断命中”和“真实模型命中”拆开。

## What Changes

- live-model-eval report 新增模型命中与 fallback 命中计数，保留现有最终命中计数兼容字段。
- 每个 entry 新增 `modelCompleted`、`modelIssueTagHit`、`modelFineTagHit`、`fallbackIssueTagHit`、`fallbackFineTagHit`。
- 当模型 runtime fallback 但规则兜底命中时，报告必须显示模型命中为 false、fallback 命中为 true。
- runtime fixture draft 把 `RATE_LIMITED` 与 `INSUFFICIENT_QUOTA` 都归类为 `QUOTA_LIMIT`，并复用 offline profile guidance。
- 扩展真实 smoke 可观察报告字段和无需 API Key 的结构测试。
- 不保存 raw request、raw prompt、raw response 或 API Key。

## Capabilities

### New Capabilities

- `live-eval-model-fallback-hit-separation`: live model eval 能区分外部模型质量命中与规则 fallback 命中。

### Modified Capabilities

- `live-eval-quota-offline-profile-guidance`: `RATE_LIMITED`/429 quota 场景也应进入 quota guidance，而不是泛化 provider error。

## Impact

- 测试 DTO：`LiveModelEvalReport`
- 测试逻辑：`ModelDiagnosisEvalTest.summarizeReport` 和 entry 构造
- Runtime draft：`LiveEvalRuntimeFixtureDraftFactory` 分类逻辑
- 测试：`ModelDiagnosisEvalTest`、`AssistantLiveEvalQualityGateTest`
- 非目标：不改变生产诊断流程、不改变规则 fallback 行为、不改变真实 ModelScope 调用策略。
