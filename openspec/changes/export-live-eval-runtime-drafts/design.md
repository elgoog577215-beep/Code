## Context

当前 live eval 有两类报告：

- `AssistantLiveEvalReport`：覆盖提交诊断、Coach 追问和成长报告，记录 assistantType、status、fallbackUsed、completedOutput、failureStage、failureReason、teacherExpectation 和 iterationSuggestion。
- `LiveModelEvalReport`：覆盖模型诊断评测，记录 stage、status、fallbackUsed、jsonValid、failureStage、failureReason 和输出摘要。

上一轮 `add-runtime-failure-eval-drafts` 已经为课堂 API 响应新增 runtime fixture 草稿，但它的数据源是数据库里的 `SubmissionAnalysis.aiInvocation`。live eval 报告通常存在于 `target/ai-eval-reports`，不能当生产数据源，却非常适合在测试产物里直接附带“下一步可沉淀草稿”。

## Goals / Non-Goals

**Goals:**

- 在 live eval JSON 报告顶层输出 runtime 草稿列表和计数。
- 让 assistant live eval 和 model diagnosis live eval 共享同一个草稿分类与脱敏逻辑。
- 区分 quota、budget guard、安全拒绝、结构校验失败、timeout、provider error、partial completion、quality miss 和 unknown runtime failure。
- 草稿能直接指导维护者把失败样本沉淀成 fixture 或先处理额度/预算保护问题。
- 不需要真实 API Key 也能通过结构测试验证草稿生成。

**Non-Goals:**

- 不把 `target/ai-eval-reports` 作为生产数据源。
- 不自动写入 `src/test/resources` 静态 fixture 文件。
- 不改变 live eval 的调用规模、阈值或预算保护策略。
- 不把 API Key、token 或 provider 原始错误全文写进草稿。

## Decisions

### 新增 test-scope 共享 DTO/Factory

新增 `LiveEvalRuntimeFixtureDraft` 和 `LiveEvalRuntimeFixtureDraftFactory`。两个报告类都只持有 DTO 列表，不复制分类逻辑。Factory 提供 `fromAssistantEntries(...)` 和 `fromModelEntries(...)`，让两条 live eval 链路共享脱敏、failureType、行动建议和 evidenceRefs 生成规则。

### 报告顶层携带草稿

草稿放在 `runtimeFixtureDrafts` 顶层，而不是塞进每条 entry。这样报告查看者可以先看 summary，再直接拿到“需要人工沉淀”的样本清单；原始 entries 仍保留完整评测明细。

### 草稿选择规则

草稿覆盖：

- `fallbackUsed=true`
- `completedOutput=false`
- `status=MODEL_RUNTIME_FALLBACK`
- `status=MODEL_PARTIAL_COMPLETED`
- `status=EXCEPTION`
- `failureReason` 包含 `SAFETY_REJECTED`、`QUALITY_MISS` 或 `TEACHING_ACTION_MISMATCH`

其中 runtime 失败和 partial 是第一优先级；quality miss 进入草稿是为了提示“模型已完成但教学质量需要 fixture 化”，不会混同为 runtime failure。

### 脱敏与截断

Factory 对 `failureReason`、`outputSummary`、`outputDetail` 统一做单行化、截断和敏感片段 redaction。`mustNotMention` 固定包含 API Key、token、密钥、完整代码、参考答案和隐藏测试点。

## Risks / Trade-offs

- [Risk] test-scope DTO 与生产 DTO 有重复字段。→ 两者来源和用途不同，先保持隔离；后续如需要导入教师端，再抽共享 schema。
- [Risk] quality miss 被误读为运行失败。→ 草稿保留 `failureType=QUALITY_MISS` 或 `TEACHING_ACTION_MISMATCH`，并在 expectedRuntimeAction 中明确先优化 fixture/prompt 质量，而不是处理 provider。
- [Risk] live eval 报告可能变大。→ 草稿只保留摘要和最多若干 evidence refs，不复制完整输出。
