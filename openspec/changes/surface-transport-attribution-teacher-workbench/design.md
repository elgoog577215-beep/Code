## Context

当前教师工作台的 AI 质量区域已经展示：

- 作业级 AI 质量摘要；
- 优先改进项；
- 模型运行归因；
- 跨作业趋势；
- 来源质量片段。

后端已经提供 `runtimeAttributionSignal.primaryTransportMode`、`streamNoContentCount`、`streamInvalidChunkCount`、`streamFallbackRetryCount`，趋势 source segment 也有同名 transport telemetry 字段。缺口在于前端没有把这些字段转成可扫描的教师视图。

## Goals / Non-Goals

**Goals:**

- 让教师在作业级“模型归因”里看到 transport mode 和 stream 计数。
- 让教师在跨作业“来源质量”里看到每个 source segment 的传输层状态。
- 用简短、无密钥、无 raw response 的文案表达外部模型接入状态。
- 保持现有工作台紧凑信息架构。

**Non-Goals:**

- 不新增后端字段。
- 不新增独立路由或弹窗。
- 不展示 raw prompt、raw response、SSE chunk 内容、API key 或 header。
- 不把 transport telemetry 误写成模型诊断质量分数。

## Decisions

### 作业级归因使用短标签

在“模型归因”卡片中保留原有 summary 和 recommendedAction，同时追加 transport chip：

- `通道 stream`
- `无内容 N`
- `解析异常 N`
- `重试 N`

这样教师能先看主导原因，再看传输层证据。

### 来源质量片段使用二级元信息

source segment 已经有版本、provider、status、runtimeMode、failureStage/failureReason。transport telemetry 作为同一片段内的二级元信息展示，不作为新的分组或独立卡片，避免列表过度切碎。

### 空 telemetry 不展示

老数据没有 transport 字段时，不展示 transport chip。这样不会让教师误以为所有历史样本都缺少传输数据。

## Risks / Trade-offs

- [Risk] 信息密度过高。→ 使用短 chip，并只在有 telemetry 时显示。
- [Risk] 教师误解 no-content 为模型推理质量差。→ chip 文案使用“无内容/解析异常/通道”，不写“模型差”。
- [Risk] 前端字段可选。→ 所有访问都做空值和 0 值兼容。
