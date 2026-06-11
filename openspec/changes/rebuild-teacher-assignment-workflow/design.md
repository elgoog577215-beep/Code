## Context

教师端当前已经有三个核心页面：作业中心、新建作业、作业详情。它们使用现有接口即可完成作业管理闭环：`api.assignments()`、`api.assignmentOverview()`、`api.classes()`、`api.problemCatalog()`、`api.createAssignment()`、`api.diagnosisTags()` 和 `api.correctDiagnosis()`。问题不在能力缺失，而在信息架构和视觉层级：作业详情把概览、学生、题目、AI/Coach 和教师校正串成一条长页面，作业创建页也缺少题库式选择和已选摘要。

本轮参考 CodeHS 的作业/题库流程、Moodle 的题库分离、PrairieLearn 的 assessment/questions 结构，但只吸收信息架构和交互逻辑，不引入新后端能力。

## Goals / Non-Goals

**Goals:**

- 作业中心成为老师的作业管理首页，首屏只服务管理和进入作业。
- 新建作业按“基本信息 -> 选择题目 -> 确认发布”组织，选题区有筛选和已选摘要。
- 作业详情按“概览 / 学生 / 题目 / 诊断”标签组织，默认停留在概览。
- AI/Coach/教师校正只在诊断标签出现，首屏保留少量需关注摘要。
- 保持现有路由、接口、共享类型和数据库兼容。

**Non-Goals:**

- 不新增截止时间、定时发布、题目标签、作业模板或批量编辑能力。
- 不重构后端聚合接口。
- 不刷新静态构建产物作为本轮交付要求。

## Decisions

### 1. 使用页面内标签而不是拆更多路由

作业详情保留 `/app/teacher/assignment/:id`，内部用本地 state 控制 `概览 / 学生 / 题目 / 诊断`。这样不改变路由契约，也能快速把复杂内容分层。备选方案是新增子路由，但第一版会引入更多导航和 smoke 复杂度。

### 2. 新建作业采用单页三段式

创建页不使用弹窗或多步路由，而是在一个页面内展示三段：基本信息、选择题目、确认发布。题目选择区提供关键字和难度筛选，右侧或下方展示已选题目摘要。这样复用现有提交 payload，同时减少老师在多个页面间来回。

### 3. 新 CSS 命名空间承接重构

新增教师作业工作流相关类名，例如 `teacher-workflow-*`、`assignment-builder-*`、`assignment-detail-tabs`。旧的 `assignment-section`、`teacher-assignment-card` 可逐步移除或仅保留兼容，但重构后的主要布局不再继续叠旧样式，避免 CSS 继续变成补丁层。

### 4. Smoke 验收跟随新信息架构

`browser-smoke.mjs` 不再要求旧详情页首屏同时出现整体统计、学生情况、作业题目、高级分析，而是检查作业中心主流程、创建页三段式、详情页标签和诊断二级入口。这样验收能阻止 UI 回到功能堆叠。

## Risks / Trade-offs

- [Risk] 第一版不新增后端字段，无法提供截止时间和定时发布。→ Mitigation: 页面不展示这些设置，避免假功能。
- [Risk] 标签式详情可能隐藏部分高级功能。→ Mitigation: 概览保留“需关注”和“进入诊断”入口，诊断标签完整承接 AI/Coach/校正。
- [Risk] `styles.css` 已经很大，改动可能影响旧页面。→ Mitigation: 新增独立命名空间，尽量只替换教师端重构页面引用的样式。
- [Risk] 当前工作区可能有其他改动。→ Mitigation: 实施时只改 OpenSpec、本轮教师端源码、教师端 CSS 和 smoke 验收，不碰后端和构建产物。
