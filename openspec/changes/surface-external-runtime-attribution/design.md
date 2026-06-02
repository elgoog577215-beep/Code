## Context

当前外部模型调用链已经能在 `aiInvocation` 中记录：

- `status`: `MODEL_COMPLETED`、`MODEL_PARTIAL_COMPLETED`、`MODEL_RUNTIME_FALLBACK` 等。
- `fallbackUsed`: 是否使用规则兜底。
- `runtimeMode`: `single-call`、`staged` 或旧路径标识。
- `failureStage`: 失败发生在哪个模型阶段。
- `failureReason`: 额度不足、限流、安全风险、校验失败等原因。

`AiQualityOverviewService` 已经有模型完成、部分完成和运行失败统计，但跨作业趋势 `AiQualityTrendService` 仍只把来源片段按 provider/model/prompt/status 聚合。结果是教师可以知道“有 fallback”，却看不到 fallback 是否来自真实外部模型额度不足、single-call 教学阶段校验失败，还是本地规则路径。

## Goals / Non-Goals

**Goals:**

- 让 AI 质量趋势顶层和作业趋势点暴露模型完成、部分完成、运行失败和运行失败率。
- 让来源质量片段暴露运行模式、失败阶段、失败原因和对应计数。
- 让教师工作台能直接看到模型运行问题，而不需要阅读 live eval JSON。
- 保持旧报告兼容：缺失新增字段时不破坏趋势响应。

**Non-Goals:**

- 不修改外部模型调用策略，不改变 single-call/staged 的运行行为。
- 不新增数据库字段；继续从 `SubmissionAnalysis.reportJson.aiInvocation` 读取。
- 不提交或展示 API key。
- 不重新设计教师工作台整体布局。

## Decisions

### 1. 复用 `aiInvocation` 作为唯一归因来源

趋势服务继续通过 `DiagnosisReportReader.aiInvocation` 读取归因字段。这样后端趋势、教师端和 live eval 使用同一份结构化证据，避免从中文摘要、trace 或异常文本二次解析。

备选方案是从 live eval 报告文件读取归因，但生产教师端不应依赖本地测试产物，因此不采用。

### 2. 趋势顶层和作业点使用概览层一致口径

模型运行失败按以下条件计数：

```text
aiInvocation.status == MODEL_RUNTIME_FALLBACK
OR aiInvocation.fallbackUsed == true
```

部分完成按 `MODEL_PARTIAL_COMPLETED` 计数，完成按 `MODEL_COMPLETED` 计数。这个口径与 `AiQualityMetrics` 已有统计保持一致，避免同一批样本在概览和趋势中数字不一致。

### 3. 来源片段按运行归因拆分

source segment 的 key 新增 `runtimeMode`、`failureStage` 和 `failureReason`。这样同一 provider/model/prompt 在成功和失败时不会被揉成一个片段，老师可以看出“DeepSeek single-call 成功两条，但 DIAGNOSIS_AND_TEACHING 阶段额度不足一条”。

旧数据缺字段时使用空字符串或 `unknown` fallback，只影响归因精度，不影响响应生成。

### 4. 教师端展示聚焦可行动信号

趋势顶部新增“模型失败”和“部分完成”读数；作业趋势点在出现运行失败或部分完成时显示 badge；来源片段展示运行模式、失败阶段和失败原因。这样老师能先看到异常，再决定是否检查额度、提示安全或模型校验。

## Risks / Trade-offs

- [Risk] source segment 数量可能因为失败原因拆分而增加。→ 继续限制展示前 8 个后端片段、前 3 个前端片段，并按样本数排序。
- [Risk] 旧数据没有 `runtimeMode`、`failureStage`、`failureReason`。→ 字段保持可空，趋势服务用兼容 fallback。
- [Risk] 运行失败率与 fallback 率容易被混淆。→ DTO 同时保留 `fallbackCount`，新增 `modelRuntimeFailureCount` 明确表示模型运行失败或兜底样本。
- [Risk] 教师端指标变多导致拥挤。→ 只增加两个顶部读数和条件 badge，不扩大页面结构。
