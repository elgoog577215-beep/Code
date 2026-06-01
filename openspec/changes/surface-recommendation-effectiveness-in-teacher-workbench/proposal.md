## Why

推荐学习闭环已经记录曝光、点击、进入题目、后续提交和同类错因未解等效果信号，但教师工作台还看不到这些证据。这样推荐系统仍像一个“给学生发任务”的功能，而不是可校准、可复盘、可持续优化的教育 agent。

现在需要把推荐效果接入教师工作台，让老师能判断推荐是否真正带来了学习动作，以及何时需要教师介入或降低练习台阶。

## What Changes

- 教师工作台读取 `/api/teacher/recommendations/effectiveness`，独立处理 loading/error。
- 在 AI 质量区域新增推荐效果摘要，展示推荐曝光、点击、后续提交、通过、同类错因未解和教师介入建议数量。
- 展示 recommendation feedback signals，让老师看到推荐失败或卡点的原因与建议动作。
- 展示策略分布或焦点标签片段，帮助老师判断哪类推荐策略需要调参或复盘。
- 保持当前作业 AI 质量、跨作业趋势和 eval 草稿流程不变。

## Capabilities

### New Capabilities

- `teacher-recommendation-effectiveness-surface`: 教师工作台必须可视化推荐学习闭环效果，并把推荐后的学习证据转化为可行动的教师信号。

### Modified Capabilities

无。

## Impact

- 前端：更新 `TeacherPage.tsx` 和 `styles.css`。
- API：复用现有 `api.recommendationEffectiveness()` 和 `RecommendationEffectiveness` 类型。
- 后端：无新增接口或数据库迁移。
- 验证：运行 OpenSpec 严格校验、前端 typecheck、教师端视觉检查和 diff 检查。
