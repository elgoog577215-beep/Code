## Context

baseline regression report 目前有两类关键信号：

- `status=PASSED/FAILED`：是否存在相对 baseline 的质量违例。
- `comparabilityStatus=COMPARABLE/PARTIAL/NOT_COMPARABLE`：本次结果能否代表真实外部模型质量。

真实 ModelScope 429 场景下，`status=PASSED` 可能和 `comparabilityStatus=NOT_COMPARABLE` 同时出现。JSON 字段已经能表达，但控制台只打印路径，不能阻止人类或轻量脚本误读。

## Goals / Non-Goals

**Goals:**

- 提供统一的 baseline regression console summary。
- 同时展示 pass/fail 和 comparability。
- 摘要包含足够排障信息：compared cases、violations、comparability reasons。
- assistant 和 model baseline regression 使用同一格式。

**Non-Goals:**

- 不改变 JSON schema。
- 不改变 gate 失败条件。
- 不引入外部依赖或日志框架。
- 不打印 raw prompt、raw response、API key、headers 或 provider raw body。

## Decisions

### 1. 摘要方法放在 report DTO 上

`AssistantLiveEvalTest` 和 `ModelDiagnosisEvalTest` 都已经拿到 `LiveEvalBaselineRegressionReport`。把格式方法放在 DTO 上，可以避免两个测试类复制字符串拼接，也让结构测试直接断言摘要内容。

### 2. 摘要只打印安全、结构化字段

格式包含：

```text
status=PASSED, comparability=NOT_COMPARABLE, reasons=8, compared=0/0, violations=0
```

不展开完整原因列表，避免控制台过长；完整原因仍在 JSON。原因数量足以提醒使用者打开报告查看。

### 3. 保持 gate 语义不变

即使 `comparabilityStatus=NOT_COMPARABLE`，只要没有 violations，既有 `status=PASSED` 和断言仍保持。这个变更只提升可见性，不改变 CI 行为。

## Risks / Trade-offs

- [Risk] 只打印原因数量不够具体。→ 摘要目标是防误读；完整排障仍由 JSON report 承担。
- [Risk] 旧脚本解析控制台文本可能受到影响。→ 新增一行独立输出，不修改原有 report path 行。
- [Risk] assistant report 没有 recovery 字段。→ 摘要只依赖通用 comparability 字段，assistant/model 共用。
