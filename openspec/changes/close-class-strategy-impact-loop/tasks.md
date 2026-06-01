## 1. 后端策略反馈与成效

- [x] 1.1 为 `ClassTeachingStrategySignal` 增加 `strategyKey` 和 `impact` DTO。
- [x] 1.2 让 `ClassTeachingStrategyAnalyzer` 生成稳定 `strategyKey` 和策略证据标签。
- [x] 1.3 新增 `ClassTeachingStrategyImpactAnalyzer`，判断无反馈、忽略、等待后续、改善、转移和仍卡同类问题。
- [x] 1.4 将策略 impact 接入 `ClassroomService.getAssignmentOverview`。

## 2. 质量闭环

- [x] 2.1 扩展 `AiQualityMetrics`，统计策略反馈、等待后续、改善、转移、仍卡和升级信号。
- [x] 2.2 将策略 impact 接入 `AiQualityOverviewService` 的 `CLASS_TEACHING_STRATEGY_LOOP`。
- [x] 2.3 更新质量维度摘要、分数、证据引用和推荐动作。

## 3. 前端反馈与展示

- [x] 3.1 更新前端 API 类型，补充策略 key 和 impact。
- [x] 3.2 在教师策略区展示 impact 状态、摘要和推荐动作。
- [x] 3.3 在教师策略区增加采纳、调整、忽略按钮，复用现有反馈接口。

## 4. 验证

- [x] 4.1 新增 `ClassTeachingStrategyImpactAnalyzerTest`，覆盖无反馈、忽略、等待后续、改善、转移、仍卡。
- [x] 4.2 扩展 `ClassroomServiceCorrectionTest`，断言策略反馈后概览返回 impact。
- [x] 4.3 扩展 `AiQualityOverviewServiceTest`，断言质量维度消费策略 impact。
- [x] 4.4 运行 OpenSpec 严格校验、相关后端测试、后端编译、前端 typecheck 和 diff 检查。
