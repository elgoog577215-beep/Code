## 设计

### Fixture 结构

新增 `prompt-safety-cases.json`，每条样本包含：

- `name`、`source`、`caseId`
- `problem`、`submission`、`caseResults`
- `unsafeAnalysis`：模拟模型或运行时产生的越界提示内容
- `expected`：包含 `riskLevel`、`blockedReasons`、`expectedSafetyAction`、`mustNotMention`、`requiredEvidenceRefs`
- `sourceMaterial` 和 `quality`

资源重点是安全边界，不要求重新跑诊断 agent 得出错因。

### Loader

`PromptSafetyEvalFixtureLoader` 负责读取默认资源，并提供：

- `toProblem()`
- `toSubmission()`
- `toCaseResults()`
- `toUnsafeAnalysis()`

`toUnsafeAnalysis()` 生成包含不安全 `studentHint`、`studentHintPlan`、`learningInterventionPlan` 和 `reportMarkdown` 的 `SubmissionAnalysisResponse`，再交给 `HintSafetyService.verifyAndRecord`。

### 测试口径

新增测试：

1. fixture 能加载，且每条样本都包含风险等级、blockedReasons、forbidden phrases、evidenceRefs 和安全动作。
2. 每条 fixture 经 `HintSafetyService` 处理后：
   - `answerLeakRisk` 至少达到 fixture 期望风险等级。
   - 输出不包含 `mustNotMention`。
   - `studentHintPlan.teachingAction` 和 `learningInterventionPlan.interventionType` 降级为 `COLLECT_EVIDENCE`。
   - 输出保留或生成可验证的学习动作，而不是完整答案。

### 兼容性

所有内容只在测试资源和测试代码中新增，不影响生产接口。
