## 1. 后端 readiness 结构

- [x] 1.1 扩展 `AiQualityOverviewResponse.EvalReadiness`，新增 intervention candidate、waiting、improved、shifted、still stuck 计数和 recommendedAction。
- [x] 1.2 扩展前端 `AiQualityEvalReadiness` 类型。

## 2. readiness 计算

- [x] 2.1 让 `AiQualityOverviewService.buildEvalReadiness` 接收教师介入 impacts 和班级策略 signal。
- [x] 2.2 统计可沉淀 intervention candidate 和等待后续证据数量。
- [x] 2.3 更新 readiness status、summary、evidenceRefs 和 recommendedAction。

## 3. 教师端展示

- [x] 3.1 在 AI 质量面板评测沉淀区展示课堂介入候选数量。
- [x] 3.2 展示 `evalReadiness.recommendedAction`。

## 4. 验证

- [x] 4.1 扩展 `AiQualityOverviewServiceTest`，覆盖课堂介入成效使 readiness 为 READY。
- [x] 4.2 扩展 `AiQualityOverviewServiceTest`，覆盖等待后续证据使 readiness 为 PARTIAL。
- [x] 4.3 运行 OpenSpec 严格校验、相关后端测试、后端编译、前端 typecheck 和 diff 检查。
