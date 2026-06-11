## Why

当前教师端已经有作业、题目、学生观察和 AI 诊断能力，但页面组织仍像功能堆叠：老师进入后需要在统计、题目、学生、AI/Coach、校正之间自己判断路径，容易眼花且缺少真实作业管理系统的顺序感。现在需要把教师端重构为清晰的作业工作流，让老师先完成“管理作业、创建作业、查看作业”，再按需进入诊断。

## What Changes

- 重构 `/app/teacher` 为作业管理首页，只展示作业列表、状态筛选、核心指标和“新建作业”主操作。
- 重构 `/app/teacher/assignment/new` 为三段式创建页：基本信息、选择题目、确认发布。
- 重构 `/app/teacher/assignment/:assignmentId` 为标签式作业详情：概览、学生、题目、诊断。
- 将 AI/Coach/教师校正降级到“诊断”二级区域，不再抢占作业中心和作业详情首屏。
- 更新前端 browser smoke，让验收语义匹配新信息架构，而不是继续断言旧的纵向堆叠页面。
- 不新增后端 API、不修改共享类型、不做数据库迁移、不引入截止时间或定时发布 UI。

## Capabilities

### New Capabilities

- `teacher-assignment-workflow`: 定义教师端作业管理、新建作业、作业详情和二级诊断的工作流与验收要求。

### Modified Capabilities

无。

## Impact

- Frontend: `TeacherPage`、`AssignmentCreatePage`、`AssignmentDetailPage`、教师端相关 CSS、`browser-smoke.mjs`。
- OpenSpec: 新增 `teacher-assignment-workflow` 能力说明、设计和任务清单。
- APIs / Backend / Database: 无变更。
- Validation: `npm run typecheck`、`npm run smoke:browser`，以及本地浏览器检查三个教师端核心路由。
