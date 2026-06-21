# 旧 AI 运行链路资产清单

本清单记录从主程序中移出的旧 AI 诊断运行资产。所有条目的当前状态均为 `ARCHIVED_ONLY`：只保留档案，不参与编译、运行、测试或配置。

| 资产 | 来源 | 旧作用 | 旧依赖 | 当前状态 |
|---|---|---|---|---|
| legacy long prompt | `AiReportService.enhanceSubmissionAnalysis` | 缺少 evidence/runtime 时直接让模型生成旧 `AiAnalysisPayload` | `AiAnalysisPayload`、行号解析、本地 fallback | ARCHIVED_ONLY |
| staged runtime | `AiReportService.enhanceWithExternalRuntime` | 两次调用：诊断裁决后再生成教学提示 | `DiagnosisJudgeOutput`、`TeachingHintOutput` | ARCHIVED_ONLY |
| old single-call runtime | `AiReportService.enhanceWithSingleCallRuntime` | 一次调用生成 `CombinedOutput` | `diagnosis-and-teaching-*`、`CombinedOutput` | ARCHIVED_ONLY |
| truncated single-call recovery | `retainDiagnosisDecisionFromTruncatedSingleCall` | 输出被截断时尝试提取 `diagnosisDecision` | 旧 single-call JSON 结构 | ARCHIVED_ONLY |
| old prompt registry | `PromptTemplateRegistry` | 注册 `diagnosis-judge-*`、`teaching-hint-*`、`diagnosis-and-teaching-*` | 旧 runtime 选择逻辑 | ARCHIVED_ONLY |
| old runtime selector | `ExternalModelAgentRuntime.prepare` | 允许 old prompt version 和 staged prompt | `diagnosisPrompt`、`teachingPrompt`、`singleCallPromptVersion` | ARCHIVED_ONLY |
| old output contracts | `ExternalModelStagePayloads` | 定义旧模型输出 DTO | `DiagnosisJudgeOutput`、`TeachingHintOutput`、`CombinedOutput` | ARCHIVED_ONLY |
| old output normalizer | `ExternalModelOutputNormalizer` | 归一化旧 DTO 与旧 feedback | 旧 DTO、旧 validator | ARCHIVED_ONLY |
| old output validator | `ModelOutputValidator` | 校验旧诊断裁决、教学提示、旧学生反馈 | 旧 DTO、旧标准 tag | ARCHIVED_ONLY |
| old runtime tests | 多个旧测试类 | 验证旧 prompt/stage/fallback 行为 | 旧 prompt/stage 名 | ARCHIVED_ONLY |

## 不归档删除的底座

以下组件属于新链路底座，不是旧链路：

- `DiagnosisEvidencePackageBuilder`
- `RuleSignalAnalyzer`
- `ModelDiagnosisBriefBuilder`
- `StandardLibraryPackBuilder`
- `SearchLocationRetrievalService`
- `SearchLocationOutputValidator`
- `SearchLocationPackSelector`
- `AdviceGenerationOutput`
- `AdviceGenerationOutputValidator`
- `AdviceGenerationFeedbackMapper`
