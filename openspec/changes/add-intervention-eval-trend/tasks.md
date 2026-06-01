## 1. 响应结构

- [x] 1.1 扩展 `AiQualityTrendResponse` 总览字段，加入 intervention eval candidate、waiting、improved、shifted、still stuck 计数。
- [x] 1.2 扩展 `AssignmentQualityPoint` 对应字段。
- [x] 1.3 更新前端 `AiQualityTrend` 和 `AiQualityTrendPoint` 类型。

## 2. 趋势计算

- [x] 2.1 让 `AiQualityTrendService` 读取 `ClassReviewFeedbackRepository`。
- [x] 2.2 基于反馈后提交和诊断标签计算 intervention 成效状态。
- [x] 2.3 汇总跨作业总数和 assignment point 数值。
- [x] 2.4 更新趋势 summary，优先提示 still stuck 和可沉淀 intervention fixture。

## 3. 验证

- [x] 3.1 扩展 `AiQualityTrendServiceTest`，覆盖 improved、still stuck 和 waiting followup 聚合。
- [x] 3.2 运行 OpenSpec 严格校验、相关后端测试、后端编译、前端 typecheck 和 diff 检查。
