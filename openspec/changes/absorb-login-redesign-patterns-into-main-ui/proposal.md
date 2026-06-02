## Why

当前 `main` 已经完成学生端、题目页和教师端的信息降噪，但入口仍偏工具菜单，公共练习入口不足，教师端单作业过程仍集中在一个大页面里。`origin/feature/login-ui-redesign` 提供了更清楚的角色入口、公共题库、作业列表和单作业详情拆分，本变更要吸收这些优点，同时保留 `main` 已完成的教学工具型简化。

## What Changes

- 将 `/app` 调整为更清楚的角色与任务入口，突出学生、教师、公共题库三条路径。
- 新增 `/app/problems` 公共题库页面，支持搜索、难度筛选和题目卡片浏览。
- 优化全局导航，增加题库和邀请码快捷入口，但保持低噪音、工具型风格。
- 新增 `/app/teacher/assignment/:id` 单作业详情页，把邀请码、课堂 KPI、高频问题、学生列表和教师校正动作从教师总览中拆出。
- 调整 `/app/teacher` 为教师总览入口，优先展示作业选择、课堂摘要、需关注学生和高频问题。
- 保留 `main` 的学生端、题目页和 AI 质量降噪逻辑，不直接合并 origin 分支的后端、AI、静态构建产物或 mock/localStorage 数据流。

## Capabilities

### New Capabilities
- `main-ui-origin-pattern-absorption`: 以 `main` 为基线，吸收 origin 分支的入口、题库、教师作业详情和导航模式。

### Modified Capabilities

## Impact

- 影响代码集中在 `online-judge/frontend/src`，包括路由、入口页、公共题库页、教师页面拆分和样式。
- 不新增、不删除、不重命名后端 API。
- 不改变数据库、AI 诊断逻辑、评测链路或字段语义。
- 不引入新 UI 框架或新图标库。
