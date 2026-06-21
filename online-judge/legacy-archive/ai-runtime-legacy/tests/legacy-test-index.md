# 旧链路测试索引

以下测试曾直接或间接覆盖旧 prompt、旧 stage、旧 failure reason 或旧 runtime 配置。主代码清理后，这些测试应删除、改写为新链路测试，或保留为历史展示逻辑测试。

## 旧运行链路相关

- `AiReportServiceExternalRuntimeTest`
- `PromptTemplateRegistryTest`
- `ModelOutputValidatorTest`
- `ExternalModelOutputNormalizerTest`
- `StandardLibraryPackBuilderTest` 中旧 prompt profile 断言
- `AiReportServiceSearchLocationRuntimeTest` 中旧 prompt 回退断言

## 旧评测报告相关

- `ModelDiagnosisEvalTest`
- `LiveModelEvalComparisonReportFactoryTest`
- `AssistantLiveEvalQualityGateTest`
- `AssistantLiveEvalTest`

## 教师质量看板历史 stage 相关

- `AiQualityOverviewServiceTest`
- `AiQualityTrendServiceTest`
- `ClassroomServiceCorrectionTest`

当前状态：`ARCHIVED_ONLY`。
