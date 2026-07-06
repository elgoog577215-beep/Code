## Why

教师端当前页面把作业、题目、学生诊断、班级学情和管理入口混在顶部导航与局部卡片里，导致老师难以按真实教学路径判断“先看作业、再看题目、再看学生证据”。前面生成的视觉草稿已经明确新方向：教师端应统一为左侧导航 + 右侧内容区，并让传统教学进度信息优先，AI 信息作为解释和治理辅助。

## What Changes

- 教师端统一使用固定左侧栏，主导航包含作业中心、班级学情、题库管理、AI 标准库和教学分析。
- 作业中心改为以作业列表为主，展示作业状态、班级、题目数、提交/通过/需关注和待处理队列。
- 作业详情按层级下钻：作业题目列表、题目分析、学生提交诊断，分别展示不同粒度的信息。
- 班级学情突出学生 x 作业矩阵、优先关注学生、班级薄弱点和教学建议。
- 新建作业、题库管理、AI 标准库和班级名单管理采用同一侧栏工作台语言，减少管理页横向阻断与无关工程态。
- 学生端仅做轻量视觉协调：返回入口、按钮、边界、间距与教师端保持同一产品质感，不改变学生端核心做题流程。

## Capabilities

### New Capabilities
- `teacher-console-ui`: 定义教师端统一导航骨架、教学下钻页面层级、AI 辅助信息展示边界，以及学生端轻量一致性要求。

### Modified Capabilities

## Impact

- 主要影响 `frontend/src/features/teacher/`、`frontend/src/features/insights/`、`frontend/src/features/task-editor/`、`frontend/src/features/student/`、`frontend/src/features/problem/` 与 `frontend/src/styles.css`。
- 不新增后端接口，不改变数据库和 AI 诊断契约。
- 需要同步中英文可见文案，避免教师端新增中文硬编码只存在于组件中。
