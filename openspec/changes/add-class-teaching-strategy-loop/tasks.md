## 1. 后端结构与策略分析

- [x] 1.1 在 `AssignmentOverviewResponse` 新增 `classTeachingStrategySignal` 及分组/证据相关 DTO 字段。
- [x] 1.2 新增 `ClassTeachingStrategyAnalyzer`，从学生级信号、班级错因、能力弱点和讲评建议生成班级策略。
- [x] 1.3 将 analyzer 接入 `ClassroomService.getAssignmentOverview`，保证无提交或证据不足时返回兼容状态。

## 2. AI 质量闭环

- [x] 2.1 扩展 `AiQualityMetrics`，统计班级策略信号、可执行策略、缺证据策略、分组和退出题覆盖。
- [x] 2.2 将 `ClassTeachingStrategyAnalyzer` 接入 `AiQualityOverviewService`。
- [x] 2.3 新增 `CLASS_TEACHING_STRATEGY_LOOP` 质量维度、优先级排序、摘要、证据引用和建议动作。

## 3. 前端展示

- [x] 3.1 更新 `frontend/src/shared/api/types.ts`，补充班级教学策略类型。
- [x] 3.2 在教师工作台新增紧凑策略区，展示状态、聚焦点、教师动作、退出题和分组摘要。
- [x] 3.3 更新质量维度标签/排序文案，识别 `CLASS_TEACHING_STRATEGY_LOOP`。

## 4. 验证

- [x] 4.1 新增 `ClassTeachingStrategyAnalyzerTest`，覆盖全班小讲评、小组复盘、分层支持和证据不足。
- [x] 4.2 扩展 `ClassroomServiceCorrectionTest`，断言作业概览返回班级策略信号。
- [x] 4.3 扩展 `AiQualityOverviewServiceTest`，断言 AI 质量概览包含班级策略闭环维度。
- [x] 4.4 运行 OpenSpec 严格校验、相关后端测试、后端编译、前端 typecheck 和 diff 检查。
