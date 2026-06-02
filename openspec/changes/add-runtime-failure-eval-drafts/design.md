## Context

当前系统已经完成三层外部模型运行可观测性：默认 single-call 模式、趋势中的 source segment 归因，以及 AI 质量概览的 `runtimeAttributionSignal`。这些能力能告诉教师和开发者“模型为什么没有真实完成”，但 eval 草稿导出仍只覆盖教师校正、课堂介入和提示安全。

持续目标要求优先提升真实外部模型参与后的能力，而不是只增强本地规则兜底。因此 runtime 失败和部分完成样本需要进入 fixture 草稿闭环，成为可审查、可沉淀、可回归验证的素材。

## Goals / Non-Goals

**Goals:**

- 将外部模型运行失败和部分完成样本导出为 runtime fixture 草稿。
- 复用现有运行归因类别，区分 quota、budget guard、安全拒绝、校验失败、超时、provider 错误、部分完成和未知失败。
- 给教师端提供 runtime 草稿计数、失败类别、推荐动作和 JSON 预览。
- 草稿脱敏，不输出 API Key、token 或 provider 原始敏感错误全文。
- 通过后端测试、前端 typecheck、OpenSpec 校验和差异检查验证。

**Non-Goals:**

- 不自动写入静态 eval fixture 文件，仍由教师或维护者人工审查后沉淀。
- 不改变外部模型调用策略、预算保护策略或 live eval 运行命令。
- 不重构 `AiQualityOverviewService` 的归因实现；本轮只在 `ClassroomService` 中复用同等分类语义。

## Decisions

### 新增独立 RuntimeFixtureDraft

运行归因样本不是诊断错因校正，也不是提示安全风险。新增 `RuntimeFixtureDraft` 比塞进 `SafetyFixtureDraft` 或 `FixtureDraft` 更清晰，可以直接承载 status、runtimeMode、failureStage、failureReason、failureType 和 expectedRuntimeAction。

### 从 assignment submissions 直接生成草稿

`exportDiagnosisEvalFixtureDraft` 已经读取作业内所有提交及分析。本轮直接遍历 assignment submissions，读取对应 `SubmissionAnalysis.reportJson.aiInvocation`，筛选 `MODEL_RUNTIME_FALLBACK`、`fallbackUsed=true` 和 `MODEL_PARTIAL_COMPLETED`。这样不依赖教师校正是否存在，也不会漏掉 live eval 暴露的运行问题。

### 归因类别保持与质量概览一致

草稿分类使用与 `runtimeAttributionSignal` 相同的类别：`QUOTA_LIMIT`、`BUDGET_GUARD`、`SAFETY_REJECTED`、`VALIDATION_FAILED`、`TIMEOUT`、`PROVIDER_ERROR`、`PARTIAL_COMPLETION` 和 `UNKNOWN_RUNTIME_FAILURE`。这样教师端看到的行动建议和草稿样本能互相印证。

### 脱敏优先于原始错误细节

`failureReason` 只保留单行、截断、去除明显 key/token 片段后的摘要。`mustNotMention` 明确禁止 API Key、token、完整代码、参考答案和隐藏测试点。sourceMaterial 只包含 submission、aiInvocation status、failureStage、failureType 等可定位但不泄密的 artifact。

## Risks / Trade-offs

- [Risk] 复制一份归因分类逻辑可能和质量概览产生漂移。→ 本轮使用相同类别和文案，并通过 spec 约束；后续可以抽成共享分类器。
- [Risk] failureReason 过度脱敏会降低排障细节。→ 保留失败类别、阶段、runtimeMode、submissionId 和 evidenceRefs，足够支撑 fixture 编目和回归验证。
- [Risk] runtime 草稿和安全草稿可能同时指向同一 submission。→ 两类草稿表达的验收目标不同，允许并列出现；教师端统计和 JSON 分开展示。
