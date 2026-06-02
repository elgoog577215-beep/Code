## Context

系统已经记录 `streamFinishReason`，但它只作为 telemetry 展示。低 token 预算实验产生了明确证据：stream content chunk 已返回，`finishReason=length`，解析失败导致诊断 fallback。此时 failureReason 仍是 `EMPTY_RESPONSE`，runtime draft 也只能给未知失败建议。

## Goals / Non-Goals

**Goals:**

- 把 `finish_reason=length` 识别为输出截断。
- 让截断进入 `aiInvocation.failureReason`、live eval report、runtime draft、教师 fixture draft 和质量概览归因。
- 行动建议指向 token 预算/schema/context/分阶段调用，而不是 quota、provider 或未知失败。
- 不泄露 raw response 或 prompt。

**Non-Goals:**

- 不自动扩大 token 预算。
- 不实现 JSON 修复或续写调用。
- 不改变安全校验、标签校验和 evidence 校验规则。

## Decisions

### 通过 telemetry 修正校验失败原因

`parseModelStagePayload` 当前返回 null，后续 validator 报 `EMPTY_RESPONSE`。这符合“对象为空”的局部事实，但对运行归因不够精确。更好的位置是在 fallback 前，根据 `lastCallTelemetry.streamFinishReason` 将 `EMPTY_RESPONSE/INVALID_JSON/UNKNOWN_ERROR` 修正为 `OUTPUT_TRUNCATED`。

### Runtime draft 使用独立 failureType

`OUTPUT_TRUNCATED` 不应混入 `VALIDATION_FAILED`，因为它的处理动作不同：不是泛泛修 schema，而是调整输出预算、减少字段或拆成 staged runtime。独立分类能让低延迟 profile 的实验结果更清晰。

### 保留 finish reason 证据

runtime draft 的 mustMention 和 source artifacts 继续保留 `streamFinishReason=length`，但不保存 raw chunk 内容。

## Risks / Trade-offs

- [Risk] 有些 `length` 截断仍可能伴随 schema 设计问题。→ 行动建议同时包含提高 token 预算和收缩 schema/上下文。
- [Risk] 旧报告没有该字段。→ 分类兼容旧 failureReason，缺失 finish reason 时仍走原分类。
- [Risk] 真实 provider 偶发行为不同。→ 结构测试固定归因，真实 smoke 只作为当前外部状态证据。
