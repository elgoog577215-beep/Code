## Context

live-model-eval 的 entry 现在记录的是最终 `SubmissionAnalysisResponse` 是否命中 expected tags。这个最终结果对用户体验有价值，但当真实模型失败后进入规则 fallback 时，命中来自本地规则，不代表外部模型诊断质量。最新真实低延迟 smoke 就是这种情况：ModelScope 返回 429 今日额度超限，`fallbackUsed=true`，但最终 `issueTagHitCount/fineTagHitCount` 仍为 1。

同时 runtime fixture draft 目前只把 `INSUFFICIENT_QUOTA` 归为 `QUOTA_LIMIT`，`RATE_LIMITED` 被归为 `PROVIDER_ERROR`。ModelScope 429 quota exhausted 在当前链路中会成为 `RATE_LIMITED`，导致刚新增的 offline profile guidance 没有触发。

## Goals / Non-Goals

**Goals:**

- 保留现有最终命中计数，继续表示“用户最后看到的诊断是否命中”。
- 新增模型命中计数，只有真实模型完成或部分完成时才计入。
- 新增 fallback 命中计数，表示规则兜底保住了哪些 expected tags。
- entry 级别输出模型/fallback 命中布尔值，便于后续 baseline gate 和 teacher report 消费。
- 将 `RATE_LIMITED`/429 quota 场景纳入 `QUOTA_LIMIT` runtime draft，引导 offline profile eval。

**Non-Goals:**

- 不改变模型调用、fallback 生成、校验器或诊断内容。
- 不删除 `issueTagHitCount/fineTagHitCount` 兼容字段。
- 不把 fallback 命中视为外部模型质量成功。

## Decisions

### 使用三组指标表达不同语义

`issueTagHitCount/fineTagHitCount` 保持原义：最终诊断命中。新增：

- `modelIssueTagHitCount/modelFineTagHitCount`
- `fallbackIssueTagHitCount/fallbackFineTagHitCount`
- entry 级 `modelCompleted/modelIssueTagHit/modelFineTagHit/fallbackIssueTagHit/fallbackFineTagHit`

这样一个 quota fallback report 会清楚显示：最终命中是 1，模型命中是 0，fallback 命中是 1。

### 以 status/fallbackUsed 判定模型完成

`modelCompleted` 为 true 的条件是 `fallbackUsed=false` 且 status 为 `MODEL_COMPLETED` 或 `MODEL_PARTIAL_COMPLETED`。partial completion 可以保留有效诊断，因此仍可计入模型命中；纯 runtime fallback 不能计入。

### 将 rate-limited 也视作 quota guidance

对 live eval runtime draft，`RATE_LIMITED` 与 `INSUFFICIENT_QUOTA` 都是 provider 额度/限流阻断真实评测的原因。Factory 将其统一分类为 `QUOTA_LIMIT`，这样 offline profile guidance 能在 429 rate-limited 报告中出现。

## Risks / Trade-offs

- [Risk] 指标变多后阅读成本上升。→ 保留最终命中字段，同时新增字段命名明确为 model/fallback。
- [Risk] 部分完成是否应计入模型命中存在边界。→ 只有对应 expected tag 命中才计入；已有 partial report 仍保留 runtime draft。
- [Risk] `RATE_LIMITED` 有时是临时限流而非额度耗尽。→ 仍属于外部调用受限，推荐 offline profile eval 是低成本前置证据，不替代额度恢复后的 live eval。
