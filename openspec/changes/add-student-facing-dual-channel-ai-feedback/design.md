## Context

现有链路是：

```text
SubmissionAnalysisService
-> DiagnosticAgentService
-> DiagnosisEvidencePackage
-> RuleSignalAnalyzer
-> ModelDiagnosisBrief + StandardLibraryPack
-> AiReportService 外接模型
-> ModelOutputNormalizer / ModelOutputValidator
-> SubmissionAnalysisResponse
```

这条链路适合保证模型不乱说、可追溯、可评测，但学生端需要更清晰的产品化输出。新能力不重写主链路，而是在模型输出和最终响应之间新增学生反馈组装层。

## Goals / Non-Goals

**Goals:**

- 学生端看到“当前错误点”和“继续提升点”两类反馈。
- 当前错误点必须优先解释 first failed case 或明确的编译/运行证据。
- 继续提升点不能覆盖主错因，不能把优化建议伪装成导致当前失败的原因。
- 外接模型输出必须绑定标准库标签、证据引用和安全规则。
- fallback 时仍返回可用本地学生反馈，但不计入外接模型 AI 能力分。

**Non-Goals:**

- 不删除旧字段，不重写前端主页面。
- 不新增数据库迁移。
- 不做完整静态代码风格分析器。
- 不把本地规则输出冒充为外接模型智能能力。

## Decisions

- `SubmissionAnalysisResponse` 新增 `StudentFeedback studentFeedback`。
- `StudentFeedback` 包含：
  - `summary`
  - `blockingIssues`
  - `secondaryIssues`
  - `improvementOpportunities`
  - `nextLearningAction`
- `blockingIssues` 至少包含一个条目；条目包含 `priority`、`title`、`studentMessage`、`evidence`、`nextAction`、`issueTag`、`fineGrainedTag`、`evidenceRefs`。
- `improvementOpportunities` 使用固定 v1 分类：`COMPLEXITY`、`TESTING_HABIT`、`CODE_CLARITY`、`BOUNDARY_AWARENESS`、`ROBUSTNESS`、`DEBUG_CLEANUP`。
- 新增 `StudentFeedbackAssembler`，从模型输出或本地诊断结果生成学生端反馈。
- 外接模型完整通过时使用模型反馈；模型诊断有效但学生提示不安全时，保留诊断并本地重写学生反馈；模型失败时使用本地反馈。
- `StandardLibraryPack` 新增 `improvementTags` 和 `studentFeedbackRules`，低延迟 profile 下保留 id/label/action 级最小字段。
- prompt 使用 `diagnosis-and-teaching-v3`，输入仍是 `brief + standardLibrary`，输出新增 `studentFeedback`，保留 `diagnosisDecision` 和 `teachingHint`。

## Student Feedback Behavior

学生端输出必须满足：

- 第一屏能说明最主要问题。
- 当前错误点先讲导致当前失败的原因。
- 次要问题只在“不应优先处理”时出现。
- 继续提升点必须是学习价值建议，例如补自测、估算复杂度、拆清读取和处理、清理调试输出。
- 下一步必须是观察、对照、追踪、估算或构造样例，不能是完整修法。

## Risks / Trade-offs

- [Risk] 输出 schema 变长影响外接模型延迟 -> 保留 low-latency 裁剪，学生反馈字段短句化。
- [Risk] 模型把提升点当主错因 -> validator 和评测指标检查 `blockingIssues` 与 `improvementOpportunities` 的分离。
- [Risk] 旧前端不消费新字段 -> 所有旧字段保留，学生反馈作为增强字段。
- [Risk] 本地 fallback 文案质量不如模型 -> v1 优先保证安全和可行动，再用 live eval 持续优化模型提示。
