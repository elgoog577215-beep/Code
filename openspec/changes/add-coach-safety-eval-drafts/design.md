## Context

`DiagnosisEvalFixtureDraftResponse` 已经包含 `safetyFixtureCount` 和 `safetyFixtures`。`ClassroomService.buildSafetyFixtureDrafts(...)` 当前从两类来源构建安全草稿：诊断报告中的高 `answerLeakRisk` 和 `HintSafetyCheck` 的安全降级记录。教师端 Fixture 草稿预览已经能展示这些安全草稿，也能把 `safetyFixtures` 放进 JSON 预览。

上一轮 Coach 安全拒绝改造新增了 `CoachPrompt.modelFailureReason` 和 `modelAnswerLeakRisk`，并在班级 Coach 质量汇总中显示安全回退计数。本轮需要把这些真实安全拒绝事件纳入 eval 草稿导出。

## Goals / Non-Goals

**Goals:**

- 将 `SAFETY_REJECTED` Coach prompt 转换为 `SafetyFixtureDraft`。
- 与同 submissionId 的诊断高泄题风险和提示安全降级合并成一条草稿。
- 给草稿提供 Coach 专属 risk source、blocked reason、evidenceRefs 和 evalPurpose。
- 不暴露原始不安全模型草稿全文，避免把泄题内容写进导出预览。

**Non-Goals:**

- 不新增新的 DTO 字段或前端卡片。
- 不改变 Coach prompt 的保存策略。
- 不自动把草稿写入静态 fixture 文件，仍由教师人工审查。

## Decisions

### 复用 SafetyDraftAccumulator

扩展现有 `SafetyDraftAccumulator`，加入 `coachPrompts` 与 Coach risk source。这样同一个 submissionId 下的多类安全信号会自然合并，教师只审查一条汇总草稿。

### 在 export 时按 assignment submissions 查询 CoachPrompt

`exportDiagnosisEvalFixtureDraft` 已经拿到了作业内所有 submissionId。新增一步通过 `coachPromptRepository.findBySubmissionIdIn(...)` 读取 prompt，再筛选 `modelFailureReason=SAFETY_REJECTED`。这避免新增仓储方法，也与现有分析器读取 prompt 的方式保持一致。

### 草稿只展示安全 fallback 与风险元数据

Coach 安全拒绝没有保存原始不安全模型草稿，本轮也不补存。草稿的 `safeHintPreview` 使用最终安全规则追问，`originalHintPreview` 只用“Coach 模型追问草稿已被安全门拒绝”这类脱敏描述。

## Risks / Trade-offs

- [Risk] 不保存原始越界草稿会降低人工复盘细节。→ 保留风险来源、模型风险等级、prompt id 和 submission id，满足 fixture 编目与回归沉淀；泄题文本不进入导出结果。
- [Risk] 多来源合并后某类风险被摘要淹没。→ `riskSources`、`blockedReasons` 和 `sourceMaterial.artifacts` 都保留来源级信息。
- [Risk] 作业 prompt 较多时导出读取范围变大。→ 本轮只按作业 submissionId 查询，且草稿最终仍限制前 8 条安全样本。
