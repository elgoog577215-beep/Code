## Why

真实 ModelScope low-latency live eval 当前可能因 `INSUFFICIENT_QUOTA` 失败，而系统已经有不消耗额度的 offline runtime profile eval 可以验证同一批 case 的 request 压缩与结构锚点。现在缺少一条从 quota fallback runtime draft 指向离线 profile 证据的结构化桥接，导致维护者仍需要人工记住下一步该跑哪个报告。

## What Changes

- live-model-eval runtime fixture draft 在识别 `QUOTA_LIMIT` 时输出 offline profile eval 建议。
- 为 runtime draft 增加安全的离线 profile 指导字段：是否推荐离线 profile eval、报告路径模式、同 case evidence ref、必须检查的 profile 门槛。
- 行动建议必须明确：先处理 ModelScope 额度；额度恢复前先运行 offline runtime profile eval 验证 `low-latency` request bytes、compact 标记和结构锚点。
- 扩展无需真实 API Key 的结构测试，覆盖 quota fallback 的 offline profile 指导和非 quota 失败不误报。
- 不保存 raw request、raw prompt、raw response 或 API Key。

## Capabilities

### New Capabilities

- `live-eval-quota-offline-profile-guidance`: live eval quota fallback 草稿能把真实额度失败转化为可执行的离线 profile 评测下一步。

### Modified Capabilities

- `export-live-eval-runtime-drafts`: runtime fixture draft schema 增加安全离线 profile 指导字段，并在 live-model-eval quota fallback 时填充。

## Impact

- 测试 DTO：`LiveEvalRuntimeFixtureDraft`
- 测试 factory：`LiveEvalRuntimeFixtureDraftFactory`
- 测试：`AssistantLiveEvalQualityGateTest`、`ModelDiagnosisEvalTest`
- OpenSpec：新增 quota offline profile guidance 规格，扩展 runtime draft 导出规格。
- 非目标：不改变真实 ModelScope 调用、预算保护、stream parser、offline profile report 生成逻辑或生产接口。
