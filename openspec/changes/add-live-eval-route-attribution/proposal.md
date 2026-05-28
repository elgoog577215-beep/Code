## Why

外部模型评测已经能区分模型输出与本地兜底，也已经支持主路由和备用路由，但报告条目还不能明确展示每条样本实际使用了哪个供应商和模型。没有路由归因，后续即使配置备用模型，也难以判断是哪个路由提升了完成率、哪个路由仍在触发配额或限流。

本变更用于把 live eval 从“是否完成”推进到“由哪个外部路由完成、失败发生在哪个路由”，让外部模型能力优化可以继续基于证据迭代。

## What Changes

- 在 `AssistantLiveEvalReport.Entry` 中增加路由归因字段，记录实际 provider、model 和 route role。
- 诊断类 live eval 从 `AiInvocation` 中提取实际 provider/model，而不是只使用评测默认模型。
- Coach 和成长报告类 live eval 在无法拿到更细调用元数据时，使用当前评测路由默认值并显式标注 route role，避免误以为已有完整归因。
- 增加报告回读和路由归因测试，确保 UTF-8 JSON 与新增字段可被后续自动分析稳定使用。
- 不改变生产诊断输出、不改变学生可见内容、不改变模型调用策略。

## Capabilities

### New Capabilities

- `live-eval-route-attribution`: 外部模型 live eval 报告能够按条目记录模型路由归因，并支持后续按路由分析完成率和失败原因。

### Modified Capabilities

无。

## Impact

- 影响测试与评测报告结构：`AssistantLiveEvalReport`、`AssistantLiveEvalTest`、相关 quality gate 测试。
- 影响 live eval JSON 消费方：新增字段为兼容性增加，不删除旧字段。
- 不引入新依赖，不改变线上接口。
