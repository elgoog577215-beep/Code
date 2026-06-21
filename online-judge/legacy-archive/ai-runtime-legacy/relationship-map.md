# 旧 AI 运行链路关系图

## 旧入口 1：legacy long prompt

```text
enhanceSubmissionAnalysis(problem, submission, fallback, null, null)
-> canCallAi()
-> 构造 Map context
-> legacy long systemPrompt
-> chatCompletion()
-> AiAnalysisPayload
-> SubmissionAnalysisResponse
```

旧问题：

- 不经过 `ModelDiagnosisBrief`。
- 不经过搜索定位。
- 不经过 `diagnosis-and-advice-v1`。
- 会让无 evidence/runtime 的调用静默进入旧模型协议。

## 旧入口 2：staged runtime

```text
ExternalModelAgentRuntime.prepare()
-> diagnosisPrompt = diagnosis-judge-v2
-> teachingPrompt = teaching-hint-v1

AiReportService.enhanceWithExternalRuntime()
-> callDiagnosisJudgeStage()
-> validateDiagnosisDecision()
-> callTeachingHintStage()
-> validateTeachingHint()
-> buildRuntimeAnalysisResponse()
```

旧问题：

- 两阶段旧协议不是当前“搜索定位 + 完整建议生成”正式链路。
- failureStage 会产生 `DIAGNOSIS_JUDGE`、`TEACHING_HINT`。
- 教学表达依赖 `TeachingHintOutput`，与新 advice 结构重复。

## 旧入口 3：old single-call runtime

```text
external-runtime-mode = single-call
external-single-call-prompt-version = diagnosis-and-teaching-*
-> callSingleCallRuntimeStage()
-> CombinedOutput
-> diagnosisDecision + teachingHint + studentFeedback
-> buildRuntimeAnalysisResponse()
```

旧问题：

- prompt version 可配置回到 `diagnosis-and-teaching-v1/v2/v3/v4-lite`。
- 无效 prompt 会静默回退到 `diagnosis-and-teaching-v3`。
- failureStage 会产生 `DIAGNOSIS_AND_TEACHING`。

## 新正式链路

```text
DiagnosisEvidencePackage
-> RuleSignalAnalyzer
-> ModelDiagnosisBrief
-> SearchLocationRetrievalService
-> search-location-v1
-> selected StandardLibraryPack
-> diagnosis-and-advice-v1
-> AdviceGenerationOutputValidator
-> AdviceGenerationFeedbackMapper
-> SubmissionAnalysisResponse
```

## 删除后关系

- 主程序不再注册旧 prompt。
- 主程序不再产生旧 stage。
- 主程序不再读取 `legacy-archive`。
- 旧资产只供人工复盘和后续手动提取。
