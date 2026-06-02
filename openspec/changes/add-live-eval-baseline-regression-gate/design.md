## Context

上一轮 `qualityBaselineDrafts` 已经让成功样本带上：

- `caseId`
- `assistantType`
- `expectedSignals`
- `evidenceRefs`
- `mustKeep`
- `teachingAction`
- `regressionPurpose`

但 `AssistantLiveEvalQualityGate` 当前只检查汇总率，不读取 baseline。为了不让 test/eval 产物侵入生产链路，本轮保持 test-scope gate，专门在 live eval 运行后对比 baseline 报告。

## Goals / Non-Goals

**Goals:**

- 支持读取上一份 assistant live eval JSON 中的 `qualityBaselineDrafts`。
- 对当前 report 的同 case entry 做回归检查。
- 输出具体、可行动的退化原因。
- 通过环境变量启用，默认不阻塞普通 smoke。
- 结构测试无需真实 ModelScope key。

**Non-Goals:**

- 不支持 model diagnosis report 的 baseline gate；本轮先覆盖 assistant 多助手报告。
- 不自动选择最新 baseline 文件，避免误用错误报告。
- 不要求完整输出逐字匹配，只检查结构化 mustKeep。

## Decisions

### 独立 Gate 而不是扩展汇总 Thresholds

baseline regression 是逐 case、逐字段比较，和现有 rate threshold 语义不同。新增 `LiveEvalBaselineRegressionGate` 可以保持门禁职责清晰。

### 环境变量显式开启

使用 `AI_EVAL_BASELINE_REPORT=/path/to/report.json` 显式启用，避免日常 smoke 因找错 baseline 被阻塞。

### mustKeep 的匹配策略

baseline 的 `mustKeep` 可能包含结构化 token、evidence ref、教学动作和摘要片段。gate 先在当前 entry 的结构化字段和输出文本中查找；对于 `live_eval_case:`、`issue:`、`fine:`、`teachingAction:` 这类结构化前缀使用对应字段匹配，避免只靠文本包含。

## Risks / Trade-offs

- [Risk] baseline 太旧导致合理改进被判退化。→ 通过显式传入 baseline 文件控制版本；失败信息可指导更新 baseline。
- [Risk] mustKeep 中自然语言片段过于脆弱。→ 主要优先匹配结构化前缀；自然语言只作为辅助。
- [Risk] 只覆盖 assistant report。→ 当前 assistant report 覆盖提交诊断、Coach、成长报告，先解决最大面；model report 后续可复用 gate 思路。
