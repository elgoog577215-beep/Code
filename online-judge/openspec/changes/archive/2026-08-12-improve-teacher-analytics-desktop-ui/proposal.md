## Why

教师分析桌面端已经具备完整数据和下钻能力，但 1440px 页面没有形成稳定的阅读轨道：导航像悬浮卡，标题与状态被拉到两端，列表行缺少明确的进度重心，学生证据工作区也没有充分利用桌面宽度。需要在不增加解释性文案和装饰性统计的前提下，让教师在五秒内看清当前对象、核心状态、下一层入口与证据。

## What Changes

- 建立连续、稳定的桌面教师工作台轨道，使侧栏与正文形成清楚的主从关系。
- 收拢分析页标题、状态与操作，让对象名称和关键指标保持视觉关联。
- 把班级、作业和题目列表整理为可扫描的桌面列结构，强化提交与完成进度，避免指标漂浮在整行两端。
- 强化题目学习阶段筛选和学生名单之间的选中关系，减少空白但不增加无关统计。
- 将学生分析的分类与当前提交控制合并为一个桌面工作栏，并让证据内容使用合适的阅读宽度。
- 统一教师成长时间线的本地化日期格式，消除中文界面中的英文长日期。
- 保持 390px 移动端结构和学生端既有主体不变，只做必要回归保护。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `teacher-console-ui`: 增加桌面端工作台轨道、列表扫描、证据工作区和本地化时间的正式要求。

## Impact

- 教师工作台布局：`frontend/src/features/teacher/TeacherShell.tsx`、`TeacherHomeRefresh.css`
- 教师分析页面和组件：`frontend/src/features/teacher-analytics/`
- 成长时间线展示：`frontend/src/features/growth/SingleProblemGrowthDashboard.tsx`
- 中英文资源与前端正式构建产物
- 不修改教师分析接口、统计口径、数据库结构和学生端产品流程
