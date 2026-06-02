## Why

`live-model-eval` baseline regression report 已经能输出 `comparabilityStatus=NOT_COMPARABLE/PARTIAL/COMPARABLE`，但教师工作台当前只看到外部模型 recovery 状态。这样老师或维护者仍可能把 `recoveryStatus=BLOCKED` 下的 fallback 命中误读成真实外部模型质量可比。

本变更把“当前作业是否可以代表真实外部模型质量”提升为教师端可见的结构化信号，让恢复状态直接转化为质量对比决策。

## What Changes

- `runtimeAttributionSignal` 新增质量可比性字段：`qualityComparabilityStatus`、`qualityComparabilitySummary`、`qualityComparabilityReasonCount`、`qualityComparabilityReasons`。
- 后端根据当前作业的 `aiInvocation`、模型完成数、fallback 数和 recovery 状态派生 `COMPARABLE`、`PARTIAL`、`NOT_COMPARABLE` 或 `NOT_APPLICABLE`。
- 当 recovery blocked、真实模型命中缺失但 fallback 命中存在、或只有 partial 模型样本时，输出明确原因，避免把规则兜底误判为外部模型质量。
- 教师工作台模型归因块展示质量可比性 chip、摘要和最多两条原因。
- 不改变外部模型调用、fallback、live eval gate 或 baseline regression pass/fail 语义。

## Capabilities

### New Capabilities
- `teacher-model-quality-comparability`: 覆盖教师端 AI 质量概览如何展示当前作业的外部模型质量可比性状态和原因。

### Modified Capabilities
- `online-education-agent-quality`: 将外部模型恢复状态转化为教师可解释的质量对比决策信号。

## Impact

- 后端 API DTO：`AiQualityOverviewResponse.RuntimeAttributionSignal`
- 后端服务：`AiQualityOverviewService`
- 教师端类型与页面：`frontend/src/shared/api/types.ts`、`frontend/src/features/teacher/TeacherPage.tsx`
- 样式：`frontend/src/styles.css`
- 测试：`AiQualityOverviewServiceTest`、前端 typecheck、OpenSpec strict validate
