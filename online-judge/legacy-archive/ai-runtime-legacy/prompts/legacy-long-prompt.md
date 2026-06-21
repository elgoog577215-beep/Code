# Legacy Long Prompt

旧位置：`AiReportService.enhanceSubmissionAnalysis` 中 `shouldUseExternalRuntime(...) == false` 的分支。

旧用途：直接基于 `problem/submission/fallback/evidencePackage/ruleSignals` 拼接上下文，让模型返回旧 `AiAnalysisPayload`。

旧输出字段：

```text
headline
summary
issueTags
fineGrainedTags
abilityPoints
focusPoints
fixDirections
evidenceRefs
studentHint
studentHintPlan
learningInterventionPlan
teacherNote
progressSignal
confidence
uncertainty
answerLeakRisk
wrongSolution
correctSolution
lineIssues
reportMarkdown
```

当前状态：`ARCHIVED_ONLY`。
