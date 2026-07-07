## 1. 架构准备

- [ ] 1.1 创建 `frontend/src/features/teacher-analytics/` 模块目录，包含 pages、components、charts、model、selectors 和样式入口。
- [ ] 1.2 定义统一前端分析模型：`AnalyticsScope`、`AnalyticsGranularity`、`AnalyticsMetric`、`InsightBucket`、`KnowledgePathNode` 和证据样本类型。
- [ ] 1.3 梳理现有 `AssignmentOverview`、学生提交、诊断标签、标准库字段到新分析模型的映射关系。
- [ ] 1.4 建立数据 selector，先用现有 API 生成班级、作业、题目三层分析数据。

## 2. 递进路由和导航

- [ ] 2.1 新增班级分析路由 `/app/teacher/classes/:classId`。
- [ ] 2.2 新增作业分析路由 `/app/teacher/classes/:classId/assignments/:assignmentId`。
- [ ] 2.3 新增题目分析路由 `/app/teacher/classes/:classId/assignments/:assignmentId/problems/:problemId`。
- [ ] 2.4 实现递进面包屑，展示班级、作业、题目的当前位置并支持回到上层。
- [ ] 2.5 调整 `TeacherShell` 导航，让正式班级/作业入口进入新分析主线。

## 3. 共享分析组件

- [ ] 3.1 实现 `GranularitySelector`，支持章节、知识点、能力点、易错点平行切换。
- [ ] 3.2 实现 `AnalyticsSummaryCards`，展示提交数、正确率、错误次数、涉及学生数等客观指标。
- [ ] 3.3 实现轻量 `AnalyticsBarChart`，用于排行和分布数据。
- [ ] 3.4 实现轻量 `AnalyticsPieChart`，用于占比数据。
- [ ] 3.5 实现 `KnowledgePathTooltip`，支持悬浮或选中展示完整知识路径。
- [ ] 3.6 实现 `AiKnowledgeInsightPanel`，只展示知识归因、错因分布、路径、证据和标准库归属状态。
- [ ] 3.7 实现 `EvidenceSamples`，展示涉及题目、学生和典型提交证据。

## 4. 页面实现

- [ ] 4.1 实现 `ClassAnalyticsPage`，展示班级基础指标、作业列表、长期错误分布和班级 AI 知识归因。
- [ ] 4.2 实现 `AssignmentAnalyticsPage`，展示作业基础指标、各题提交/正确率图表、作业错因分布和作业 AI 知识归因。
- [ ] 4.3 实现 `ProblemAnalyticsPage`，展示题目基础指标、通过/未通过占比、易错点分布、知识路径和提交证据样本。
- [ ] 4.4 确保三层页面共享同一套粒度切换、图表组件、AI 归因面板和证据组件。
- [ ] 4.5 为数据不足、AI 不可用、无提交、无知识路径等状态提供客观空状态。

## 5. 旧主线迁移与淘汰

- [ ] 5.1 从旧 `AssignmentDetailPage` 迁移有价值的提交数、正确率、趋势、学生提交证据和诊断标签展示能力。
- [ ] 5.2 将教师校正能力迁移到题目分析页的提交证据样本层，不再作为正式主线入口。
- [ ] 5.3 移除或重定向旧作业详情、旧题目详情和旧学生诊断正式入口。
- [ ] 5.4 清理旧教师动作、讲评建议、优先处理等正式 UI 文案和相关入口。
- [ ] 5.5 确保教师端不会同时暴露新旧两套主线。

## 6. i18n、样式和验证

- [ ] 6.1 为所有新增教师分析文案同步维护 `public/locales/zh/translation.json` 与 `public/locales/en/translation.json`。
- [ ] 6.2 更新样式，保持教师端简洁、现代、数据密度适中，并避免卡片堆叠和决策式话术。
- [ ] 6.3 更新或新增前端 typecheck、build、visual smoke 和 browser smoke 覆盖新主线。
- [ ] 6.4 检查中英文模式下无中文残留、文字遮挡、布局溢出或新旧入口混杂。
- [ ] 6.5 运行 `openspec validate --all`，确认规格和实现任务保持一致。
