## Context

当前外部模型质量链路已经分成两层：

- live eval / baseline regression report 用 `comparabilityStatus` 解释一份评测报告是否能代表真实外部模型质量。
- 教师工作台当前作业概览用 `runtimeAttributionSignal` 和 `recoveryStatus` 解释模型调用是否恢复。

两层之间仍有表达缺口：教师工作台知道“恢复 BLOCKED”，但不知道这意味着“当前样本不可用于判断外部模型诊断质量”。本轮需要把 recovery 和 runtime attribution 翻译成教师可见的质量可比性信号，同时保持生产接口不依赖本地 `target/ai-eval-reports` 文件。

## Goals / Non-Goals

**Goals:**

- 在当前作业 AI 质量概览中输出外部模型质量可比性状态。
- 将 `recoveryStatus=BLOCKED`、model completed 缺失、fallback 命中、partial 样本转成结构化原因。
- 让教师工作台模型归因块直接显示“质量对比不可比/部分可比/可比”。
- 保持字段可选和向后兼容，老前端或老后端可以忽略新增字段。

**Non-Goals:**

- 不读取 live eval JSON 作为生产数据源。
- 不改变 baseline regression report 的 `status=PASSED/FAILED` 语义。
- 不改变外部模型调用、重试、fallback、预算保护或 recovery 判定。
- 不把质量可比性当作最终诊断质量分，只解释“当前证据是否足以比较真实外部模型质量”。

## Decisions

### 1. 可比性从当前作业运行证据派生

`AiQualityOverviewService` 已经能读取当前作业所有 `SubmissionAnalysis.reportJson.aiInvocation`，并汇总 model completed、partial、runtime failure 和 fallback。可比性直接复用这些指标和 `RecoveryStatusSummary`：

- `NOT_COMPARABLE`：recovery blocked，或没有真实模型完成样本但存在 fallback 命中。
- `PARTIAL`：存在 partial 模型样本，或真实模型完成样本很少但仍有运行失败。
- `COMPARABLE`：存在真实模型完成样本，recovery recovered，且没有当前作业级阻塞原因。
- `NOT_APPLICABLE`：当前作业没有外部模型运行上下文，或没有运行归因信号需要解释。

这样可以和 live eval 的 comparability 语义对齐，但不引入报告文件依赖。

### 2. 原因列表保留机器可断言文本

字段使用短字符串原因，例如：

- `current recovery blocked`
- `model hits missing; fallback hits present`
- `partial model outputs present`
- `runtime failures still present`

教师端展示最多两条，API 保留更多原因，便于测试、日志和后续沉淀 eval fixture。

### 3. 教师端只增加紧凑状态块

教师工作台已有模型归因、transport chips 和 recovery chips。本轮只在同一块下新增 comparability chips 与短说明：

- `质量对比 NOT_COMPARABLE`
- `原因 3`
- 摘要一句话
- 前两条原因

不新增独立页面、弹窗或大面积说明文案，避免教师端被研发细节淹没。

## Risks / Trade-offs

- [Risk] 当前作业样本少时，`COMPARABLE` 可能过于乐观。→ 只有 recovery recovered 且存在真实模型完成样本时才给 `COMPARABLE`；混合失败时降为 `PARTIAL`。
- [Risk] 英文原因对非研发教师不够友好。→ UI 用中文标题说明，原因保留精确文本给维护者对照 eval；后续可增加本地化标签。
- [Risk] `NOT_APPLICABLE` 和 `HEALTHY` 容易混淆。→ `NOT_APPLICABLE` 只表示没有足够外部模型质量对比上下文，不等于模型质量优秀。
- [Risk] 与 live eval comparability 语义漂移。→ 使用相同核心原因命名，并在测试里覆盖 blocked、recovered、healthy 三类场景。
