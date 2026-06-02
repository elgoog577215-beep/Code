## Why

上一轮 runtime draft 已经把恢复后该跑的 recovery smoke 和 required checks 结构化，但 live-model-eval report 顶层仍没有说明“本次运行是否已经完成恢复验证”。维护者需要人工组合 completed/fallback/modelHit/evidence/stream 字段，才能判断外部模型是否真的恢复。

本轮新增 live-model-eval recovery status，让真实 smoke 报告直接给出 `RECOVERED`、`BLOCKED` 或 `NOT_APPLICABLE`，并附带阻塞原因与已通过检查。

## What Changes

- `LiveModelEvalReport` 顶层新增 recovery status summary 字段。
- `ModelDiagnosisEvalTest.summarizeReport(...)` 根据 entry 和 runtime fixture draft 自动判定恢复状态。
- 恢复成功要求：模型完成、未 fallback、模型标签命中、证据有效、安全通过，stream 场景还要求 content chunk 大于 0。
- 恢复阻塞时输出具体 blocked reasons，例如 quota/runtime fallback、no model hit、missing evidence、safety failure、stream no content。
- 控制台摘要追加 recovery status，便于 live smoke 直接读结论。

## Capabilities

### New Capabilities
- `live-model-recovery-status`: 覆盖 live-model-eval 如何汇总 recovery smoke 检查结果并输出恢复状态。

### Modified Capabilities
- `runtime-recovery-smoke-guidance`: recovery smoke guidance 的执行结果需要能在 live-model-eval report 顶层表达。

## Impact

- 测试 DTO：`LiveModelEvalReport` 新增 recovery status fields。
- 评测逻辑：`ModelDiagnosisEvalTest.summarizeReport(...)` 填充 recovery status。
- 控制台摘要：live-model-eval summary line 增加 recovery status 和 blocked count。
- 测试：扩展无需 API Key 的结构测试，并用真实 live smoke 验证当前 429 会输出 `BLOCKED`。
