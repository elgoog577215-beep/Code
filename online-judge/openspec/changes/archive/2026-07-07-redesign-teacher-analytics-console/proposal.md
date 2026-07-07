## Why

当前教师端仍以“作业详情、学生诊断、校正、教师动作”为主线，容易把 AI 信息做成替老师决策的面板，也会让班级、作业、题目三层对象和章节、知识点、能力点、易错点四种统计粒度混在一起。

新的教师端需要重构为“班级 -> 作业 -> 题目”的递进分析系统：老师先确定教学对象，再在当前对象范围内查看客观数据、图表和 AI 知识归因。

## What Changes

- **BREAKING**: 教师端正式主线从旧的作业详情/教师动作模型切换为新的教学结果分析模型。
- **BREAKING**: 旧的教师端作业详情、题目详情、学生诊断页面不再作为正式入口继续扩展；有价值的数据字段和证据能力迁入新分析主线。
- 新增递进路由和页面层级：班级分析页、作业分析页、题目分析页。
- 新增统一分析框架：当前范围、统计粒度、指标卡、图表、AI 知识归因、证据样本。
- 新增统计粒度切换：按章节、按知识点、按能力点、按易错点查看同一范围内的错误分布。
- 新增知识路径浮层：悬浮或选中图表中的知识点、能力点或易错点时展示完整路径、涉及题目、涉及学生和证据样本。
- 调整 AI 展示边界：AI 信息只作为知识归因、错因分布、路径解释和证据聚合，不出现“下一步、建议、讲评、应当处理”等替老师决策的话术。
- 保留并迁移旧教师端中有价值的资产：教师登录、教师侧栏、API client、i18n、shared UI、作业/题目/学生/提交/诊断数据、校正能力和标准库标签映射。

## Capabilities

### New Capabilities

- `teacher-analytics-console`: 定义教师端新的班级、作业、题目递进分析工作台，包括范围层级、统计粒度、客观指标、图表、AI 知识路径和证据样本。

### Modified Capabilities

- `teacher-console-ui`: 将旧教师端正式入口和语义从“作业详情 + 教师动作/讲评建议”调整为“教学对象递进分析 + 客观结果 + AI 知识归因”，并要求旧主线在迁移完成后退出正式导航。

## Impact

- 前端路由：新增或替换教师端正式路由，建议目标形态为 `/app/teacher/classes/:classId`、`/app/teacher/classes/:classId/assignments/:assignmentId`、`/app/teacher/classes/:classId/assignments/:assignmentId/problems/:problemId`。
- 前端模块：新增 `features/teacher-analytics/`，承载分析页、分析模型、数据 selector、图表组件、知识路径浮层和证据组件。
- 旧教师端模块：`features/teacher/AssignmentDetailPage.tsx`、`ClassOverviewPage.tsx`、相关 CSS 与旧教师动作区不再继续作为新能力承载点，只作为迁移参考。
- API 与 DTO：短期优先复用 `AssignmentOverview`、班级、作业、题目、学生提交和诊断字段；若知识路径统计不足，需要新增聚合接口或扩展 DTO。
- i18n：所有新增教师端文案必须同步维护中文和英文 translation。
- 视觉与交互：需要图表能力，若项目继续不引入第三方图表库，则先实现轻量 SVG 图表；如引入图表库需单独评估依赖和打包影响。
