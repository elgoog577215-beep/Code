## Context

当前跨作业趋势已经具备三类外部模型来源信号：

- runtime attribution：完成、partial、runtime failure、fallback、failureStage/failureReason。
- transport telemetry：stream content chunk、invalid chunk、fallback retry。
- recovery status：`RECOVERED/BLOCKED/NOT_APPLICABLE` 和 required checks。

缺口是来源片段没有把这些信号翻译成“这组样本能否用于真实外部模型质量对比”。这会让长期趋势仍停留在工程计数层，而不是教育 agent 质量决策层。

## Goals / Non-Goals

**Goals:**

- 给每个 `sourceSegment` 输出质量可比性状态、摘要和原因。
- 复用当前作业概览的状态语义：`NOT_COMPARABLE`、`PARTIAL`、`COMPARABLE`、`NOT_APPLICABLE`。
- 在教师工作台来源质量列表中用紧凑 chip 展示可比性。
- 保持新增字段可选，兼容旧数据和旧 API 调用方。

**Non-Goals:**

- 不改变 source segment 的分组键。
- 不读 live eval JSON 作为生产趋势数据源。
- 不把可比性混入模型质量分或校正率。
- 不改变 recovery 判定、fallback 策略或外部模型调用。

## Decisions

### 1. 可比性在 `SourceAccumulator` 输出阶段计算

`SourceAccumulator` 已经聚合了当前 segment 的 model completed、partial、runtime failure 和 fallback。输出 DTO 时再结合同 recovery key 的 `RecoveryAccumulator` 计算可比性，可以避免新增二次遍历，也能保证和现有 recovery 展示一致。

### 2. 使用和当前作业概览一致的原因文本

原因文本沿用：

- `current recovery blocked`
- `model hits missing; fallback hits present`
- `partial model outputs present`
- `runtime failures still present`

这样 source segment 与 live eval baseline comparability、当前作业概览 comparability 能被同一套测试和后续报告解释。

### 3. UI 复用紧凑 comparability 样式

教师工作台当前已有 `teacher-ai-comparability` 样式。本轮复用该块，在 source segment 下展示：

- `质量对比 NOT_COMPARABLE/PARTIAL/COMPARABLE`
- `原因 N`
- 一句 summary
- 最多两条原因

不新增图表或大段说明，避免跨作业趋势区域过载。

## Risks / Trade-offs

- [Risk] source segment 按 status 拆分，而 recovery 按更粗 recovery key 聚合，某个失败片段可能拿到同来源的 recovered 状态。→ 这是现有 recovery 设计刻意为之，用来表达“同一来源已出现恢复证据”；可比性也复用这个含义。
- [Risk] 长期趋势中 mixed segment 很常见，`PARTIAL` 可能增多。→ `PARTIAL` 只在 partial 或未恢复运行失败存在时输出，比直接 `NOT_COMPARABLE` 更能保留可观察进展。
- [Risk] UI 信息密度增加。→ 只展示非 `NOT_APPLICABLE` 状态，原因限制两条。
