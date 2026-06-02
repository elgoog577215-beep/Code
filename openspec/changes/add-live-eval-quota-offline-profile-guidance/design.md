## Context

当前 live-model-eval 已经能记录真实外部模型的 runtime profile、request bytes、stream telemetry 和 quota fallback 原因。刚补上的 offline runtime profile eval 又能在不调用 ModelScope 的情况下，对同一类 eval case 比较 `standard` 与 `low-latency` request 体积，并验证 compact profile 是否保留证据、标签、教学动作和 hidden boundary。

缺口在两者之间：当 live eval 因 `INSUFFICIENT_QUOTA` 失败时，runtime fixture draft 只给出额度/stream 排查建议，没有结构化指向 offline profile eval。这样维护者知道 provider 挂了，却不知道可以先验证“低延迟输入压缩链路是否健康”。

## Goals / Non-Goals

**Goals:**

- quota fallback runtime draft 明确推荐 offline runtime profile eval。
- draft 输出可机器读取的安全字段，而不只是在 `expectedRuntimeAction` 里追加自然语言。
- 离线指导必须包含同 case 证据引用、报告路径模式和必须检查的 profile 门槛。
- 保持 raw prompt、raw request、raw response 和 API Key 不进入报告或草稿。

**Non-Goals:**

- 不改变真实 ModelScope 调用、预算保护、stream parser 或 retry 策略。
- 不让 offline profile eval 替代真实 live eval 的质量判断。
- 不把 request size 改善等同于 latency 一定改善。
- 不改生产 API；本轮只影响测试侧 live eval/report fixture 草稿。

## Decisions

### 在 runtime fixture draft 上新增安全指导字段

新增字段放在 `LiveEvalRuntimeFixtureDraft`，而不是 `LiveModelEvalReport.Entry`。Entry 表示真实调用事实，draft 表示“应该沉淀成什么回归样本/下一步动作”。offline profile eval 是 quota fallback 后的行动建议，因此属于 draft 层。

字段保持最小：

- `offlineProfileEvalRecommended`
- `offlineProfileReportPattern`
- `offlineProfileCaseId`
- `offlineProfileRequiredChecks`

这些字段都不包含 prompt、response、request body 或 provider payload。

### 只在 quota-limited live-model-eval 上启用

Factory 已经有 `failureType` 分类。只有 `QUOTA_LIMIT` 才填充 offline profile 指导，避免 timeout、validation、slow response 等不同问题误导维护者去看 request size。

### 自然语言行动建议消费结构字段

`expectedRuntimeAction` 和 `iterationSuggestion` 会继续存在，但其内容来自相同判断：quota 时先处理额度，额度恢复前运行 `offline-runtime-profile-eval-*.json` 路径对应的离线报告，检查 request bytes reduced、compact、compression ratio 和结构锚点。

## Risks / Trade-offs

- [Risk] 维护者误以为 offline profile 通过就等于真实模型恢复。→ 文案和 required checks 明确只验证输入体积与结构锚点，真实质量仍需 live eval。
- [Risk] draft schema 增加字段后旧消费者忽略。→ 字段 nullable/兼容，既有 JSON 消费不受影响。
- [Risk] 非 quota 问题被错误推荐 offline eval。→ 测试覆盖 budget guard/slow response 不填充 offline profile 指导。
- [Risk] 泄露上下文。→ 字段只含路径模式、caseId 和检查项；测试断言不包含 raw request、messages、sourceCode、真实 API Key 值、Bearer token 值或 `ms-` token。
