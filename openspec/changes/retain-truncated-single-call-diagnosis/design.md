## Context

`AI_EVAL_MAX_OUTPUT_TOKENS=384` 的真实 smoke 产生了典型截断样本：stream 返回了 501 个 chunk，其中 content chunk 78、reasoning chunk 422，`streamFinishReason=length`，原始内容中 `diagnosisDecision` 已经完整，但 `teachingHint` 在字段中途被截断。上一轮已经把这类失败归因为 `OUTPUT_TRUNCATED`，但仍会走整单 runtime fallback。

当前 single-call 路径先调用 `parseModelStagePayload(content, CombinedOutput.class)`。根 JSON 解析失败时返回 null，后续诊断校验得到 `EMPTY_RESPONSE`，再由 telemetry 修正为 `OUTPUT_TRUNCATED`。这个流程保留了失败原因，但没有利用已经完整的诊断裁决。

## Goals / Non-Goals

**Goals:**

- 在 `finish_reason=length` 且根 `CombinedOutput` 解析失败时，尽可能保留完整、可校验的 `diagnosisDecision`。
- 保留后的响应状态为 `MODEL_PARTIAL_COMPLETED`，表示错因裁决来自外部模型，教学提示由本地安全模板补齐。
- 失败阶段仍指向 `DIAGNOSIS_AND_TEACHING`，失败原因为 `OUTPUT_TRUNCATED`，方便 runtime draft 和教师端理解原因。
- live eval 报告能单独统计 partial completion，避免把 partial 误算为 full completion。

**Non-Goals:**

- 不尝试修复或续写被截断的 `teachingHint`。
- 不保存 raw provider payload 或原始 chunk。
- 不接受不完整、不安全、标签非法或证据非法的诊断裁决。
- 不改变默认 token 预算。

## Decisions

### 在 single-call 层做受限子对象提取

截断抢救只服务 `CombinedOutput`，不扩展成通用 JSON repair。实现上从原始内容定位 `"diagnosisDecision"` 字段，找到其后第一个 `{`，再用字符串状态机按花括号深度提取完整对象。状态机需要处理字符串和转义，避免被中文提示或字符串中的括号误导。

替代方案是使用宽松 JSON parser 或正则。宽松 parser 引入依赖和误接受风险；正则无法可靠处理嵌套对象和字符串转义。因此采用小范围平衡括号扫描。

### 必须复用现有 normalize + validate

提取出来的 `DiagnosisJudgeOutput` 必须经过 `normalizeDiagnosisDecision` 和 `validateDiagnosisDecision`。只有通过校验才进入 partial response；否则保持 runtime fallback。这保证抢救路径不会绕过标准库、证据引用和安全边界。

### 把截断视为 teaching 阶段未完成

虽然根 stage 是 `DIAGNOSIS_AND_TEACHING`，但输出截断实际导致教学提示不可用。保留诊断后，系统用 `buildPartialRuntimeAnalysisResponse` 生成本地安全教学模板，并在 `aiInvocation.failureReason` 中保留 `OUTPUT_TRUNCATED`。这样既不丢掉外部模型诊断，也不把不完整 teaching 当作可用教学输出。

### live eval 增加 partial 计数

现有 `completedCount` 只看 `fallbackUsed=false`，会把 `MODEL_PARTIAL_COMPLETED` 算作完整完成。本轮新增 `partialCount`，并让 `completedCount` 只统计 `MODEL_COMPLETED`。这让真实评测能区分 full completion、partial retention 和 fallback。

## Risks / Trade-offs

- [Risk] 子对象提取误接受截断内容。→ 只有括号完整闭合且 Jackson 可解析时才接受。
- [Risk] 诊断裁决完整但证据或标签不合法。→ 继续走 runtime fallback，不保留 partial。
- [Risk] partial 让总体 fallback 降低但教学质量仍依赖本地模板。→ live eval 单独统计 partial，不把它算成 full completion。
- [Risk] 真实 provider 的输出字段顺序变化。→ 只依赖字段名 `diagnosisDecision`，不依赖其在根对象中的顺序。
