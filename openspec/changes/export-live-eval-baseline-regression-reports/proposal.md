## Why

live eval baseline gate 已经能在真实外部模型 smoke 中拦截退化，但当前失败信息只存在于测试断言里。对持续提升外接大模型能力来说，质量回归需要留下可审计产物：本次 report 和哪份 baseline 比、覆盖了多少 case、是否通过、具体 violation 是什么。

本变更把 baseline gate 从“测试里的一次性断言”推进为“断言 + 可沉淀的结构化对比报告”。

## What Changes

- 新增 baseline regression report DTO，统一表达 assistant 和 live-model-eval 的 baseline 对比结果。
- assistant live eval 在设置 `AI_EVAL_BASELINE_REPORT` 时，除执行 gate 外，写出 `assistant-live-eval-baseline-regression-*.json`。
- live-model-eval 在设置 `AI_EVAL_MODEL_BASELINE_REPORT` 时，除执行 gate 外，写出 `live-model-eval-baseline-regression-*.json`。
- 对比报告记录 baseline 路径、当前 report 路径、baseline/current/compared case 数、violation 数、状态和 violation 明细。
- 新增无需 API Key 的结构测试，验证报告摘要不会依赖真实模型调用。

## Capabilities

### New Capabilities

- `live-eval-baseline-regression-report`: 覆盖 live eval baseline gate 如何产出可审计的结构化对比报告。

### Modified Capabilities

- `live-eval-baseline-regression-gate`: assistant baseline gate 增加对比报告产物。
- `live-model-eval-baseline-regression-gate`: model baseline gate 增加对比报告产物。

## Impact

- 测试工具：新增 baseline regression report DTO / factory。
- 评测链路：`AssistantLiveEvalTest`、`ModelDiagnosisEvalTest`。
- 测试：`AssistantLiveEvalQualityGateTest`。
- 产物：`target/ai-eval-reports/*-baseline-regression-*.json`。
- 生产代码：不改动生产服务、API 或数据库。
