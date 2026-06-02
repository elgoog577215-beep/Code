## Context

最近的真实 report 显示：

- `assistant-live-eval` 已能导出 baseline，并通过 `AI_EVAL_BASELINE_REPORT` 做逐 case regression gate。
- `live-model-eval` 已能导出 `qualityBaselineDrafts`，但 baseline 主要由 `expectedIssueTagHit`、`expectedFineTagHit`、`evidenceValid` 和 `safetyPassed` 组成。
- `LiveModelEvalReport.Entry` 没有实际标签和证据 refs，导致 model baseline 无法沉淀“具体错因标签 + 证据引用”的成功样本。

为了真正提升外接大模型的诊断质量闭环，model smoke 需要能回答：同一个 case 的外部模型这次是否仍保留上次成功的细粒度诊断和证据链。

## Goals / Non-Goals

**Goals:**

- 在 live-model-eval entry 中记录实际标签和证据 refs。
- 从成功 model entry 生成结构化 baseline 草稿。
- 使用上一份 model report 的 baseline 草稿检查当前 model report 逐 case 退化。
- 默认不阻塞普通 smoke，仅在显式设置 baseline report 时启用。
- 结构测试无需真实 ModelScope key。

**Non-Goals:**

- 不自动选择最新 baseline 文件，避免误用 assistant report 或错误模型版本。
- 不做自然语言逐字回归；model baseline gate 优先检查结构化标签、证据和质量布尔值。
- 不改变生产诊断链或 prompt。

## Decisions

### 单独使用 AI_EVAL_MODEL_BASELINE_REPORT

assistant report 和 model report schema 不同，baseline 语义也不同。使用 `AI_EVAL_MODEL_BASELINE_REPORT` 显式指向上一份 `live-model-eval-*.json`，避免与 `AI_EVAL_BASELINE_REPORT` 混用。

### model gate 只检查结构化 mustKeep

旧 model baseline 可能包含 `outputSummary` 这种自然语言片段，容易因为合理表达变化产生误报。本轮 model gate 将重点放在：

- `expectedIssueTagHit`
- `expectedFineTagHit`
- `evidenceValid`
- `safetyPassed`
- `issue:*`
- `fine:*`
- evidence ref

后续如果要对表达质量做回归，应新增更明确的 rubric 字段，而不是复用自然语言摘要。

### 复用 LiveEvalBaselineRegressionGate

baseline regression 的核心职责一致：按 caseId 匹配当前 entry 和 baseline draft，输出具体 violation。复用同一个 test helper 能减少重复，同时为 assistant 和 model 保持相似失败消息。

## Risks / Trade-offs

- [Risk] 旧 baseline 只有弱布尔 mustKeep。→ gate 仍能检查 fallback、标签命中、证据有效和安全；新 baseline 会更强。
- [Risk] report 字段变多。→ 仅 test/eval report DTO 增字段，不影响生产 API。
- [Risk] 真实模型调用耗时。→ 结构测试覆盖大部分逻辑，真实 smoke 只跑默认 2 条并显式 baseline。
