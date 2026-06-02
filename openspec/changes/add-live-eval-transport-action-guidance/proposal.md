## Why

最新真实 `live-model-eval` smoke 已经把 `transportMode=stream` 与 `streamContentChunkCount=0` 写入 entry 和 runtime fixture draft，但 runtime 草稿的 `expectedRuntimeAction` 仍只给出“检查额度”的泛化建议。这样会让评测报告拥有传输证据，却没有把证据转化为下一步可执行的外部模型调试动作。

## What Changes

- live-model-eval runtime fixture draft 根据 transport telemetry 生成更精确的行动建议。
- 当 quota fallback 同时伴随 stream no-content 时，行动建议必须同时提示检查 ModelScope 额度和用最小 stream smoke 验证 content chunk。
- 当 stream invalid chunk 或 fallback retry 出现时，行动建议必须指向 SSE/JSON 解析或重试链路验证。
- 扩展无需真实 API Key 的结构测试，覆盖 transport telemetry 驱动的行动建议。

## Capabilities

### New Capabilities

- `live-eval-transport-action-guidance`: live eval runtime fixture 草稿把外部模型传输 telemetry 转换成可执行行动建议。

### Modified Capabilities

- `export-live-eval-runtime-drafts`: live-model-eval runtime fixture draft 的行动建议消费 transport telemetry。

## Impact

- 测试工具：`LiveEvalRuntimeFixtureDraftFactory`
- 测试：`AssistantLiveEvalQualityGateTest`
- OpenSpec：新增 live eval transport action guidance spec
- 非目标：不改变真实 ModelScope 调用策略、stream parser、fallback retry 或 API Key 配置。
