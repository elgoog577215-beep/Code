## Context

`CoachAgentService.generate(...)` 会在模型追问草稿不安全时返回规则 fallback，并把 fallback 的 `failureReason` 设置为 `SAFETY_REJECTED`。`CoachPromptService` 随后只保存最终问题、rationale、contextSummary 和 evidenceRefs。由于 `CoachPrompt` 没有保存模型失败原因和模型安全风险，后续 `CoachInteractionAnalyzer` 只能分析学生回答是否越界，无法识别 AI 追问草稿曾被安全拒绝。

## Goals / Non-Goals

**Goals:**

- 持久化 Coach 模型草稿被安全拒绝的结构化信号。
- 在 Coach prompt API 响应中返回模型失败原因和模型 `answerLeakRisk`。
- 在 Coach 交互摘要中输出安全拒绝计数、最近原因、证据引用和 `needsTeacherAttention`。
- 保持现有规则 fallback 行为不变：学生仍只看到安全的规则追问。

**Non-Goals:**

- 不新增新的安全判定策略。
- 不引入新的安全事件表。
- 不修改前端展示。
- 不改变学生回答质量分析中的 `SAFETY_RISK` 语义。

## Decisions

### 在 CoachPrompt 上保存模型安全元数据

新增 nullable 字段：

- `modelFailureReason`
- `modelAnswerLeakRisk`

这样无需新表，也不影响已有 prompt 记录。`promptType` 仍表示最终给学生的问题来源，安全回退时仍是规则追问。

### 交互摘要区分两类安全风险

`CoachAnswerQualitySignal.SAFETY_RISK` 继续表示学生回答疑似越界；新增 `CoachSafetyRejectionSignal` 表示 AI 模型追问草稿被安全门拒绝。两者都可要求教师关注，但来源不同。

### 证据引用使用 prompt 与 submission 级别

安全拒绝事件没有保存原始不安全草稿全文，避免把风险内容持久化。摘要证据使用 `coach_prompt:<id>` 和 `coach_safety_rejection:submission:<submissionId>`。

## Risks / Trade-offs

- 不保存原始不安全草稿，教师不能复盘具体泄露内容。→ 这是有意取舍，避免把泄题内容固化到数据库；后续可做脱敏摘要或只保存 blocked reason。
- 仅通过 `SAFETY_REJECTED` 识别安全拒绝。→ 当前服务已经统一设置该 failure reason，本轮先消费稳定信号。
- 前端暂不展示新字段。→ 先把后端结构打稳，后续可把它纳入 AI 质量概览或教师工作台。
