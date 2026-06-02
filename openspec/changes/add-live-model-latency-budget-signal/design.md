## Context

当前 `ModelDiagnosisEvalTest` 已记录每个 case 的 `latencyMs`，但 report summary 只统计 fallback、TIMEOUT failureReason、标签命中、证据和安全。成功请求即使耗时明显超过 `AI_EVAL_TIMEOUT_SECONDS` 或评测可接受预算，也会被当作健康成功样本，继续进入 `qualityBaselineDrafts`。

对于教育 agent 来说，真实外部模型“能答对但太慢”依然是产品风险：学生等待过久、教师工作台评测变慢、baseline regression 无法区分质量退化和性能退化。

## Goals / Non-Goals

**Goals:**

- 将 live-model-eval 的延迟预算变成结构化字段。
- 统计成功但超过预算的慢响应样本。
- 慢响应样本进入 runtime fixture draft，而不是健康 baseline。
- baseline regression gate 能报告 latency budget regression。
- 保持延迟信号与 fallback、timeout、quota、安全退化区分。

**Non-Goals:**

- 不改变底层 HTTP client 的 timeout 行为。
- 不自动降低 max tokens 或切换模型。
- 不把 provider 原始响应、prompt 或 token 写入 report。

## Decisions

### 使用独立 latency budget，而不是复用 timeout

timeout 表示请求失败或异常中止；latency budget 表示请求虽然成功但不适合沉淀为健康能力。两者分开能避免把慢成功样本误归为 runtime failure，也能保留“质量对但性能差”的教育 agent 诊断。

### 默认预算来自 `AI_EVAL_LATENCY_BUDGET_MS`

新增环境变量 `AI_EVAL_LATENCY_BUDGET_MS`，默认值使用当前 live smoke 暴露的 35 秒量级。这样普通 smoke 不需要额外配置，也能把超过预期的成功调用标记出来。

### 慢响应导出为 runtime draft

慢响应不是诊断质量失败，因此不应导出 quality baseline；但它需要可执行优化动作，所以作为 runtime draft 的 `SLOW_RESPONSE` failureType 输出，建议收缩上下文、调整输出 token 或复核 stream reasoning 体积。

### baseline gate 只检查显式预算字段

只有 baseline 或 current entry 带有 latency budget 时才检查慢响应退化，兼容旧 report。

## Risks / Trade-offs

- [Risk] provider 偶发慢响应导致噪声。→ 只作为结构化信号和 draft，不默认阻塞无 baseline 的 smoke。
- [Risk] 默认预算与真实网络波动不完全匹配。→ 支持 `AI_EVAL_LATENCY_BUDGET_MS` 调整。
- [Risk] 成功样本减少 baseline 数量。→ 这是有意取舍，baseline 应代表“质量与运行体验都健康”的样本。
