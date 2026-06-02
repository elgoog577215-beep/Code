## Why

真实 ModelScope live eval 已经证明 standard profile 能命中错因，但响应仍可能超过 latency budget；随后 low-latency 真实对照又遇到 `INSUFFICIENT_QUOTA`，导致无法稳定用外部调用验证 profile 输入压缩是否生效。当前系统需要一个不消耗额度、不保存 raw prompt 的离线 profile eval，作为真实 live eval 的前置门禁和额度不足时的替代证据。

## What Changes

- 新增 offline runtime profile eval，基于现有 eval cases 构造 `standard` 与 `low-latency` runtime request telemetry。
- 导出每个 case 的 profile、requestBytes、requestCompact、compressionRatio、保留的 tag/evidence/action 数量和安全边界状态。
- 生成汇总报告，用于证明 low-latency 请求体是否比 standard 更小，并在没有 ModelScope 额度时仍能回归 profile 行为。
- 不保存 raw request、raw prompt、raw response 或 API Key。
- 增加测试，验证 low-latency request 更小、仍保留验证所需的 tag/evidence/action，并能对压缩不足输出可行动失败原因。

## Capabilities

### New Capabilities

- `offline-runtime-profile-eval`: 离线生成外部 runtime profile 对比报告，验证 request size、compact 标记和质量门槛。

### Modified Capabilities

- `low-latency-external-runtime-profile`: 增加离线评测场景，证明 low-latency profile 的压缩效果不依赖真实 provider 额度。

## Impact

- 测试与评测：`ModelDiagnosisEvalTest`、新增 offline profile report DTO/factory。
- Runtime 输入构造：复用 `ExternalModelAgentRuntime.prepare`，不改变线上默认调用行为。
- 报告产物：`target/ai-eval-reports/offline-runtime-profile-eval-*.json`。
- 安全：只记录长度、计数、profile 和布尔门槛，不记录 raw request 内容或密钥。
