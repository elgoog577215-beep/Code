## Context

外部模型链路现在已经能在 `LiveModelEvalReport.Entry` 和 `LiveEvalRuntimeFixtureDraft` 中保留 transport telemetry。刚运行的真实 smoke 证明当前 provider 返回 429 额度不足，report entry 与 runtime draft 都保留了 `transportMode=stream`、`streamContentChunkCount=0` 和 quota failure。

问题是 runtime draft 的 `expectedRuntimeAction` 仍只按 `failureType` 生成，未消费 transport telemetry。对于外接大模型能力迭代来说，runtime draft 是把真实失败沉淀成 fixture 的入口；如果行动建议不包含传输层证据，后续排查会重新回到人工读 report。

## Goals / Non-Goals

**Goals:**

- 让 live-model-eval runtime draft 的行动建议消费 transport telemetry。
- 对 stream no-content、stream invalid chunk、stream fallback retry 给出可执行调试焦点。
- 保持建议为安全摘要，不写入 raw stream chunk、prompt、response、API Key 或 provider 原始 payload。
- 用结构测试覆盖，无需真实外部模型即可回归。

**Non-Goals:**

- 不改变外部模型 HTTP 调用、stream parser 或 retry 策略。
- 不新增 report schema 字段。
- 不把 transport telemetry 纳入质量分数。

## Decisions

### 在 Factory 生成 transport-aware action

`LiveEvalRuntimeFixtureDraftFactory` 已经同时拥有 `failureType` 和 `LiveModelEvalReport.Entry` 的 transport 字段，因此在这里生成行动建议最小、最直接。相比在 report entry 里新增字段，这不会扩大 schema，也能复用现有 runtime draft 消费端。

### 保留 failureType 默认建议作为 fallback

transport-aware helper 只在已有 telemetry 能提供更具体调试方向时覆盖或补充默认建议。没有 transport mode 的 budget guard 或 unknown runtime failure 继续使用现有建议，避免把本地短路误判成 provider stream 问题。

### 建议文本只包含安全摘要

行动建议只提 `stream content chunk`、`SSE/JSON 解析`、`fallback retry` 等摘要，不包含 raw chunk 或 provider body，和现有 runtime fixture 脱敏边界一致。

## Risks / Trade-offs

- [Risk] 建议文本变长。→ 只在有 transport evidence 时补充关键调试焦点，并继续复用短句。
- [Risk] no-content 与 quota 同时出现时误导为 parser 问题。→ 文案明确先检查额度，再用最小 stream smoke 验证 content chunk。
- [Risk] assistant live eval 没有 transport 字段。→ 只调整 model entry 路径，assistant draft 仍使用原建议。
