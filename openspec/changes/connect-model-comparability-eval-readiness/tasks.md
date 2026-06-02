## 1. 后端 eval readiness

- [x] 1.1 扩展 `AiQualityOverviewResponse.EvalReadiness`，新增模型质量 baseline 状态、摘要、原因数量和原因列表。
- [x] 1.2 调整 `AiQualityOverviewService`，让 `buildEvalReadiness` 消费 `runtimeAttributionSignal.qualityComparabilityStatus`。
- [x] 1.3 扩展 `AiQualityOverviewServiceTest`，覆盖 fixture ready 但模型 baseline blocked、recovered baseline ready 和 partial baseline。

## 2. 教师工作台展示

- [x] 2.1 扩展前端 `AiQualityEvalReadiness` 类型。
- [x] 2.2 在“评测沉淀”块展示模型质量 baseline 状态、摘要和最多两条原因。
- [x] 2.3 复用紧凑可比性样式，避免增加大段说明文案。

## 3. 验证

- [x] 3.1 运行 `AiQualityOverviewServiceTest`。
- [x] 3.2 运行前端 typecheck。
- [x] 3.3 运行 OpenSpec strict validate、secret scan 和 `git diff --check`。
