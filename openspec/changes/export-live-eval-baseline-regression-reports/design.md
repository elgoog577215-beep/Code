## Context

当前外接模型评测链路已经具备两类 baseline gate：

- `assistant-live-eval` 通过 `AI_EVAL_BASELINE_REPORT` 读取上一份 assistant report 的 `qualityBaselineDrafts`。
- `live-model-eval` 通过 `AI_EVAL_MODEL_BASELINE_REPORT` 读取上一份 model report 的 `qualityBaselineDrafts`。

两者都能把退化转换为 violation，但 violation 只在断言失败时展示。对长期追踪 ModelScope 外部模型效果而言，还需要一份稳定 JSON 产物，让人或 CI 后续能读取“这次对比的上下文和结果”。

## Goals / Non-Goals

**Goals:**

- 为 assistant 和 live-model-eval baseline gate 统一产出结构化对比报告。
- 报告必须包含 baseline report、current report、case 覆盖数、对比数、violation 数、状态和 violation 明细。
- 报告必须在 gate 断言前写出，使失败场景也能留下排查产物。
- 无 baseline 环境变量时保持现有行为，不额外写对比报告。
- 结构测试不依赖真实 ModelScope key。

**Non-Goals:**

- 不自动选择最新 baseline，避免跨模型、跨 smoke 范围或跨 report 类型误用。
- 不引入数据库、前端 UI 或生产 API。
- 不改变 baseline gate 的判定规则；本轮只把判定结果结构化沉淀。

## Decisions

### 使用单独的 regression report DTO

新增 `LiveEvalBaselineRegressionReport`，避免把对比元信息塞回 assistant/model 原始 report。原始 report 表达“本次模型输出”，regression report 表达“本次与 baseline 的比较结果”，职责分离更清楚，也便于后续被 CI 或质量看板消费。

### gate 调用方负责写文件

`AssistantLiveEvalTest` 和 `ModelDiagnosisEvalTest` 已经知道当前 report 的实际路径，也负责读取 baseline 路径。因此写对比报告放在调用方，而不是放进 `LiveEvalBaselineRegressionGate`。这样 gate 仍保持纯函数，只输出 violations。

### 失败前写出报告

调用顺序为：读取 baseline -> 计算 violations -> 写 regression report -> assert empty。这样即使断言失败，`target/ai-eval-reports` 下也会保留失败详情。

## Risks / Trade-offs

- [Risk] 报告数量增多。→ 仅在显式配置 baseline 环境变量时写出，普通 smoke 不增加产物。
- [Risk] case 覆盖数被误读为质量分。→ 报告明确区分 `baselineCaseCount`、`currentCaseCount`、`comparedCaseCount` 和 `violationCount`，不伪造分数。
- [Risk] 当前 report 路径为空时难以追溯。→ live eval 先写当前 report，再执行 baseline gate，因此 regression report 总能记录 current report path。
