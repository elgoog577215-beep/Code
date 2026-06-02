## Why

当前 `evalReadiness` 主要回答“有没有教师纠错、课堂介入或风险样本值得沉淀”，但不会告诉教师这些样本是否能作为真实外部模型质量 baseline。最新真实 live eval 已出现 `status=PASSED` 但 `comparabilityStatus=NOT_COMPARABLE` 的情况，说明只看候选数量会误导后续 baseline 回归判断。

本变更把模型质量可比性接入评测沉淀状态，让教师端区分“可以沉淀诊断/课堂 fixture”和“还不能沉淀真实外部模型质量基线”。

## What Changes

- `AiQualityOverviewResponse.EvalReadiness` 新增模型质量 baseline 可比性字段：`modelQualityBaselineStatus`、`modelQualityBaselineSummary`、`modelQualityBaselineReasonCount`、`modelQualityBaselineReasons`。
- `AiQualityOverviewService` 复用 `runtimeAttributionSignal.qualityComparabilityStatus` 生成 eval readiness 的模型质量基线建议。
- 当可比性为 `NOT_COMPARABLE` 时，`evalReadiness.status` 可继续保留 `READY/PARTIAL`，但新增字段明确提示不要把当前样本当成真实外部模型质量 baseline。
- 教师工作台“评测沉淀”块展示模型质量 baseline 状态、摘要和最多两条原因。
- 不改变 live eval baseline regression gate、外部模型调用或现有 eval candidate 判定。

## Capabilities

### New Capabilities
- `model-comparability-eval-readiness`: 覆盖 AI 质量概览的评测沉淀状态如何表达真实外部模型质量 baseline 可比性。

### Modified Capabilities
- `ai-quality-feedback-loop`: `evalReadiness` 除了候选数量，还需要输出模型质量 baseline 是否可用于真实外部模型对比。
- `online-education-agent-quality`: 评测沉淀闭环必须区分模型成功、partial、fallback 和不可比状态。

## Impact

- 后端 DTO：`AiQualityOverviewResponse.EvalReadiness`
- 后端服务：`AiQualityOverviewService`
- 后端测试：`AiQualityOverviewServiceTest`
- 前端类型和教师页：`frontend/src/shared/api/types.ts`、`frontend/src/features/teacher/TeacherPage.tsx`
- 样式：复用 `teacher-ai-comparability`
- 验证：概览服务测试、前端 typecheck、OpenSpec strict validate、secret scan、`git diff --check`
