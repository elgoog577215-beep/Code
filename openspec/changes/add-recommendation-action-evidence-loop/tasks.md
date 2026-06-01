## 1. 行动证据模型

- [x] 1.1 新增推荐行动证据 analyzer，按 recommendation token 生成 outcome。
- [x] 1.2 在 `RecommendationEffectivenessResponse` 新增 `actionEvidenceSignals`。
- [x] 1.3 在前端 API 类型新增兼容字段。

## 2. 推荐效果与质量闭环

- [x] 2.1 更新 `RecommendationEffectivenessService` 使用 action evidence 计算 feedback signals。
- [x] 2.2 更新 `AiQualityMetrics` 和 `AiQualityOverviewService`，让 `RECOMMENDATION_LOOP` 消费 action evidence。
- [x] 2.3 保持旧计数、rate、segments 和 evidence refs 兼容。

## 3. 下一轮推荐消费

- [x] 3.1 更新 `StudentRecommendationService`，使用 action evidence 判断未兑现推荐行动。
- [x] 3.2 当 action evidence 显示同类错因未解决或高风险失败时，优先降级复盘或教师介入。

## 4. 测试

- [x] 4.1 扩展 `RecommendationEffectivenessServiceTest`，覆盖 fulfilled、unresolved、no submission、teacher attention outcome。
- [x] 4.2 扩展 `StudentRecommendationServiceTest`，覆盖下一轮推荐消费 action evidence。
- [x] 4.3 扩展 `AiQualityOverviewServiceTest`，覆盖 `RECOMMENDATION_LOOP` evidence refs 来自行动证据。

## 5. 验证

- [x] 5.1 运行 `openspec validate add-recommendation-action-evidence-loop --strict`。
- [x] 5.2 运行推荐和 AI 质量相关后端测试。
- [x] 5.3 运行后端编译、前端 typecheck 和 `git diff --check`。
