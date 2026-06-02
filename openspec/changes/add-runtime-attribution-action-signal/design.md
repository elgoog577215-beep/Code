## Context

前序变更已经完成两件事：

- `aiInvocation` 记录 `runtimeMode`、`failureStage` 和 `failureReason`。
- AI 质量趋势和教师端来源片段展示模型完成、部分完成、运行失败和失败归因。

但当前 `AiQualityOverviewService` 的 `MODEL_RUNTIME` 维度仍使用统一文案：“优先检查模型调用、prompt 输出稳定性和预算保护状态”。这不足以区分真实故障：额度不足应检查 ModelScope 额度或降级评测规模；预算保护打开应检查最近连续失败和冷却；安全拒绝应回到提示安全 fixture；校验失败应回到 prompt 契约和输出解析。

## Goals / Non-Goals

**Goals:**

- 从当前作业的 `SubmissionAnalysis.reportJson.aiInvocation` 中提取运行失败归因。
- 输出结构化 `runtimeAttributionSignal`，包含主导失败类别、原因、阶段、计数、摘要、推荐动作和证据。
- 让 `MODEL_RUNTIME` 质量维度和优先改进项复用该信号。
- 在教师工作台 AI 质量摘要区展示运行归因和行动建议。

**Non-Goals:**

- 不改变外部模型调用、重试、预算保护或 fallback 行为。
- 不新增数据库字段。
- 不解析或展示 API key、provider 原始错误全文。
- 不把 live eval 报告文件作为生产数据源。

## Decisions

### 1. 运行归因信号从当前作业样本实时派生

`runtimeAttributionSignal` 由 `AiQualityOverviewService` 根据当前作业的 `analyses` 派生。它不依赖 live eval 文件，也不需要迁移历史数据。旧报告没有 `failureReason` 时仍可通过 `status` 和 `fallbackUsed` 归入未知运行失败。

### 2. 用枚举式类别承接不同故障动作

失败分类使用字符串类别：

- `QUOTA_LIMIT`: `INSUFFICIENT_QUOTA`、`quota`
- `BUDGET_GUARD`: `BUDGET_GUARD_OPEN`
- `SAFETY_REJECTED`: `SAFETY_RISK`、`SAFETY_REJECTED`
- `TIMEOUT`: `TIMEOUT`
- `VALIDATION_FAILED`: `INVALID_TAG`、`VALIDATION`、`JSON`
- `PROVIDER_ERROR`: `RATE_LIMITED`、`HTTP`、`PROVIDER`
- `PARTIAL_COMPLETION`: `MODEL_PARTIAL_COMPLETED`
- `UNKNOWN_RUNTIME_FAILURE`: 兜底分类

分类只输出概括原因，不保留 provider 原始错误详情，避免把外部错误文本或潜在敏感内容带到教师端。

### 3. MODEL_RUNTIME 维度消费归因信号

当存在 `runtimeAttributionSignal` 时：

- 维度 summary 使用信号摘要。
- recommendedAction 使用类别化动作。
- evidenceRefs 使用信号证据。

没有失败信号时保持原有健康文案。

### 4. 前端只展示摘要和动作

教师工作台在 AI 质量摘要 action 区新增“模型归因”块，展示主导类别、摘要和推荐动作。详细来源片段仍由跨作业趋势负责，当前作业概览只给“该怎么处理”。

## Risks / Trade-offs

- [Risk] 失败原因字符串来自多处旧逻辑，命名可能不统一。→ 使用包含式匹配并保留 `UNKNOWN_RUNTIME_FAILURE`。
- [Risk] 教师端信息过多。→ 只展示主导类别、摘要和动作，证据细节留在质量详情与来源片段。
- [Risk] 运行失败和部分完成同时存在。→ 运行失败优先级高于部分完成；若无运行失败但有部分完成，则主导类别为 `PARTIAL_COMPLETION`。
- [Risk] 旧数据没有新增字段。→ 信号字段可空，前端类型为可选。
