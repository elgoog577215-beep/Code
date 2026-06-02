## Context

已有链路：

- live eval report 输出 `recoveryStatus`；
- AI 质量概览 `runtimeAttributionSignal` 输出当前作业 recovery 状态；
- 教师工作台模型归因块展示当前作业 recovery 状态。

缺口在跨作业趋势：`sourceSegments` 已经按 source/provider/model/prompt/status/runtime/failure 聚合，但只展示失败和传输层计数，没有恢复状态。对于真实外部模型接入，维护者需要知道某个模型来源是否仍然阻塞，还是已经有完成样本证明恢复。

## Goals / Non-Goals

**Goals:**

- 在 source segment 中输出 recovery 状态和阻塞原因。
- 让教师工作台来源质量片段展示 recovery chip。
- 保持与当前作业概览相同的恢复检查语义：完成、未 fallback、model hit、证据存在、安全不高风险；stream 场景要求 content chunk。

**Non-Goals:**

- 不改变 source segment 分组规则。
- 不改变外部模型调用、fallback 或 live eval gate。
- 不展示 raw provider 响应、API Key、headers 或完整模型输出。

## Decisions

### 1. 每个 source segment 独立判定 recovery

趋势已经按 `sourceSegmentKey` 聚合。recovery 状态也在该聚合范围内判定，避免一个健康模型版本掩盖另一个失败模型版本。

### 2. 状态定义沿用当前作业概览

- `RECOVERED`：片段内存在恢复上下文，并且至少一个完成样本满足恢复检查。
- `BLOCKED`：片段内存在 fallback/partial/runtime failure，但没有完成样本满足恢复检查。
- `NOT_APPLICABLE`：片段内没有恢复上下文。

### 3. 前端只展示短摘要

来源质量列表本身已经信息密集。前端展示 recovery chips 和最多两条 blocked reasons，不展示完整检查列表；完整 required checks 保留在 API 中。

## Risks / Trade-offs

- [Risk] 现有 source segment key 包含 status/failure，completed 样本可能和 failed 样本落在不同片段。→ 本轮保持现有分组规则，不跨片段合并，避免改变趋势语义；这会让失败片段显示 BLOCKED，完成片段显示 NOT_APPLICABLE 或 RECOVERED。
- [Risk] 趋势判断无法像 live eval 一样知道 expected tag。→ 使用结构化错因标签非空作为在线 model hit 近似。
- [Risk] UI 信息过多。→ 只展示 chip 和两条原因。
