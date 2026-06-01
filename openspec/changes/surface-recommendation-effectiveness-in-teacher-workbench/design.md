## Context

`RecommendationEffectivenessService` 已经基于 `StudentRecommendationEvent` 输出推荐触达、点击、进入题目、后续提交、后续 AC、同类错因未解、点击无提交和教师介入建议等指标。前端已有 `api.recommendationEffectiveness()` 和 `RecommendationEffectiveness` 类型，但教师工作台没有读取或展示这些数据。

教师目前能看到当前作业 AI 质量、跨作业趋势和 eval 草稿，但看不到推荐是否真的推动学生行动。推荐系统因此缺少教师校准入口。

## Goals / Non-Goals

**Goals:**

- 在教师工作台读取推荐效果总览，并与当前作业 AI 质量互不阻塞。
- 展示推荐触达、点击、后续提交、通过、同类错因未解和教师介入建议。
- 展示 feedback signals，帮助老师判断推荐失败的原因和下一步动作。
- 展示策略或焦点标签片段，帮助老师发现哪类推荐策略需要复盘。
- 保持教师端界面紧凑，适合重复扫描。

**Non-Goals:**

- 不新增后端 API、数据库迁移或推荐算法。
- 不修改学生端推荐卡片。
- 不做复杂图表库。
- 不改变已有 AI 质量维度计算。

## Decisions

### Decision 1: 放在 AI 质量区内、跨作业趋势之后

推荐效果是 AI 质量闭环的一部分，和跨作业趋势一样属于教师校准视角。放在 AI 质量区内可以让老师从“当前作业质量 -> 长期趋势 -> 推荐学习效果”连续判断，而不是跳到另一个页面。

### Decision 2: 独立读取与错误态

推荐效果是全局统计，不能影响当前作业 AI 质量和趋势展示。新增 `recommendationEffectiveness`、loading、error 状态，由 `loadAll` 触发读取。

### Decision 3: 使用紧凑指标 + signal 列表 + strategy/focus 片段

第一版不画复杂图表，使用已有工作台视觉语言：

- 指标条展示推荐漏斗和风险结果。
- feedback signals 展示推荐后仍卡同类错因或点击无提交。
- strategy/focus 片段展示需要校准的推荐策略和错因焦点。

这能回答教师最关心的三个问题：推荐有没有被看到、有没有带来学习动作、哪里需要教师接管。

## Risks / Trade-offs

- [Risk] 新增一块信息让 AI 质量区过重。-> Mitigation: 控制为一块紧凑卡片，只展示 top signals 和少量 segment。
- [Risk] 全局推荐效果与当前作业不完全对应。-> Mitigation: 文案明确为推荐学习闭环，不替代当前作业 AI 质量。
- [Risk] 推荐效果数据不足时可能误导。-> Mitigation: 空态说明只能判断触达，不能判断学习效果。
