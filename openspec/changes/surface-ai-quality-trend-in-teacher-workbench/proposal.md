## Why

跨作业 AI 质量趋势已经能统计诊断校正、source segment 和课堂介入 eval 候选，但教师工作台仍只展示当前作业的 AI 质量。老师缺少一个长期视角来判断哪些 AI 能力正在积累评测资产、哪些教师介入仍卡同类问题、哪些反馈还在等待后续提交证据。

教育 agent 的能力提升不能只停留在后端字段，必须让教师在日常工作台里看到可行动的趋势信号，并把它们转化为下一步校正、评测沉淀或教学复盘。

## What Changes

- 教师工作台加载 `/api/teacher/ai-quality/trend`，并独立处理 loading/error 状态。
- 在现有 AI 质量区新增跨作业趋势摘要，展示跨作业样本、诊断 eval 候选、课堂介入候选、仍卡同类问题和等待后续证据。
- 展示最近作业点的趋势列表，帮助老师识别哪个作业贡献了介入候选或 still stuck 信号。
- 展示 source segment 摘要，帮助老师看到模型/提示词/运行状态维度的质量来源。
- 保持当前作业 AI 质量详情和 eval 草稿流程不变。

## Capabilities

### New Capabilities

- `teacher-ai-quality-trend-surface`: 教师工作台必须可视化跨作业 AI 质量趋势，并把课堂介入成效候选转化为可行动的教师信号。

### Modified Capabilities

无。

## Impact

- 前端：更新 `TeacherPage.tsx` 和 `styles.css`。
- API：复用现有 `api.aiQualityTrend()` 和 `AiQualityTrend` 类型，无新增后端接口。
- 验证：运行前端 typecheck；必要时运行浏览器检查教师端 AI 质量区布局。
