## Why

最近一次真实 `live-model-eval` 仍被 ModelScope 429 / rate limit 阻塞，报告已经能说明 `recoveryStatus=BLOCKED`，但模型诊断 live eval 本身还不能像 assistant live eval 一样配置样本间等待，也不会把本次实验是否启用冷却写入报告。

这会让维护者难以区分两类情况：

- 评测连续请求过快，导致供应商限流噪声偏高。
- 已经启用样本间冷却后，外部模型仍因额度或供应商侧限制无法恢复。

本变更把已有 `AI_EVAL_CASE_DELAY_MS` 节流语义扩展到 `live-model-eval`，并在报告和控制台摘要中记录实验冷却配置，让真实外部模型评测更可复现、更可解释。

## What Changes

- `ModelDiagnosisEvalTest` 的 live model smoke 在每个 case 之间支持 `AI_EVAL_CASE_DELAY_MS` 等待。
- `LiveModelEvalReport` 顶层新增 `caseDelayMs` 和 `delayedCaseCount`。
- `liveModelSummaryLine(...)` 输出 case delay 与实际延迟 case 数。
- 补充无真实 API 的结构测试，验证报告字段和控制台摘要。
- 不改变模型调用 prompt、诊断逻辑、baseline regression gate 规则或生产配置。

## Capabilities

### New Capabilities

- `live-model-eval-case-delay`: live model eval 必须支持样本间冷却并在报告中沉淀冷却配置。

### Modified Capabilities

- `external-model-call-stability`: 既有 `AI_EVAL_CASE_DELAY_MS` 不再只适用于 assistant live eval，也适用于 model diagnosis live eval。

## Impact

- 测试报告 DTO：`LiveModelEvalReport`
- live model eval：`ModelDiagnosisEvalTest`
- 验证：相关后端测试、OpenSpec strict validate、secret scan、`git diff --check`
