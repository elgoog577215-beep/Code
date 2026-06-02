## Why

上一轮当前作业 AI 质量概览已经能显示 `qualityComparabilityStatus`，但跨作业趋势的 `sourceSegments` 仍只显示 recovery 和 runtime 计数。维护者需要长期判断某个 provider/model/prompt/runtime 来源到底可不可以代表真实外部模型质量，而不是只看单个作业。

本变更把质量可比性推进到跨作业来源片段，让教师工作台能按模型来源识别长期 `NOT_COMPARABLE`、`PARTIAL` 或 `COMPARABLE`。

## What Changes

- `AiQualityTrendResponse.SourceQualitySegment` 新增 `qualityComparabilityStatus`、`qualityComparabilitySummary`、`qualityComparabilityReasonCount`、`qualityComparabilityReasons`。
- `AiQualityTrendService` 在 source segment 聚合时基于 recovery、真实模型完成、partial、runtime failure、fallback 生成可比性状态。
- 教师工作台“来源质量”片段展示质量对比 chip、摘要和最多两条原因。
- 补充趋势服务测试，覆盖 blocked 来源不可比、recovered 来源可比、partial 来源部分可比。
- 不改变外部模型调用、baseline regression gate、source segment 分组键或 recovery 判定。

## Capabilities

### New Capabilities
- `trend-model-quality-comparability-segments`: 覆盖跨作业 AI 质量趋势来源片段如何输出外部模型质量可比性状态和原因。

### Modified Capabilities
- `online-education-agent-quality`: 将真实外部模型可比性从当前作业概览扩展到跨作业趋势来源片段。

## Impact

- 后端 DTO：`AiQualityTrendResponse.SourceQualitySegment`
- 后端服务：`AiQualityTrendService`
- 后端测试：`AiQualityTrendServiceTest`
- 前端类型和教师页：`frontend/src/shared/api/types.ts`、`frontend/src/features/teacher/TeacherPage.tsx`
- 样式：`frontend/src/styles.css`
- 验证：趋势服务测试、前端 typecheck、OpenSpec strict validate、secret scan、`git diff --check`
