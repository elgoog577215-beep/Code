## Context

当前 live model eval 同时承担两件事：验证外部模型质量，以及验证 runtime 输入/传输配置是否合理。真实 provider 可用时这很直接；但额度不足或 provider 抖动时，系统无法判断 low-latency profile 到底有没有实际减少 request body。上一轮已新增 `runtimeProfile/requestBytes/requestCompact`，但 requestBytes 只有调用发生后才记录。现在需要一个离线报告，在不发送 HTTP 请求的情况下复用同一套 runtime plan 构造逻辑，生成可回归的 request-size 对比。

## Goals / Non-Goals

**Goals:**

- 不调用 ModelScope，也能比较 `standard` 和 `low-latency` 的 serialized request bytes。
- 报告每个 eval case 的压缩比例、compact 标记、candidate/evidence/tag/action 保留情况。
- 提供质量门槛：low-latency 必须更小，且保留非空 tag/evidence/action 与 hidden boundary。
- 报告不包含 raw request、prompt、source code 或 API Key。
- 让 live eval 在额度不足时仍有一条可执行的“先验证 profile 输入体积”路径。

**Non-Goals:**

- 不替代真实 live eval 的模型质量判断。
- 不把离线 request size 改善等同于 latency 一定改善。
- 不改变 production 默认 profile。

## Decisions

### 用测试侧 factory 复用 runtime plan，而不是暴露线上 raw request

新增测试侧 `OfflineRuntimeProfileEvalReport` 和 factory。Factory 调用 `ExternalModelAgentRuntime.prepare(..., profile)`，然后按真实 chat completion 结构序列化为 request body，仅记录字节数和结构计数，丢弃 raw JSON。这保证离线报告和线上 request 构造一致，同时避免持久化敏感上下文。

### 用质量门槛约束 compact profile

每个 case 输出 `compressionRatio`、`requestBytesReduced`、`qualityPreserved`、`failureReasons`。`qualityPreserved` 只检查结构保留：candidateSignals/evidenceRefs/issueTags/teachingActions/hiddenDataBoundary。模型输出质量仍由 live eval 判断。

### 作为 quota blocker 的前置证据

当 live eval 因 `INSUFFICIENT_QUOTA` 失败时，offline profile eval 可以证明本轮代码是否至少让 low-latency 输入变小，并给下一次额度恢复后的真实 A/B 提供 baseline。

## Risks / Trade-offs

- [Risk] request size 下降不必然降低推理时延。→ 报告明确只证明输入体积；真实 latency 仍由 live eval 验证。
- [Risk] 过度压缩导致质量下降但离线 eval 看不出。→ 只把离线 eval 当结构门槛，不替代 issue/fine/evidence/safety live eval。
- [Risk] 报告意外包含 prompt。→ DTO 不包含 raw request 字段，测试断言 JSON 不含 `messages`、`sourceCode`、`api_key` 等敏感标记。
