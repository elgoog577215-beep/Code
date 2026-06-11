## 1. OpenSpec

- [x] 1.1 创建 `rebuild-teacher-assignment-workflow` 变更。
- [x] 1.2 写入 proposal、design、spec 和 tasks。
- [x] 1.3 运行 OpenSpec 状态检查，确认 apply 所需 artifact 完整。

## 2. 教师端页面重构

- [x] 2.1 重构 `/app/teacher` 为作业管理首页，加入状态筛选和核心指标，移除首屏复杂诊断内容。
- [x] 2.2 重构 `/app/teacher/assignment/new` 为基本信息、选择题目、确认发布三段式页面，加入搜索、难度筛选和已选题目摘要。
- [x] 2.3 重构 `/app/teacher/assignment/:assignmentId` 为概览、学生、题目、诊断标签结构，默认显示概览。
- [x] 2.4 将 AI/Coach 和教师错因校正收束到诊断标签，概览首屏只保留需关注摘要和进入诊断的路径。

## 3. 样式与验收

- [x] 3.1 新增教师作业工作流样式命名空间，替换旧的堆叠式教师端布局样式。
- [x] 3.2 更新 `browser-smoke` 教师端断言，匹配新的作业中心、创建页三段式和详情标签结构。
- [x] 3.3 运行 `npm run typecheck`。
- [x] 3.4 运行 `npm run smoke:browser`，如会改动静态构建产物则记录原因和范围。
- [x] 3.5 用浏览器检查 `/app/teacher`、`/app/teacher/assignment/new`、`/app/teacher/assignment/7` 的实际页面状态。
